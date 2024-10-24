package com.github.guoyx.utils;

import com.github.guoyx.entity.ProxyReCodeCfgEntry;
import lombok.SneakyThrows;

import java.util.Map;

/**
 * 通过指定classLoader获取配置信息
 */
public class ClassLoaderOptUtils {

    @SneakyThrows
    public static Object getConfigHolder(ClassLoader classLoader) {
        Class<?> clz = classLoader.loadClass(ConfigHolder.class.getName());
        return clz.getDeclaredMethod("getInstance").invoke(null);
    }

    @SneakyThrows
    public static Map<Class<?>, ProxyReCodeCfgEntry> getReCoderMap(ClassLoader classLoader) {
        Object configHolder = getConfigHolder(classLoader);

        Class<?> clz = classLoader.loadClass(ConfigHolder.class.getName());
        return (Map<Class<?>, ProxyReCodeCfgEntry>) clz.getField("reCoderMap").get(configHolder);
    }

}
