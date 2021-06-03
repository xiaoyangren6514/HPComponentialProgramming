package com.happy.prouter_annotations.bean;

import javax.lang.model.element.Element;

public class RouterBean {

    public enum TypeEnum {
        ACTIVITY
    }

    private TypeEnum typeEnum;// 枚举类型：Activity
    private Element element;// 类节点
    private Class<?> myClass;// 备注接的Class对象，eg. MainActivity.class
    private String path; // 路由地址 eg. /app/MainActivity
    private String group; // 路由组 eg. app order account

    public TypeEnum getTypeEnum() {
        return typeEnum;
    }

    public Element getElement() {
        return element;
    }

    public Class<?> getMyClass() {
        return myClass;
    }

    public String getPath() {
        return path;
    }

    public String getGroup() {
        return group;
    }
}
