package com.github.jj.android.request;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.github.jj.android.Conf;
import com.github.jj.android.response.InitResponse;

import java.util.HashMap;
import java.util.Map;

import me.yugy.app.common.network.GsonRequest;

public class InitRequest extends GsonRequest<InitResponse> {

    private String mToken;
    private String mUid;

    public InitRequest(String token, String uid, Response.Listener<InitResponse> listener, Response.ErrorListener errorListener) {
        super(Method.POST, Conf.API_INIT, null, listener, errorListener);
        mToken = token;
        mUid = uid;
    }
//
//    @Override
//    protected Response<InitResponse> parseNetworkResponse(NetworkResponse response) {
//        String body = new String(response.body);
//        DebugUtils.log(body);
//        return super.parseNetworkResponse(response);
//    }

    @Override
    protected Map<String, String> getParams() throws AuthFailureError {
        Map<String, String> params = new HashMap<>();
        params.put("token", mToken);
        params.put("uid", mUid);
        return params;
    }
}
