package io.github.proxy.utils;

public class ProxyReferenceCtx {

    //当前执行方法的代理对象
    private static final ThreadLocal<Object> ref = ThreadLocal.withInitial(() -> null);

    //覆盖方式为after时，需要将代理方法执行结果保存下来，供after方法使用
    private static final ThreadLocal<Object> afterInvokeResult = ThreadLocal.withInitial(() -> null);

    public static void init(Object thisRef) {
        ref.set(thisRef);
    }

    public static <T> T getRef() {
        return (T) ref.get();
    }

    public static void setAfterInvokeResult(Object result) {
        afterInvokeResult.set(result);
    }

    public static <T> T getAfterInvokeResult() {
       return (T) afterInvokeResult.get();
    }

    public static void clean() {
        ref.remove();
        afterInvokeResult.remove();
    }

}
