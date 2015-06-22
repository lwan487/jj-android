package com.github.jj.android.listener;

public abstract class PackListener<T> {

    public int seq;

    public abstract void onSuccess(T result);
    public abstract void onFailed(Exception e);

}
