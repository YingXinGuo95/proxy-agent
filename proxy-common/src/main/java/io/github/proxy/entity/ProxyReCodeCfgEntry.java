package io.github.proxy.entity;

import io.github.proxy.annotation.ReCodeType;
import lombok.Data;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ProxyReCodeCfgEntry {
    //被代理类名
    private Class<?> sourceClass;
    //代理方法映射 key:被代理的方法名称
    private Map<Method, List<TargetProxy>> proxyMethod = new HashMap<>();

    @Data
    public static class TargetProxy {
        //代理使用的类
        private Class<?> tragetClass;
        //被代理的方法
        private Method method;
        //代理模式
        private ReCodeType type;
    }

}
