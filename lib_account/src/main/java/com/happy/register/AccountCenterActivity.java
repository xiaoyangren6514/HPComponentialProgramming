package com.happy.register;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.happy.prouter_annotations.PRouter;
import com.happy.prouter_annotations.Parameter;

@PRouter(path = "/account/accountCenter")
public class AccountCenterActivity extends AppCompatActivity {

    @Parameter
    String name;

    @Parameter
    String sex;

    @Parameter
    int age;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        getIntent().getIntExtra("aaa",0)
    }

}
