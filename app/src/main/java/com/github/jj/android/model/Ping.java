package com.github.jj.android.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Ping implements Parcelable {
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }

    public Ping() {
    }

    protected Ping(Parcel in) {
    }

    public static final Parcelable.Creator<Ping> CREATOR = new Parcelable.Creator<Ping>() {
        public Ping createFromParcel(Parcel source) {
            return new Ping(source);
        }

        public Ping[] newArray(int size) {
            return new Ping[size];
        }
    };
}
