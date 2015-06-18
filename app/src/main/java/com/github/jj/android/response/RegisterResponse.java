package com.github.jj.android.response;

import android.text.TextUtils;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import me.yugy.app.common.network.BaseResponse;

public class RegisterResponse extends BaseResponse {

    @Expose @SerializedName("result") public int result;
    @Expose @SerializedName("uid") public String uid;
    @Expose @SerializedName("reason") public String reason;

    @Override
    public boolean isValidated() {
        if (result <= 0) {
            return false;
        }
        if (TextUtils.isEmpty(uid) && TextUtils.isEmpty(reason)) {
            return false;
        }
        return true;
    }
}
