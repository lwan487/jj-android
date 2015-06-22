package com.github.jj.android.model;

import com.github.jj.android.Conf;
import com.google.gson.Gson;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.crypto.spec.IvParameterSpec;

import me.yugy.app.common.utils.AESUtils;

public class Pack<T> {

    public Meta meta;
    public T body;

    public void write(String token, ByteBuffer buffer) throws IOException {
        Gson gson = new Gson();
        byte[] metabytes = gson.toJson(meta).getBytes();
        byte[] bodybytes = AESUtils.AES_CFB_ENCRYPT(gson.toJson(body).getBytes(), token, new IvParameterSpec(Conf.AES_IV));
        byte[] total = new byte[metabytes.length + bodybytes.length];
        System.arraycopy(metabytes, 0, total, 0, metabytes.length);
        System.arraycopy(bodybytes, 0, total, metabytes.length, bodybytes.length);
        int length = total.length;
        buffer.putInt(length).put(total);
    }

    public static class Meta {
        public int version = 1;
        public int seq = 0;
        public String path = "";
    }

}
