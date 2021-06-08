package com.happy.compile;

import com.google.auto.service.AutoService;
import com.happy.compile.utils.ProcessorConfig;
import com.happy.compile.utils.ProcessorUtils;
import com.happy.prouter_annotations.Parameter;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

/**
 * 参数直接处理器
 */
@AutoService(Processor.class) // 开启
@SupportedAnnotationTypes({ProcessorConfig.PARAMETER_PACKAGE})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ParameterProcessor extends AbstractProcessor {

    private Elements elementUtils;// 类信息
    private Types typeUtils;//具体类型
    private Messager messager;//日志
    private Filer filer;// 生成器

    // 定义缓存仓库，key为当前Activity，eg AccountCenterActivity, value: list=>age name sex
    private Map<TypeElement, List<Element>> tempParameterMap = new HashMap<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        elementUtils = processingEnv.getElementUtils();
        typeUtils = processingEnv.getTypeUtils();
        messager = processingEnv.getMessager();
        filer = processingEnv.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        /*
        默认会调用两次，一次为执行，一次为检测(内部机制检测build生成成功没)
        下面直接return false就不需要这个判断了
        if (set.isEmpty()){
            return false;
        }
        */
        if (!ProcessorUtils.isEmpty(set)) {
            Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(Parameter.class);
            // 往仓库添加信息
            for (Element element : elements) {
                // element 为name age sex这些参数，  getEnclosingElement获取属性节点的父节点，也就是类节点
                TypeElement typeElement = (TypeElement) element.getEnclosingElement();
                if (!tempParameterMap.containsKey(typeElement)) {
                    List<Element> list = new ArrayList<>();
                    list.add(element);
                    tempParameterMap.put(typeElement, list);
                } else {
                    tempParameterMap.get(typeElement).add(element);
                }
            }
            // 生成类文件
            if (ProcessorUtils.isEmpty(tempParameterMap)) return true;
            TypeElement activityType = elementUtils.getTypeElement(ProcessorConfig.ACTIVITY_PACKAGE);
            TypeElement parameterType = elementUtils.getTypeElement(ProcessorConfig.PROUTER_API_PARAMETER_GET);
            // 参数 Object targetParameter
            ParameterSpec parameterSpec = ParameterSpec.builder(TypeName.OBJECT, ProcessorConfig.PARAMETER_NAME).build();
            for (Map.Entry<TypeElement, List<Element>> entry : tempParameterMap.entrySet()) {
                // key: OrderMainActivity value:[age name sex]
                TypeElement typeElement = entry.getKey();
                if (!typeUtils.isSubtype(typeElement.asType(), activityType.asType())) {
                    throw new RuntimeException("@Parameter注解目前只能在Activity类之上哦");
                }
                // 获取类名 OrderMainActivity
                ClassName className = ClassName.get(typeElement);
                String finalClassName = typeElement.getSimpleName() + ProcessorConfig.PARAMETER_FILE_NAME;
                showLog("APT生成获取参数类文件：" + className.packageName() + "." + finalClassName);
                // 生成方法
                ParameterFactory factory = new ParameterFactory.Builder(parameterSpec)
                        .setMessager(messager)
                        .setClassName(className)
                        .build();
                // OrderMainActivity t = (OrderMainActivity)targetParameter;
                factory.addFirstStatement();
                for (Element element : entry.getValue()) {
                    factory.buildStatement(element);
                }
                // 最终生成的类名 类名$$Parameter
                try {
                    JavaFile.builder(className.packageName()//包名
                            , TypeSpec.classBuilder(finalClassName)// 类名
                                    .addSuperinterface(ClassName.get(parameterType))
                                    .addModifiers(Modifier.PUBLIC)
                                    .addMethod(factory.build())
                                    .build())
                            .build()
                            .writeTo(filer);
                    showLog("APT生成获取参数类文件成功");
                } catch (Exception e) {
                    e.printStackTrace();
                    showLog("APT生成获取参数类文件失败，错误：" + e.getMessage());
                }
            }

        }
        return false;// 只执行一次
    }

    private void showLog(String msg) {
        // 如果在注解处理器抛出异常，可以使用Diagnostic.Kind.ERROR
        messager.printMessage(Diagnostic.Kind.NOTE, ">>>>> " + msg);
    }

    private void showErrorLog(String msg) {
        messager.printMessage(Diagnostic.Kind.ERROR, ">>>>>" + msg);
    }
}
