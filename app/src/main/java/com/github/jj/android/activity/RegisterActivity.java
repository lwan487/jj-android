package com.github.jj.android.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.github.jj.android.Conf;
import com.github.jj.android.R;
import com.github.jj.android.request.RegisterRequest;
import com.github.jj.android.response.RegisterResponse;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import me.yugy.app.common.BaseActivity;
import me.yugy.app.common.utils.MessageUtils;

public class RegisterActivity extends BaseActivity {

    public static void launch(Context context) {
        Intent intent = new Intent(context, RegisterActivity.class);
        context.startActivity(intent);
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
        setContentView(R.layout.activity_register);
        ButterKnife.inject(this);
    }

    @OnEditorAction(R.id.password)
    boolean onPasswordEnter(int actionId) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            onRegisterClick();
            return true;
        }
        return false;
    }

    @OnClick(R.id.register)
    void onRegisterClick() {
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
        addRequest(new RegisterRequest(email, password, new Response.Listener<RegisterResponse>() {
            @Override
            public void onResponse(RegisterResponse response) {
                if (response.isValidated()) {
                    switch (response.result) {
                        case Conf.RESULT_FAILED:
                            mEmailContainer.setError(response.reason);
                            break;
                        case Conf.RESULT_OK:
                            MessageUtils.toast(getActivity(), "Register success, please login.");
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
