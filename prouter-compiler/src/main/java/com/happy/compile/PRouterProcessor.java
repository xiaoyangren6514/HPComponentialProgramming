package com.happy.compile;

import com.google.auto.service.AutoService;
import com.happy.compile.utils.ProcessorConfig;
import com.happy.compile.utils.ProcessorUtils;
import com.happy.prouter_annotations.PRouter;
import com.happy.prouter_annotations.bean.RouterBean;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import java.io.IOException;
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
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
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

    // 各个模块传递过来的模块名，eg. app order account
    private String options;

    // 各个模块传递过来的目录 用于统一存放 apt生成的文件
    private String aptPackage;

    // 仓库一 Path 缓存一
    // Map<"order",List<RouterBean>>
    private Map<String, List<RouterBean>> mAllPathMap = new HashMap<>();

    // 仓库二 Group 缓存二
    // Map<"order","PRouter$$Path$$order.class">
    private Map<String, String> mAllGroupMap = new HashMap<>();

    /**
     * 初始化函数，相当于Activity的onCreate
     *
     * @param processingEnv
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        elementTool = processingEnv.getElementUtils();
        messager = processingEnv.getMessager();
        filer = processingEnv.getFiler();
        typeTool = processingEnv.getTypeUtils();

        // 只有接收到App壳传递过来的数据，才能证明APT环境搭建完成
        options = processingEnv.getOptions().get(ProcessorConfig.OPTIONS);
        aptPackage = processingEnv.getOptions().get(ProcessorConfig.APT_PACKAGE);
        showLog("options:" + options);
        showLog("aptPackage:" + aptPackage);
        if (options != null && aptPackage != null) {
            showLog("APT环境搭建完成，options:" + options + ",aptPackage:" + aptPackage);
        } else {
            showLog("APT环境有问题，请检查options与aptPackage设置，不能为null哦");
        }
    }

    /**
     * 在App工程编译时执行，相当于main函数，开始处理注解
     * 注解处理器的和新方法，处理具体的注解，生成Java文件
     * 如果App工程没有使用注解，那该函数不会执行
     * true: 告诉apt 干完了
     * false: 告诉apt 不干了
     *
     * @param set              使用了支持处理注解的节点集合
     * @param roundEnvironment 当前或之前的允许环境，可以通过该对象查找注解
     * @return true 表示后续处理器不会再处理(已经处理完成)
     */
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        messager.printMessage(Diagnostic.Kind.NOTE, ">>>>> process run");
        if (set.isEmpty()) {
            showLog("没有发现被@PRouter注解的类");
            // 没有机会处理
            return false;
        }
        // 获取所有被@PRouter注解的元素集合
        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(PRouter.class);
        // 通过Element工具类，获取Activity，Callback类型
        TypeElement activityType = elementTool.getTypeElement(ProcessorConfig.ACTIVITY_PACKAGE);
        // 显示类信息(获取被注解的节点、类节点) 这也叫自描述 Mirror
        TypeMirror activityMirror = activityType.asType();
        // 遍历所有类节点
        for (Element element : elements) {
            // 获取简单类名 eg. MainActivity
            String className = element.getSimpleName().toString();
            showLog("被@PRouter注解的类：" + className);
            // 拿到注解
            PRouter pRouter = element.getAnnotation(PRouter.class);
            // 对路由对象进行封装
            RouterBean routerBean = new RouterBean.Builder()
                    .addGroup(pRouter.group())
                    .addPath(pRouter.path())
                    .addElement(element)
                    .build();
            TypeMirror elementMirror = element.asType();// 拿到当前元素的具体信息，eg. MainActivity Application
            if (typeTool.isSubtype(elementMirror, activityMirror)) {// android.app.Activity描述信息
                // 如果当前元素是activity
                routerBean.setTypeEnum(RouterBean.TypeEnum.ACTIVITY);
            } else {
                // 在非Activity类上使用该注解，抛出异常
                throw new RuntimeException("@PRouter注解目前只能作用在Activity上，不要乱写哦");
            }
            // 生成Path
            if (checkRouterPath(routerBean)) {
                showLog("RouterBean Check Success:" + routerBean.toString());
                List<RouterBean> routerBeans = mAllPathMap.get(routerBean.getGroup());
                // 如果map中没有该group数据，那么新建list集合再添加进map
                if (ProcessorUtils.isEmpty(routerBeans)) { // 仓库一 还为空时
                    routerBeans = new ArrayList<>();
                    routerBeans.add(routerBean);
                    mAllPathMap.put(routerBean.getGroup(), routerBeans);// 加入仓库一
                } else {
                    routerBeans.add(routerBean);
                }
            } else {
                showErrorLog("@PRouter注解未按规范配置，请查阅，eg. /app/MainActivity");
            }
        }
        // now mAllPathMap已经有值
        // 定义生成类文件实现的接口 Path Group
        TypeElement pathType = elementTool.getTypeElement(ProcessorConfig.PROUTER_API_PATH);// PRouterPath描述
        TypeElement groupType = elementTool.getTypeElement(ProcessorConfig.PROUTER_API_GROUP);// PRouterGroup描述
        // 生成PATH
        try {
            createPathFile(pathType);
        } catch (Exception e) {
            e.printStackTrace();
            showLog("生成PATH模板出现异常，错误为：" + e.getMessage());
        }
        // 生成GROUP
        try {
            createGroupFile(groupType, pathType);
        } catch (Exception e) {
            e.printStackTrace();
            showLog("生成GROUP模板出现异常，错误为：" + e.getMessage());
        }
//        genDemo2(roundEnvironment);
        return true;
    }

    /**
     * 生成路由组Group文件，eg. PRouter$$Group$$app
     *
     * @param groupType PRouterGroup接口信息
     * @param pathType  PRouterPath接口信息
     */
    private void createGroupFile(TypeElement groupType, TypeElement pathType) throws IOException {
        if (ProcessorUtils.isEmpty(mAllPathMap) || ProcessorUtils.isEmpty(mAllGroupMap)) return;
        // 返回值 Map<String,Class<? extends PRouterPath>>
        TypeName methodReturns = ParameterizedTypeName.get(
                ClassName.get(Map.class) // Map
                , ClassName.get(String.class) // Map<String,
                , ParameterizedTypeName.get(ClassName.get(Class.class)
                        , WildcardTypeName.subtypeOf(ClassName.get(pathType))) // ? extends PRouterPath
        );
        // public Map<String,Class<? extends PRouterPath>> getGroupMap
        MethodSpec.Builder methodBuild = MethodSpec.methodBuilder(ProcessorConfig.GROUP_METHOD_NAME)
                .addAnnotation(Override.class)// override注解
                .addModifiers(Modifier.PUBLIC)
                .returns(methodReturns);// 方法返回值
        // Map<String,Class<? extends PRouterPath>> groupMap = new HashMap<>();
        methodBuild.addStatement("$T<$T,$T> $N = new $T<>()"
                , ClassName.get(Map.class)
                , ClassName.get(String.class)
                , ParameterizedTypeName.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(ClassName.get(pathType)))
                , ProcessorConfig.GROUP_VAR1
                , HashMap.class
        );
        // groupMap.put("order",PRouter$$Path$$order.class)
        for (Map.Entry<String, String> entry : mAllGroupMap.entrySet()) {
            methodBuild.addStatement("$N.put($S,$T.class)",
                    ProcessorConfig.GROUP_VAR1
                    , entry.getKey()
                    , ClassName.get(aptPackage, entry.getValue()));
        }
        // return groupMap
        methodBuild.addStatement("return $N", ProcessorConfig.GROUP_VAR1);
        // 最终生成的文件
        String finalClassName = ProcessorConfig.GROUP_FILE_NAME + options;
        showLog("APT生成路由组Group文件：" + aptPackage + "." + finalClassName);
        // 生成类文件
        JavaFile javaFile = JavaFile.builder(aptPackage//包名
                , TypeSpec.classBuilder(finalClassName) // 类名
                        .addSuperinterface(ClassName.get(groupType))//实现PRouterGroup接口
                        .addModifiers(Modifier.PUBLIC) // public修饰符
                        .addMethod(methodBuild.build()) // 方法构建(方法参数+方法太)
                        .build()//类构建完成
        ).build();// JavaFile构建完成
        javaFile.writeTo(filer);
    }

    /**
     * 生成Path
     *
     * @param pathType
     */
    private void createPathFile(TypeElement pathType) throws IOException {
        if (ProcessorUtils.isEmpty(mAllPathMap)) {
            return;
        }
        showLog("pathType:" + pathType);
        // 倒序生成代码
        // 1. 生成方法
        // Map<String,RouterBean>
        TypeName methodReturn = ParameterizedTypeName.get(
                ClassName.get(Map.class) // Map
                , ClassName.get(String.class)// Map<String,
                , ClassName.get(RouterBean.class)// Map<String,RouterBean>
        );
        for (Map.Entry<String, List<RouterBean>> entry : mAllPathMap.entrySet()) {
            MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(ProcessorConfig.PATH_METHOD_NAME)
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(methodReturn);
            // 生成 Map<String,RouterBean> pathMap = new HashMap<>(); $N == 变量
            methodBuilder.addStatement("$T<$T,$T> $N = new $T<>()"
                    , ClassName.get(Map.class) // Map
                    , ClassName.get(String.class) // Map<String,
                    , ClassName.get(RouterBean.class) // Map<String,RouterBean>
                    , ProcessorConfig.PATH_VAR1 // Map<String,RouterBean> pathMap
                    , ClassName.get(HashMap.class)// Map<String,RouterBean> pathMap = new HashMap<>();
            );
            // 循环 pathMap.put("/order/main",RouterBean.create(RouterBean.TypeEnum.ACTIVITY))
            // $N:变量  $L:枚举
            for (RouterBean routerBean : entry.getValue()) {
                methodBuilder.addStatement("$N.put($S, $T.create($T.$L,$T.class,$S,$S))"
                        , ProcessorConfig.PATH_VAR1 // pathMap.put
                        , routerBean.getPath() // "/order/main"
                        , ClassName.get(RouterBean.class)// RouterBean
                        , ClassName.get(RouterBean.TypeEnum.class)// RouterBean.Type
                        , routerBean.getTypeEnum()// 枚举类型 ACTIVITY
                        , ClassName.get((TypeElement) routerBean.getElement())// MainActivity.class
                        , routerBean.getPath() // 路径名
                        , routerBean.getGroup() // 组名
                );
            }
            // 拼接返回 return pathMap
            methodBuilder.addStatement("return $N", ProcessorConfig.PATH_VAR1);
            // 内部有implements，所以方法和类要合为一体生成才行，特殊情况
            // 最终生成的类文件名 PRouter$$Path$$order
            String finalClassName = ProcessorConfig.PATH_FILE_NAME + entry.getKey();
            showLog("key:" + entry.getKey());
            showLog("APT生成路由Path类文件：" + aptPackage + "." + finalClassName);
            // 生成类文件
            JavaFile.builder(aptPackage//包名 APT存放的路径
                    , TypeSpec.classBuilder(finalClassName) // 类名
                            .addModifiers(Modifier.PUBLIC) // public修饰符
                            .addSuperinterface(ClassName.get(pathType)) //实现PRouterPath接口
                            .addMethod(methodBuilder.build()) // 方法构建
                            .build())// 类构件完成
                    .build()// javaFile构建完成
                    .writeTo(filer);
            //  缓存二， PATH路径文件生成出来，赋值给mAllGroupMap
            mAllGroupMap.put(entry.getKey(), finalClassName);
        }
    }

    /**
     * 校验@PRouter注解的值，如果group未填写就从必填项Path中截取
     *
     * @param bean 路由详细进行，最终实体封装类
     * @return 是否合格
     */
    private final boolean checkRouterPath(RouterBean bean) {
        String group = bean.getGroup();
        String path = bean.getPath();
        if (ProcessorUtils.isEmpty(path) || !path.startsWith("/")) {
            showErrorLog("@PRouter注解中的Path值，必须要以/开头");
            return false;
        }
        if (path.lastIndexOf("/") == 0) {
            showErrorLog("@PRouter注解未按规范配置，eg. /app/MainActivity");
            return false;
        }
        String finalGroup = path.substring(1, path.indexOf("/",1));
        showLog("finalGroup:" + finalGroup);
        if (!ProcessorUtils.isEmpty(group) && !group.equals(options)) {
            showErrorLog("@PRouter注解中的group值必须和子模块名保持一致");
            return false;
        } else {
            bean.setGroup(finalGroup);
        }
        return true;
    }

    private void genDemo2(RoundEnvironment roundEnvironment) {
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
    }

    private void showLog(String msg) {
        // 如果在注解处理器抛出异常，可以使用Diagnostic.Kind.ERROR
        messager.printMessage(Diagnostic.Kind.NOTE, ">>>>> " + msg);
    }

    private void showErrorLog(String msg) {
        messager.printMessage(Diagnostic.Kind.ERROR, ">>>>>" + msg);
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
