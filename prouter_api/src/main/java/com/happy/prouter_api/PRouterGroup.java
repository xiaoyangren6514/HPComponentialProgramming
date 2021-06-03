package com.happy.prouter_api;

import java.util.Map;

/**
 * group分组
 */
public interface PRouterGroup {
    /**
     * eg. order组，account组
     * "order"--->PRouterPath实现类--->APT生成PRouter$$Path$$order
     *
     * @return key:"order/app" value: order组下所有的(path---class)
     */
    Map<String, Class<? extends PRouterPath>> getGroupMap();
}
