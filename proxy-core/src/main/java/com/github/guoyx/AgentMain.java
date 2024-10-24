package com.github.guoyx;

import com.github.guoyx.transformer.ReCoderTransformer;
import com.github.guoyx.utils.ClassLoaderOptUtils;
import com.github.guoyx.utils.ReflectionUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.lang.instrument.Instrumentation;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class AgentMain {

    @SneakyThrows
    public static void agentmain(String args, Instrumentation ins) {
        try {
            ClassLoader classLoader = null;
            for (Class<?> loadedClass : ins.getAllLoadedClasses()) {
                String clzName = Optional.of(loadedClass)
                        .map(Class::getClassLoader)
                        .map(Object::getClass)
                        .map(Class::getName)
                        .orElse(null);
                if ("org.springframework.boot.loader.LaunchedURLClassLoader".equals(clzName)) {
                    classLoader = loadedClass.getClassLoader();
                    break;
                }
            }
            //优先通过springBoot的classloader获取ConfigHolder
            classLoader = classLoader == null ? ReflectionUtils.getCurThreadClassLoader() : classLoader;

            //注册Transformer
            ins.addTransformer(new ReCoderTransformer(), true);

            //重新定义已加载的类
            redefineClass(ins, classLoader);
        } catch (Exception e) {
            log.error("agent main run error", e);
        }
    }

    private static void redefineClass(Instrumentation ins, ClassLoader classLoader) throws Exception {
        long start = System.currentTimeMillis();
        Class<?>[] cls = ins.getAllLoadedClasses();

        Set<String> redefineClzSet = ClassLoaderOptUtils.getReCoderMap(classLoader).keySet().stream()
                .map(Class::getName)
                .collect(Collectors.toSet());

        for (Class<?> cl : cls) {
            if(!redefineClzSet.contains(cl.getName())) {
                //未配置的重写类，跳过
                continue;
            }
            if (!ins.isModifiableClass(cl) || cl.getName().startsWith("java.") || cl.getName().startsWith("sun.")
                    || cl.isAnonymousClass() || cl.isLocalClass() || cl.isArray() || cl.isInterface() || cl.isEnum()) {
               log.error("[proxy-agent] 类{} 不允许重写，跳过...", cl.getName());
               continue;
            }
            //重写类字节码
            ins.retransformClasses(cl);
        }
        log.info("[proxy-agent] redefine loaded class complete, cost:{}ms", System.currentTimeMillis() - start);
    }

}
