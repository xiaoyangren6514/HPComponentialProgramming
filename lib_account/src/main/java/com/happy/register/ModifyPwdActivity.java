package com.happy.register;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.happy.prouter_annotations.PRouter;

@PRouter(path = "/account/modify")
public class ModifyPwdActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modify_pwd);
    }
}