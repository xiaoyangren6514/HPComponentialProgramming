package com.happy.compro;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.happy.order.LoginActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = " ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_login).setOnClickListener(this);
        if (BuildConfig.isRelease) {
            showLog("onCreate:当前是集成化 线上环境，以App壳为主导运行方式");
        } else {
            showLog("onCreate:当前是组件化 测试环境，所有子模块都可以组里运行");
        }
        showLog("当前URL为：" + BuildConfig.debug);
    }

    private void showLog(String msg) {
        Log.v(TAG, msg);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_login:
                startActivity(new Intent(this, LoginActivity.class));
                break;
        }
    }
}