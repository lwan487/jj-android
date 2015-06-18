package com.github.jj.android.request;

import android.support.annotation.NonNull;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.github.jj.android.Conf;
import com.github.jj.android.response.RegisterResponse;

import java.util.HashMap;
import java.util.Map;

import me.yugy.app.common.network.GsonRequest;
import me.yugy.app.common.utils.MD5Utils;

public class RegisterRequest extends GsonRequest<RegisterResponse> {

    private String mEmail;
    private String mPassword;

    public RegisterRequest(@NonNull String email, @NonNull String password, Response.Listener<RegisterResponse> listener, Response.ErrorListener errorListener) {
        super(Method.POST, Conf.API_REGISTER, null, listener, errorListener);
        mEmail = email;
        mPassword = password;
    }

    @Override
    protected Map<String, String> getParams() throws AuthFailureError {
        Map<String, String> params = new HashMap<>();
        params.put("email", mEmail);
        params.put("secret", MD5Utils.md5(mPassword + Conf.SALT));
        return params;
    }
}