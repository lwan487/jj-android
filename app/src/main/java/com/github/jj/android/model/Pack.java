package com.github.jj.android.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.github.jj.android.Conf;
import com.google.gson.Gson;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.crypto.spec.IvParameterSpec;

import me.yugy.app.common.utils.AESUtils;

public class Pack<T extends Parcelable> implements Parcelable{

    public Meta meta;
    public T body;

    public ByteBuffer getBuffer(String token) throws IOException {
        Gson gson = new Gson();
        byte[] metabytes = gson.toJson(meta).getBytes();
        byte[] bodybytes = AESUtils.AES_CFB_ENCRYPT(gson.toJson(body).getBytes(), token, new IvParameterSpec(Conf.AES_IV));
        byte[] total = new byte[metabytes.length + bodybytes.length];
        System.arraycopy(metabytes, 0, total, 0, metabytes.length);
        System.arraycopy(bodybytes, 0, total, metabytes.length, bodybytes.length);
        int length = total.length;
        ByteBuffer buffer = ByteBuffer.allocate(4 + length);
        buffer.putInt(length).put(total);
        return buffer;
    }

    public Pack() {

    }

    private Pack(Parcel in) {
        this.meta = in.readParcelable(Meta.class.getClassLoader());
        Class clazz = (Class) in.readSerializable();
        this.body = in.readParcelable(clazz.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeParcelable(meta, flags);
        dest.writeSerializable(body.getClass());
        dest.writeParcelable(body, flags);
    }

    public static final Parcelable.Creator<Pack> CREATOR = new Parcelable.Creator<Pack>() {
        public Pack createFromParcel(@NonNull Parcel source) {
            return new Pack(source);
        }

        @NonNull
        public Pack[] newArray(int size) {
            return new Pack[size];
        }
    };

    public static class Meta implements Parcelable {
        public int version = 1;
        public int seq = 0;
        public String path = "";

        @Override
        public int describeContents() {
            return 1;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            dest.writeInt(this.version);
            dest.writeInt(this.seq);
            dest.writeString(this.path);
        }

        public Meta() {
        }

        protected Meta(Parcel in) {
            this.version = in.readInt();
            this.seq = in.readInt();
            this.path = in.readString();
        }

        public static final Parcelable.Creator<Meta> CREATOR = new Parcelable.Creator<Meta>() {
            public Meta createFromParcel(@NonNull Parcel source) {
                return new Meta(source);
            }

            @NonNull
            public Meta[] newArray(int size) {
                return new Meta[size];
            }
        };
    }

}
