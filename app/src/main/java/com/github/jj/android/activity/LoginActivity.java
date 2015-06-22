package com.github.jj.android.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TextInputLayout;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.github.jj.android.Conf;
import com.github.jj.android.R;
import com.github.jj.android.request.LoginRequest;
import com.github.jj.android.response.LoginResponse;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import me.yugy.app.common.BaseActivity;
import me.yugy.app.common.utils.MD5Utils;
import me.yugy.app.common.utils.MessageUtils;

public class LoginActivity extends BaseActivity {

    public static void launch(Context context) {
        context.startActivity(new Intent(context, LoginActivity.class));
    }

    @InjectView(R.id.email_container)
    TextInputLayout mEmailContainer;
    @InjectView(R.id.email)
    EditText mEmail;
    @InjectView(R.id.password_container)
    TextInputLayout mPasswordContainer;
    @InjectView(R.id.password)
    EditText mPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.inject(this);
    }

    @OnEditorAction(R.id.password)
    boolean onPasswordEnter(int actionId) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            onLoginClick();
            return true;
        }
        return false;
    }

    @OnClick(R.id.login)
    void onLoginClick() {
        if (mEmail.getText().length() == 0) {
            mEmailContainer.setError("Email can not be empty.");
            return;
        }
        if (mPassword.getText().length() == 0) {
            mEmailContainer.setError("");
            mPasswordContainer.setError("Password can not be empty.");
            return;
        } else {
            mPasswordContainer.setError("");
        }
        final String email = mEmail.getText().toString();
        final String password = mPassword.getText().toString();
        addRequest(new LoginRequest(email, password, new Response.Listener<LoginResponse>() {
            @Override
            public void onResponse(LoginResponse response) {
                if (response != null && response.isValidated()) {
                    switch (response.result) {
                        case Conf.RESULT_FAILED:
                            mEmailContainer.setError(response.reason);
                            break;
                        case Conf.RESULT_OK:
                            MessageUtils.toast(getActivity(), "Login success.");
                            SharedPreferences preferences
                                    = PreferenceManager.getDefaultSharedPreferences(getActivity());
                            preferences.edit()
                                    .putString("uid", response.uid)
                                    .putString("token", response.token)
                                    .putString("email", email)
                                    .putString("secret", MD5Utils.md5(password + Conf.SALT))
                                    .apply();
                            finish();
                            break;
                        case Conf.RESULT_MAINTAIN:
                            getActivity().startActivity(
                                    new Intent(Intent.ACTION_VIEW, Uri.parse(response.reason)));
                            break;
                    }
                } else {
                    MessageUtils.toast(getActivity(), "Server Error.");
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                MessageUtils.toast(getActivity(), "Connect server failed.");
            }
        }));
    }
}
