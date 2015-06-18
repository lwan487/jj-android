package com.github.jj.android.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.TextView;

import com.github.jj.android.R;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import me.yugy.app.common.BaseActivity;

public class MainActivity extends BaseActivity {

    @InjectView(R.id.info)
    TextView mInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        StringBuilder sb = new StringBuilder("Logined: ");
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (preferences.contains("uid") && preferences.contains("token")
                && preferences.contains("email") && preferences.contains("secret")) {
            sb.append(true + "\n");
            sb.append("uid: ").append(preferences.getString("uid", "error"));
        } else {
            sb.append(false);
        }
        mInfo.setText(sb.toString());
    }

    @OnClick(R.id.register)
    void onRegisterClick() {
        RegisterActivity.launch(getActivity());
    }

    @OnClick(R.id.login)
    void onLoginClick() {
        LoginActivity.launch(getActivity());
    }
}
