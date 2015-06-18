package com.github.jj.android.response;

import android.text.TextUtils;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import me.yugy.app.common.network.BaseResponse;

public class LoginResponse extends BaseResponse {

    @Expose @SerializedName("result") public int result;
    @Expose @SerializedName("uid") public String uid;
    @Expose @SerializedName("reason") public String reason;
    @Expose @SerializedName("token") public String token;

    @Override
    public boolean isValidated() {
        if (result <= 0) {
            return false;
        }
        if (TextUtils.isEmpty(uid) && TextUtils.isEmpty(reason) && TextUtils.isEmpty(token)) {
            return false;
        }
        return true;
    }
}
