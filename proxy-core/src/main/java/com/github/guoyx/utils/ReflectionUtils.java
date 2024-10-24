package com.github.guoyx.utils;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * 通过指定classLoader反射调用
 */
public class ReflectionUtils {

    public static <T> T invokeMethod(Class<?> clz, Object instance, String methodName, Object... args) {
        try {
            Class<?> clazz = getCurThreadClassLoader().loadClass(clz.getName());
            Method method = clazz.getMethod(methodName);
            return (T) method.invoke(instance, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ClassLoader getCurThreadClassLoader() {
        return Optional.of(Thread.currentThread())
                .map(Thread::getContextClassLoader)
                .orElse(ClassLoader.getSystemClassLoader());
    }

}
