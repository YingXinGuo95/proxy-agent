package io.github.proxy.transformer;

import io.github.proxy.annotation.ReCodeType;
import io.github.proxy.entity.ProxyReCodeCfgEntry;
import io.github.proxy.utils.AgentSpringCtxHolder;
import io.github.proxy.utils.ConfigHolder;
import io.github.proxy.utils.ProxyReferenceCtx;
import io.github.proxy.utils.ReflectionUtils;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 注解方式字节码覆盖
 */
@Slf4j
public class ReCoderTransformer implements ClassFileTransformer {

    @SneakyThrows
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        ClassLoader originLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(loader);

        try {
            Map<Class<?>, ProxyReCodeCfgEntry> reCoderMap = getReCoderMap();
            if (reCoderMap == null || !reCoderMap.containsKey(classBeingRedefined)) {
                return null;
            }

            Class<?> cfgEntryClz = loader.loadClass(ProxyReCodeCfgEntry.class.getName());
            Object cfgEntry = reCoderMap.get(classBeingRedefined);

            Map<Method, Object> proxyMap = ReflectionUtils.invokeMethod(cfgEntryClz, cfgEntry, "getProxyMethod");
            if (proxyMap == null || proxyMap.isEmpty()) {
                return null;
            }
            log.info("[proxy-agent] 开始重写类:[{}]字节码", classBeingRedefined.getName());

            ClassPool classPool = new ClassPool(true);
            classPool.appendClassPath(new javassist.ClassClassPath(classBeingRedefined));
            CtClass ctClass = classPool.get(classBeingRedefined.getName());

            //根据注解配置，将类对应的方法做字节码调整。（可能同时存在多种type的重写）
            for (Map.Entry<Method, Object> proxyEntry : proxyMap.entrySet()) {
                //遍历所有需要重写的方法
                Method proxyMethod = proxyEntry.getKey();
                List<Object> proxyArr = (List<Object>) proxyEntry.getValue();
                if (proxyArr == null) {
                    continue;
                }
                for (Object methodConfig : proxyArr) {
                    //遍历该方法的所有重写配置
                    codecMethod(proxyMethod, methodConfig, ctClass, classPool);
                }
            }

            byte[] bytecode = ctClass.toBytecode();
            saveClassFile(classBeingRedefined.getName(), bytecode);
            return bytecode;

        } catch (Exception e) {
            log.error("[proxy-agent] 类:[{}]代理异常", classBeingRedefined, e);

        } finally {
            Thread.currentThread().setContextClassLoader(originLoader);
        }

        return null;
    }

    public static void saveClassFile(String filename, byte [] data)throws Exception{
        String property = System.getProperty("debug.saveCodeFile");
        if (property == null || property.isEmpty() || !"true".equalsIgnoreCase(property)) {
            return;
        }
        if(data == null || filename == null || filename.isEmpty()){
            return;
        }
        Path path = Paths.get("/data/agent/code/");
        Files.createDirectories(path);

        String filepath ="/data/agent/code/" +  filename + ".class";
        File file  = new File(filepath);
        if(file.exists()){
            file.delete();
        }
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(data,0, data.length);
        fos.flush();
        fos.close();
    }


    @SneakyThrows
    private Map<Class<?>, ProxyReCodeCfgEntry> getReCoderMap() {
        try {
            Class<?> clz = ReflectionUtils.getCurThreadClassLoader().loadClass(ConfigHolder.class.getName());
            Object configHolder = clz.getDeclaredMethod("getInstance").invoke(null);
            return (Map<Class<?>, ProxyReCodeCfgEntry>) clz.getField("reCoderMap").get(configHolder);

        } catch (ClassNotFoundException e) {
            //基础、自定义classloader可能会加载不到ConfigHolder.class，这种请求直接返回
            return null;
        }
    }

    /**
     * 字节码覆盖
     * @param proxyMethod 代理使用的方法
     * @param methodConfig 代理配置
     * @param srcClass 被代理的类
     */
    private void codecMethod(Method proxyMethod, Object methodConfig, CtClass srcClass, ClassPool classPool) {
        try {
            List<CtClass> args = new ArrayList<>();

            Method cfgMethod = ReflectionUtils.invokeMethod(ProxyReCodeCfgEntry.TargetProxy.class, methodConfig, "getMethod");
            Object type = ReflectionUtils.invokeMethod(ProxyReCodeCfgEntry.TargetProxy.class, methodConfig, "getType");
            Class<?> targetClz = ReflectionUtils.invokeMethod(ProxyReCodeCfgEntry.TargetProxy.class, methodConfig, "getTragetClass");

            for (Class<?> arg : cfgMethod.getParameterTypes()) {
                CtClass ctClass = classPool.get(arg.getName());
                args.add(ctClass);
            }

            CtMethod method = srcClass.getDeclaredMethod(cfgMethod.getName(), args.toArray(new CtClass[]{}));
            StringBuilder methodInvokeBody = new StringBuilder("()");
            if (args.size() > 0) {
                methodInvokeBody = new StringBuilder("(");
                for (int i = 1; i <= args.size(); i++) {
                    methodInvokeBody.append("$").append(i).append(",");
                }
                methodInvokeBody = new StringBuilder(methodInvokeBody.substring(0, methodInvokeBody.length() - 1) + ")");
            }
            String refCtxClz = ProxyReferenceCtx.class.getName();
            String ctxClz = AgentSpringCtxHolder.class.getName();

            boolean isStatic = Modifier.isStatic(cfgMethod.getModifiers());
            String initBody = isStatic ? "" : refCtxClz + ".init($0);";
            boolean notHasReturn = "void".equalsIgnoreCase(method.getReturnType().getName());

            boolean isStaticProxyMethod = Modifier.isStatic(proxyMethod.getModifiers());

            String methodBody = "{" +
                    initBody + //放置当前上下文引用
                    "try {" +
                    "   Class clz = Class.forName(\""+ targetClz.getName() +"\");" +
                    "   Object bean = null;" +
                    "   if (" + isStaticProxyMethod + " != true) {" +
                    "       bean = "+ ctxClz +".getBeanByType(clz);" +
                    "   }" +
                    "${template}" + //根据不同类型使用不同调用形式
                    "} finally { " +
                    refCtxClz + ".clean();" + //清除当前上下文引用
                    "}}";

            //静态方法 Class.invoke()，非静态方法 ((Class)bean).invoke()
            String invokeMethod = isStaticProxyMethod ? targetClz.getName() +"." + proxyMethod.getName() + methodInvokeBody
                    : "(("+ targetClz.getName() +")bean)." + proxyMethod.getName() + methodInvokeBody;

            if (ReCodeType.BEFORE.name().equalsIgnoreCase(type.toString())) {
                //前置代理，先执行代理逻辑
                String invokeBody;
                if (notHasReturn) {
                    invokeBody = invokeMethod + ";";
                } else {
                    invokeBody = "Object result = " + invokeMethod + ";" + "if (result != null) { return result; }";
                }
                methodBody = methodBody.replace("${template}", invokeBody);
                method.insertBefore(methodBody);

            } else if (ReCodeType.AFTER.name().equalsIgnoreCase(type.toString())) {
                //后置代理，先执行原始逻辑，后执行代理逻辑
                String invokeBody;
                if (notHasReturn) {
                    invokeBody = refCtxClz + ".setAfterInvokeResult($_);"
                            + invokeMethod + ";";
                } else {
                    invokeBody = refCtxClz + ".setAfterInvokeResult($_);"
                            + "Object result = " + invokeMethod + ";"
                            + "if (result != null) { return result; } else { return $_; }";
                }
                methodBody = methodBody.replace("${template}", invokeBody);
                method.insertAfter(methodBody);

            } else {
                //覆盖方法，直接return
                String invokeBody;
                if (notHasReturn) {
                    invokeBody = invokeMethod + ";";
                } else {
                    invokeBody = "return " + invokeMethod + ";";
                }
                methodBody = methodBody.replace("${template}", invokeBody);
                method.setBody(methodBody);

            }

        } catch (Exception e) {
            log.info("[proxy-agent] 类方法:[{}]代理失败", srcClass.getName(), e);
        }
    }

}
