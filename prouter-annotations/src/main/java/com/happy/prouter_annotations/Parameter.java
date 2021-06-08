package com.happy.prouter_annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)// 该注解在类中字段上起作用
@Retention(RetentionPolicy.CLASS)//编译时期进行预处理操作
public @interface Parameter {
    // 不填写name的注解值表示该属性名就是key,填写了就用注解值作为key
    String name() default "";
}
