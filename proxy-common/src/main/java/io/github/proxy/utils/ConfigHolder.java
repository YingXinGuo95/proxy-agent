package io.github.proxy.utils;


import io.github.proxy.annotation.ProxyRecodeCfg;
import io.github.proxy.entity.ProxyReCodeCfgEntry;
import io.github.proxy.service.ProxyReCode;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ConfigHolder {

    private volatile static ConfigHolder instance = null;

    //注解方式的重写配置
    public final Map<Class<?>, ProxyReCodeCfgEntry> reCoderMap = new HashMap<>();

    private ConfigHolder() {}

    public static ConfigHolder getInstance() {
        if (instance == null) {
            synchronized (ConfigHolder.class) {
                if (instance == null) {
                    instance = new ConfigHolder();
                }
            }
        }
        return instance;
    }

    /**
     * 初始化注解配置
     */
    public void initAnnotation() {
        //注解模式下重写类集合
        Map<String, ProxyReCode> map = AgentSpringCtxHolder.getBeansOfType(ProxyReCode.class);
        if (map == null || map.size() == 0) {
            return;
        }

        for (ProxyReCode coder : map.values()) {
            for (Method method : coder.getClass().getDeclaredMethods()) {
                ProxyRecodeCfg cfg = method.getAnnotation(ProxyRecodeCfg.class);
                if (cfg == null) {
                    continue;
                }
                String targetClass = Void.class.equals(cfg.proxyClass())  ? cfg.proxyClassName() : cfg.proxyClass().getName();
                if (cfg.method() == null || targetClass == null || targetClass.isEmpty()) {
                    continue;
                }

                Class<?> targetClz;
                try {
                   targetClz = Class.forName(targetClass);
                } catch (ClassNotFoundException e) {
                    continue;
                }

                //将注解信息放入缓存
                ProxyReCodeCfgEntry configs = reCoderMap.getOrDefault(targetClz, new ProxyReCodeCfgEntry());
                configs.setSourceClass(targetClz);
                Map<Method, List<ProxyReCodeCfgEntry.TargetProxy>> proxyMethods = configs.getProxyMethod();

                try {
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    Method proxyMethod = targetClz.getDeclaredMethod(cfg.method(), parameterTypes);
                    proxyMethod.setAccessible(true);

                    ProxyReCodeCfgEntry.TargetProxy proxy = new ProxyReCodeCfgEntry.TargetProxy();
                    proxy.setMethod(proxyMethod);
                    proxy.setTragetClass(coder.getClass());
                    proxy.setType(cfg.type());

                    List<ProxyReCodeCfgEntry.TargetProxy> proxyMethodArr = proxyMethods.getOrDefault(method, new ArrayList<>());
                    proxyMethodArr.add(proxy);
                    proxyMethods.put(method, proxyMethodArr);

                    reCoderMap.put(targetClz, configs);

                } catch (NoSuchMethodException e) {
//                    log.error("class[{}] not exits method[{}] param[{}]", targetClz, cfg.method(),
//                            method.getParameterTypes());

                }
            }
        }
    }

}
