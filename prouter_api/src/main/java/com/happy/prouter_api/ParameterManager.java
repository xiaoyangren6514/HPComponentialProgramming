package com.happy.prouter_api;

import android.app.Activity;
import android.util.LruCache;

public class ParameterManager {

    private static ParameterManager mInstance;
    // key:类名  value:参数加载接口
    private LruCache<String, ParameterGet> cache;

    private ParameterManager() {
        cache = new LruCache<>(100);
    }

    public static ParameterManager getInstance() {
        if (mInstance == null) {
            synchronized (ParameterManager.class) {
                if (mInstance == null) {
                    mInstance = new ParameterManager();
                }
            }
        }
        return mInstance;
    }

    static final String FILE_SUFFIX_NAME = "$$Parameter";

    /**
     * 使用者调用这个方法就可以进行参数的接收
     *
     * @param activity
     */
    public void loadParameter(Activity activity) {
        String className = activity.getClass().getName();//className=OrderMainActivity
        ParameterGet parameterLoad = cache.get(className);
        if (parameterLoad == null) {
            Class<?> aClass = null;
            try {
                aClass = Class.forName(className + FILE_SUFFIX_NAME);
                parameterLoad = (ParameterGet) aClass.newInstance();
                cache.put(className, parameterLoad);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        parameterLoad.getParameter(activity);
    }

}
