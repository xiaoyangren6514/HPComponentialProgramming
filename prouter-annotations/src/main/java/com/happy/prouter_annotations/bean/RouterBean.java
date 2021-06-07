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

    public void setTypeEnum(TypeEnum typeEnum) {
        this.typeEnum = typeEnum;
    }

    public void setElement(Element element) {
        this.element = element;
    }

    public void setMyClass(Class<?> myClass) {
        this.myClass = myClass;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    private RouterBean(TypeEnum typeEnum, Class<?> myClass, String path, String group) {
        this.typeEnum = typeEnum;
        this.myClass = myClass;
        this.path = path;
        this.group = group;
    }

    /**
     * 对外暴露
     * 对外地提供简易版构造方法，方便APT生成代码
     */
    public static RouterBean create(TypeEnum type, Class<?> clazz, String path, String group) {
        return new RouterBean(type, clazz, path, group);
    }

    private RouterBean(Builder builder) {
        this.typeEnum = builder.type;
        this.element = builder.element;
        this.myClass = builder.clazz;
        this.path = builder.path;
        this.group = builder.group;
    }

    public static class Builder {
        private TypeEnum type;
        private Element element;
        private Class<?> clazz;
        private String path;
        private String group;

        public Builder addType(TypeEnum type) {
            this.type = type;
            return this;
        }

        public Builder addElement(Element element) {
            this.element = element;
            return this;
        }

        public Builder addClazz(Class<?> clazz) {
            this.clazz = clazz;
            return this;
        }

        public Builder addPath(String path) {
            this.path = path;
            return this;
        }

        public Builder addGroup(String group) {
            this.group = group;
            return this;
        }

        public RouterBean build() {
            if (path == null || path.length() == 0) {
                throw new IllegalArgumentException("path不能为空，eg. /app/MainActivity");
            }
            return new RouterBean(this);
        }
    }

    @Override
    public String toString() {
        return "RouterBean{" +
                "typeEnum=" + typeEnum +
                ", element=" + element +
                ", myClass=" + myClass +
                ", path='" + path + '\'' +
                ", group='" + group + '\'' +
                '}';
    }
}
