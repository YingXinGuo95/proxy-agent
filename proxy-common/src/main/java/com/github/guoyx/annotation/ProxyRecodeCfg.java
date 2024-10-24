package com.github.guoyx.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ProxyRecodeCfg {


    /**
     * 代理目标类名称
     */
    String proxyClassName() default "";

    /**
     * 代理目标类，proxyClassName有值时可以不填，优先使用proxyClassName
     */
    Class<?> proxyClass() default Void.class;

    /**
     * 代理方法名称
     */
    String method();

    /**
     * 字节码执行类型
     */
    ReCodeType type() default ReCodeType.OVERRIDER;

}
