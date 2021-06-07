package com.happy.compile.utils;

public interface ProcessorConfig {

    String ACTIVITY_PACKAGE = "android.app.Activity";

    // @PRouter注解的包名+类名
    String PROUTER_PACKAGE = "com.happy.prouter_api";

    // 接受参数的TAG标记
    // 接收每个module名称
    String OPTIONS = "moduleName";

    // 接收包名 APT存放包名
    String APT_PACKAGE = "packageNameForAPT";

    // PRouter api的PRouterGroup 高层标准
    String PROUTER_API_GROUP = PROUTER_PACKAGE + ".PRouterGroup";

    // PRouter api的PRouterPath 高层标准
    String PROUTER_API_PATH = PROUTER_PACKAGE + ".PRouterPath";

    // 路由组Path中方法名
    String PATH_METHOD_NAME = "getPathMap";

    // 路由组Group中方法名
    String GROUP_METHOD_NAME = "getGroupMap";

    // 路由组中Path里面变量名
    String PATH_VAR1 = "pathMap";

    // 路由组中Group里面变量名
    String GROUP_VAR1 = "groupMap";

    // 路由组，PATH最终要生成的文件名
    String PATH_FILE_NAME = "PRouter$$Path$$";

    // 路由组，GROUP最终要生成的文件名
    String GROUP_FILE_NAME = "PRouter$$Group$$";

}
