package com.happy.compile;

import com.google.auto.service.AutoService;
import com.happy.prouter_annotations.PRouter;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

/**
 * 注解处理器
 */
@AutoService(Processor.class) //启用服务
@SupportedAnnotationTypes({"com.happy.prouter_annotations.PRouter"}) //注解
@SupportedSourceVersion(SourceVersion.RELEASE_8) // 环境版本
//接受Android工程传递的参数
@SupportedOptions("hp")
public class PRouterProcessor extends AbstractProcessor {

    // 操作Element的工具类(类、函数 属性 都是element)
    private Elements elementTool;

    //    type(类信息)的工具类，包含用于操作TypeMirror的工具方法
    private Types typeTool;

    //    message用来打印日志相关信息
    private Messager messager;

    // 文件生成器，类 资源等最终要生成的文件需要Filer来完成
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        elementTool = processingEnv.getElementUtils();
        messager = processingEnv.getMessager();
        filer = processingEnv.getFiler();
//        接收传参
        String hp = processingEnv.getOptions().get("hp");
//        如果在注解处理器抛出异常，可以使用Diagnostic.Kind.ERROR
        messager.printMessage(Diagnostic.Kind.NOTE, ">>>>" + hp);
    }

    /**
     * 在App工程编译时执行
     * 如果App工程没有使用注解，那该函数不会执行
     * true: 告诉apt 干完了
     * false: 告诉apt 不干了
     *
     * @param set
     * @param roundEnvironment
     * @return
     */
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        messager.printMessage(Diagnostic.Kind.NOTE, ">>>>> process run");
        if (set.isEmpty()) return false;
        //获取被PRouter注解的类节点信息
        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(PRouter.class);
        for (Element element : elements) { // 此处为3次 MainActivity MainActivity2 MainActivity3
//            要生成的文件名称
            String className = element.getSimpleName().toString();
            showLog("被@PRouter注解的类有：" + className);
            String finalClassName = className + "$$$$$$$$PRouter";
            PRouter pRouter = element.getAnnotation(PRouter.class);
            String packageName = elementTool.getPackageOf(element).getQualifiedName().toString();
            showLog("packageName:" + packageName);
//            1. 方法
            MethodSpec findTargetClass = MethodSpec.methodBuilder("findTargetClass")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(String.class, "path")
                    .returns(Class.class)
                    .addStatement("return path.equals($S) ? $T.class : null", pRouter.path(), ClassName.get((TypeElement) element))
                    .build();
//            2. 类
            TypeSpec typeClass = TypeSpec.classBuilder(finalClassName)
                    .addMethod(findTargetClass)
                    .build();
//            3. 包
            JavaFile javaFile = JavaFile.builder(packageName, typeClass).build();
            try {
                javaFile.writeTo(filer);
                showLog(finalClassName + "类生成成功");
            } catch (IOException e) {
                e.printStackTrace();
                showLog(finalClassName + "类生成失败，错误为：" + e.getMessage());
            }

//            generateTestFile();
        }
        return true;
    }

    private void showLog(String msg) {
        messager.printMessage(Diagnostic.Kind.NOTE, ">>>>> " + msg);
    }

    private void generateTestFile() {
        /*
        *   package com.happy.demo;

            import java.lang.System;

            public final class HPTest {
              public static void main(System[] args) {
                System.out.println("Hello javaPoet");
              }
            }
        * */

        //            1. 方法
        MethodSpec mainMethod = MethodSpec.methodBuilder("main")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(void.class)
                .addParameter(String[].class, "args")
                .addStatement("$T.out.println($S)", System.class, "Hello javaPoet")
                .build();
//            2. 类
        TypeSpec testClass = TypeSpec.classBuilder("HPTest")
                .addMethod(mainMethod)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .build();
//            3. 包
        JavaFile packageF = JavaFile.builder("com.happy.demo", testClass).build();
//            生成文件
        try {
            packageF.writeTo(filer);
        } catch (IOException e) {
            e.printStackTrace();
            messager.printMessage(Diagnostic.Kind.NOTE, "生成HPTest文件失败，错误：" + e.getMessage());
        }
    }

}
