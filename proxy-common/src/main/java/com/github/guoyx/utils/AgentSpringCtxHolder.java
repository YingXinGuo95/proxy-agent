package com.github.guoyx.utils;

import lombok.SneakyThrows;

import java.lang.reflect.Method;
import java.util.Map;

public class AgentSpringCtxHolder {
    private static Object applicationContext;
    private static Method getBeanByType;
    private static Method getBeansOfType;

    @SneakyThrows
    public static void initCtx(Object context) {
        Class<?> clz = Class.forName("org.springframework.context.ApplicationContext");
        if (!clz.isInstance(context)) {
            throw new IllegalArgumentException("context must be org.springframework.context.ApplicationContext");
        }
        applicationContext = context;
        getBeanByType =  context.getClass().getMethod("getBean", Class.class);
        getBeansOfType =  context.getClass().getMethod("getBeansOfType", Class.class);
    }

    public static void initProxyBean() {
        //初始化注解模式下的配置
        ConfigHolder.getInstance().initAnnotation();
    }

    @SneakyThrows
    public static Object getBeanByType(Class<?> clz) {
        return getBeanByType.invoke(applicationContext, clz);
    }

    @SneakyThrows
    public static <T> Map<String, T> getBeansOfType(Class<?> clz) {
        return (Map<String, T>) getBeansOfType.invoke(applicationContext, clz);
    }

}
