package com.happy.order;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.happy.prouter_annotations.PRouter;

@PRouter(path = "/order/list")
public class OrderListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_list);
    }
}