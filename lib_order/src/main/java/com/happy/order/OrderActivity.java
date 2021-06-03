package com.happy.order;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.happy.prouter_annotations.PRouter;

@PRouter(path = "order/main")
public class OrderActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);
    }
}