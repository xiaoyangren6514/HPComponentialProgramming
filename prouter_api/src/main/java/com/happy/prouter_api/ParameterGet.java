package com.happy.prouter_api;

/**
 * 获取参数规则
 */
public interface ParameterGet {
    /**
     * 目标对象.属性名 = getIntent().属性类型.. 完成赋值操作
     *
     * @param targetParameter 目标对象，eg. MainActivity中的各种属性
     */
    void getParameter(Object targetParameter);
}
