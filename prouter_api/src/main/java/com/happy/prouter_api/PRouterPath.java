package com.happy.prouter_api;

import com.happy.prouter_annotations.bean.RouterBean;

import java.util.Map;

/**
 * 路由组Group对应的---详细Path加载数据接口PRouterPath
 * eg. order分组对应--- 有哪些类要加载(OrderDetailActivity,OrderListActivity)
 */
public interface PRouterPath {
    /**
     * key: "/order/order_mainactivity"   value: RouterBean == OrderMainActivity.class
     * key: "/account/account_activity"   value: RouterBean == AccountActivity.class
     * @return
     */
    Map<String, RouterBean> getPathMap();
}
