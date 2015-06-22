package com.github.jj.android.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.github.jj.android.Conf;
import com.github.jj.android.listener.PackListener;
import com.github.jj.android.model.Pack;
import com.github.jj.android.request.InitRequest;
import com.github.jj.android.response.InitResponse;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import me.yugy.app.common.network.RequestManager;
import me.yugy.app.common.utils.DebugUtils;
import me.yugy.app.common.utils.MessageUtils;

public class MessageService extends Service {

    public static void start(Context context) {
        context.startService(new Intent(context, MessageService.class));
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, MessageService.class));
    }

    public static void write(Context context, Pack pack, PackListener listener) {
        DebugUtils.log("Assign seq: " + sCurrentSeq);
        listener.seq = sCurrentSeq;
        pack.meta.seq = listener.seq;
        sPackListeners.add(new WeakReference<>(listener));

        Intent intent = new Intent(context, MessageService.class);
        intent.putExtra("pack", pack);
        intent.putExtra("type", "write");
        context.startService(intent);
    }

    private static int sCurrentSeq = 0;
    private static ArrayList<WeakReference<PackListener>> sPackListeners = new ArrayList<>();

    private String[] mManagerAddress;
    private String mToken;
    private String mUid;
    private boolean mIsRunning = false;
    private ReadThread mReadThread;
    private WriteThread mWriteThread;
    private Queue<Pack> mPackQueue;
    private Handler mHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        DebugUtils.log("onCreate");
        mIsRunning = true;
        mHandler = new Handler(Looper.getMainLooper());
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        mToken = preferences.getString("token", "");
        mUid = preferences.getString("uid", "");
        mPackQueue = new LinkedList<>();
        RequestManager.getInstance(this).addRequest(new InitRequest(mToken, mUid, new Response.Listener<InitResponse>() {
            @Override
            public void onResponse(InitResponse response) {
                if (response != null && response.isValidated()) {
                    switch (response.result) {
                        case Conf.RESULT_FAILED:
                            DebugUtils.log("init failed: " + response.reason);
                            stop(getApplicationContext());
                            break;
                        case Conf.RESULT_OK:
                            mManagerAddress = response.managerAddress;
                            for (String address : mManagerAddress) {
                                DebugUtils.log(address);
                            }
                            if (mManagerAddress.length > 0) {
                                String[] server = mManagerAddress[0].split(":");
                                new PrepareChannelTask().execute(server);
                            }
                            break;
                        case Conf.RESULT_MAINTAIN:
                            DebugUtils.log(response.reason);
                            break;
                    }
                } else {
                    MessageUtils.toast(getApplicationContext(), "Server Error.");
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                onFail(error);
                MessageUtils.toast(getApplicationContext(), "Connect server failed.");
            }
        }));
    }

    @Override
    public int onStartCommand(@NonNull Intent intent, int flags, int startId) {
        handleIntent(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    private void handleIntent(Intent intent) {
        if (intent != null) {
            DebugUtils.log("handleIntent");
            String type = intent.getStringExtra("type");
            if (type == null) {
                type = "";
            }
            switch (type) {
                case "write":
                    DebugUtils.log("write");
                    Pack pack = intent.getParcelableExtra("pack");
                    mPackQueue.offer(pack);
                    break;
            }
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        DebugUtils.log("onTaskRemoved");
        terminateThreads();
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        DebugUtils.log("onDestroy");
        terminateThreads();
        super.onDestroy();
    }

    private void terminateThreads() {
        mIsRunning = false;
        if (mReadThread != null && mReadThread.isAlive()) {
            mReadThread.interrupt();
        }
        if (mWriteThread != null && mWriteThread.isAlive()) {
            mWriteThread.interrupt();
        }
    }

    @Override
    public IBinder onBind(@NonNull Intent intent) {
        return null;
    }

    private void onFail(Exception e) {
        e.printStackTrace();
    }

    private class PrepareChannelTask extends AsyncTask<String, Void, SocketChannel> {

        @Override
        protected SocketChannel doInBackground(@NonNull String... params) {
            String domain = params[0];
            int port = Integer.decode(params[1]);

            try {
                SocketChannel socketChannel = SocketChannel.open();
                socketChannel.configureBlocking(false);
                socketChannel.connect(new InetSocketAddress(domain, port));
                return socketChannel;
            } catch (IOException e) {
                onFail(e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(SocketChannel socketChannel) {
            if (socketChannel != null) {
                terminateThreads();

                mIsRunning = true;
                mReadThread = new ReadThread(socketChannel);
                mWriteThread = new WriteThread(socketChannel);

                mReadThread.start();
                mWriteThread.start();
            }
        }
    }

    private class WriteThread extends Thread {

        private final SocketChannel mSocketChannel;

        public WriteThread(SocketChannel socketChannel) {
            mSocketChannel = socketChannel;
        }

        @Override
        public void run() {
            DebugUtils.log("WriteThread run");

            Pack pack;
            while (mIsRunning) {
                try {
                    if ((pack = mPackQueue.poll()) != null) {
                        DebugUtils.log("get pack to write, seq: " + pack.meta.seq);
                        ByteBuffer buffer = pack.getBuffer(mToken);
                        buffer.flip();

                        DebugUtils.log("buffer is ready.");

                        while (buffer.hasRemaining()) {
                            if (!mSocketChannel.finishConnect()) {
                                continue;
                            }
                            mSocketChannel.write(buffer);
                        }
                        sCurrentSeq++;
                        DebugUtils.log("buffer has been written.");
                    }
                    if (mPackQueue.size() == 0) {
                        Thread.sleep(80);
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    private class ReadThread extends Thread {

        private final SocketChannel mSocketChannel;

        private ReadThread(SocketChannel socketChannel) {
            mSocketChannel = socketChannel;
        }

        @Override
        public void run() {
            DebugUtils.log("ReadThread run");
            try {
                Selector selector = Selector.open();
                mSocketChannel.register(selector, SelectionKey.OP_READ);
                while (mIsRunning) {
                    if (!mSocketChannel.finishConnect()) {
                        continue;
                    }

                    selector.select();
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                    while(keyIterator.hasNext() && mIsRunning) {

                        SelectionKey key = keyIterator.next();
                        if (key.isReadable() && mIsRunning) {
                            SocketChannel channel = (SocketChannel) key.channel();
                            ByteBuffer buffer = ByteBuffer.allocate(4);
                            channel.read(buffer);
                            buffer.flip();
                            int length = buffer.getInt();
                            buffer = ByteBuffer.allocate(length);
                            channel.read(buffer);
                            buffer.flip();
                            byte[] content = new byte[length];
                            buffer.get(content);
                            ByteArrayInputStream in = new ByteArrayInputStream(content);
                            JsonReader reader = new JsonReader(new InputStreamReader(in));
                            Pack.Meta meta = new Gson().fromJson(reader, Pack.Meta.class);
                            DebugUtils.log("Read, length: " + length + ", seq: " + meta.seq);
                            final String body = new Gson().fromJson(reader, String.class);

                            dispatchSuccessAndRemove(meta, body);
                        }

                        keyIterator.remove();
                    }
                }
            } catch (IOException | BufferUnderflowException e) {
                if (e instanceof BufferUnderflowException) {
                    terminateThreads();
                    stopSelf();
                }
                e.printStackTrace();
            }
        }
    }

    private void dispatchSuccessAndRemove(Pack.Meta meta, final Object body) {
        Iterator<WeakReference<PackListener>> iterator = sPackListeners.iterator();
        while (iterator.hasNext()) {
            final WeakReference<PackListener> reference = iterator.next();
            if (reference.get() != null && reference.get().seq == meta.seq) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        //noinspection unchecked
                        reference.get().onSuccess(body);
                    }
                });
            }
            iterator.remove();
        }
    }
}
