package com.happy.compile;

import com.happy.compile.utils.ProcessorConfig;
import com.happy.compile.utils.ProcessorUtils;
import com.happy.prouter_annotations.Parameter;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

public class ParameterFactory {

    // 方法构建
    private MethodSpec.Builder method;

    // 日志打印
    private Messager messager;

    // 类名，eg. MainActivity
    private ClassName className;

    private ParameterFactory(Builder builder) {
        this.className = builder.className;
        this.messager = builder.messager;
        // 生成方法
        // public void getParameter(Object targetParameter){}
        this.method = MethodSpec.methodBuilder(ProcessorConfig.PARAMETER_METHOD_NAME)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(builder.parameterSpec);

    }

    /**
     * 生成
     * OrderMainActivity t = (OrderMainActivity)targetParameter;
     */
    public void addFirstStatement() {
        method.addStatement("$T t = ($T)" + ProcessorConfig.PARAMETER_NAME, className, className);
    }

    /**
     * 构建方法体内容
     * eg. t.name = t.getIntent.getStringExtra("name")
     *
     * @param element 被注解的属性元素
     */
    public void buildStatement(Element element) {
        TypeMirror typeMirror = element.asType();
        // 获取TypeKind枚举类型的序列号
        int ordinal = typeMirror.getKind().ordinal();
        // 获取属性名 eg. name age sex
        String fieldName = element.getSimpleName().toString();
        // 获取注解的值
        String annotationValue = element.getAnnotation(Parameter.class).name();
        annotationValue = ProcessorUtils.isEmpty(annotationValue) ? fieldName : annotationValue;
        // t.name = getIntent()
        String finalValue = "t." + fieldName;
        String methodContent = finalValue + " = t.getIntent().";
        if (ordinal == TypeKind.INT.ordinal()) {
            // t.s = t.getIntent().getIntExtra("age",t.age);
            methodContent += "getIntExtra($S," + finalValue + ")";
        } else if (ordinal == TypeKind.BOOLEAN.ordinal()) {
            methodContent += "getBooleanExtra($S," + finalValue + ")";
        } else {// String类型 没有序列号的提供，需手动完成
            if (typeMirror.toString().equalsIgnoreCase(ProcessorConfig.STRING)) {
                methodContent += "getStringExtra($S)";
            }
        }
        messager.printMessage(Diagnostic.Kind.NOTE, "拼接："+methodContent);
        if (methodContent.endsWith(")")) {
            method.addStatement(methodContent, annotationValue);
        } else {
            messager.printMessage(Diagnostic.Kind.ERROR, "目前只支持String、int和boolean传参哦");
        }
    }

    public MethodSpec build() {
        return method.build();
    }


    public static class Builder {
        private Messager messager;
        private ClassName className;
        private ParameterSpec parameterSpec;

        public Builder(ParameterSpec parameterSpec) {
            this.parameterSpec = parameterSpec;
        }

        public Builder setMessager(Messager messager) {
            this.messager = messager;
            return this;
        }

        public Builder setClassName(ClassName className) {
            this.className = className;
            return this;
        }

        public ParameterFactory build() {
            if (parameterSpec == null) {
                throw new IllegalArgumentException("parameterSpec方法参数体为空");
            }
            if (className == null) {
                throw new IllegalArgumentException("方法内容中ClassName为空");
            }
            if (messager == null) {
                throw new IllegalArgumentException("messager不能为空");
            }
            return new ParameterFactory(this);
        }
    }


}
