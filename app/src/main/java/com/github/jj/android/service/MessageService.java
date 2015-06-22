package com.github.jj.android.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.github.jj.android.Conf;
import com.github.jj.android.model.Pack;
import com.github.jj.android.request.InitRequest;
import com.github.jj.android.response.InitResponse;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
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

    private String[] mManagerAddress;
    private String mToken;
    private String mUid;
    private ReadThread mReadThread;
    private WriteThread mWriteThread;

    @Override
    public void onCreate() {
        super.onCreate();
        DebugUtils.log("onCreate");
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        mToken = preferences.getString("token", "");
        mUid = preferences.getString("uid", "");
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
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        DebugUtils.log("onTaskRemoved");
        ReadThread.isRunning = false;
        WriteThread.isRunning = false;
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        DebugUtils.log("onDestroy");
        ReadThread.isRunning = false;
        WriteThread.isRunning = false;
        super.onDestroy();
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
                if (mReadThread != null && mReadThread.isAlive()) {
                    ReadThread.isRunning = false;
                    mReadThread.interrupt();
                }
                ReadThread.isRunning = true;
                new ReadThread(mToken, socketChannel).start();

                if (mWriteThread != null && mWriteThread.isAlive()) {
                    WriteThread.isRunning = false;
                    mWriteThread.interrupt();
                }
                WriteThread.isRunning = true;
                new WriteThread(mToken, socketChannel).start();
            }
        }
    }

    private static class WriteThread extends Thread {

        public static boolean isRunning = false;

        private final String mToken;
        private final SocketChannel mSocketChannel;

        public WriteThread(String token, SocketChannel socketChannel) {
            mToken = token;
            mSocketChannel = socketChannel;
        }

        @Override
        public void run() {
            DebugUtils.log("run");
            ByteBuffer buffer = ByteBuffer.allocate(512);

            try {
                buffer.clear();
                Pack<String> pack = new Pack<>();
                pack.meta = new Pack.Meta();
                pack.meta.path = "debug.ping";
                pack.write(mToken, buffer);
                buffer.flip();

                DebugUtils.log("buffer is ready.");

                while (buffer.hasRemaining()) {
                    if (! mSocketChannel.finishConnect()) {
                        continue;
                    }
                    mSocketChannel.write(buffer);
                }

                DebugUtils.log("buffer has been written.");
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private static class ReadThread extends Thread {

        public static boolean isRunning = false;

        private final String mToken;
        private final SocketChannel mSocketChannel;

        private ReadThread(String token, SocketChannel socketChannel) {
            mToken = token;
            mSocketChannel = socketChannel;
        }

        @Override
        public void run() {
            try {
                Selector selector = Selector.open();
                mSocketChannel.register(selector, SelectionKey.OP_READ);
                while (isRunning) {
                    if (!mSocketChannel.finishConnect()) {
                        continue;
                    }

                    selector.select();
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                    while(keyIterator.hasNext() && isRunning) {

                        SelectionKey key = keyIterator.next();
                        if (key.isReadable() && isRunning) {
                            SocketChannel channel = (SocketChannel) key.channel();
                            ByteBuffer buffer = ByteBuffer.allocate(4);
                            channel.read(buffer);
                            buffer.flip();
                            int length = buffer.getInt();
                            DebugUtils.log("Read, length: " + length);
                            buffer = ByteBuffer.allocate(length);
                            channel.read(buffer);
                            buffer.flip();
                            byte[] content = new byte[length];
                            buffer.get(content);
                            ByteArrayInputStream in = new ByteArrayInputStream(content);
                            JsonReader reader = new JsonReader(new InputStreamReader(in));
                            Pack.Meta meta = new Gson().fromJson(reader, Pack.Meta.class);
                            DebugUtils.log(meta.seq);
                            String body = new Gson().fromJson(reader, String.class);
                            DebugUtils.log(body);
                        }

                        keyIterator.remove();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
