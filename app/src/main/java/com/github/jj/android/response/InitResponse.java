package com.github.jj.android.response;

import android.support.v4.text.TextUtilsCompat;
import android.text.TextUtils;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.w3c.dom.Text;

import me.yugy.app.common.network.BaseResponse;
import me.yugy.app.common.network.Param;

public class InitResponse extends BaseResponse {

    @Expose @SerializedName("result") public int result;
    @Expose @SerializedName("reason") public String reason;
    @Expose @SerializedName("mgraddr") public String[] managerAddress;

    @Override
    public boolean isValidated() {
        if (result <= 0) {
            return false;
        }
        if (result == 200) {
            if (managerAddress == null) {
                return false;
            }
            for (String address : managerAddress) {
                if (TextUtils.isEmpty(address)) {
                    return false;
                }
            }
        }
        return true;
    }
}
