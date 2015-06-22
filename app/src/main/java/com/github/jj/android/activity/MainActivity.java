package com.github.jj.android.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.TextView;

import com.github.jj.android.R;
import com.github.jj.android.listener.PackListener;
import com.github.jj.android.model.Pack;
import com.github.jj.android.model.Ping;
import com.github.jj.android.service.MessageService;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import me.yugy.app.common.BaseActivity;
import me.yugy.app.common.utils.MessageUtils;
import me.yugy.app.common.utils.ServiceUtils;

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
        refreshState();
    }

    private void refreshState() {
        StringBuilder sb = new StringBuilder("Logined: ");
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (preferences.contains("uid") && preferences.contains("token")
                && preferences.contains("email") && preferences.contains("secret")) {
            sb.append(true + "\n");
            sb.append("uid: ").append(preferences.getString("uid", "error")).append("\n");
        } else {
            sb.append(false).append("\n");
        }
        sb.append("MessageService state: ");
        sb.append(
                ServiceUtils.isServiceRunning(getActivity(), MessageService.class)
                        ? "running\n" : "not running\n");
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

    @OnClick(R.id.start_service)
    void onStartServiceClick() {
        MessageService.start(getActivity());
        refreshState();
    }

    @OnClick(R.id.stop_service)
    void onStopServiceClick() {
        MessageService.stop(getActivity());
        refreshState();
    }

    @OnClick(R.id.ping)
    void onPingClick() {
        Pack<Ping> pack = new Pack<>();
        pack.body = new Ping();
        pack.meta = new Pack.Meta();
        pack.meta.path = "debug.ping";
        MessageService.write(getActivity(), pack, new PackListener<String>() {
            @Override
            public void onSuccess(String result) {
                MessageUtils.toast(getActivity(), result);
            }

            @Override
            public void onFailed(Exception e) {

            }
        });
    }
}
