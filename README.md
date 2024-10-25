### proxy-agent
springboot启动时自动加载java agent，使用javassist实现对字节码的重写。
# 快速开始
第一步：添加依赖
```xml
<dependency>
  <groupId>io.github.yingxinguo95</groupId>
  <artifactId>proxy-sdk</artifactId>
  <version>0.0.1</version>
</dependency>
```
第二步：定义你自己的字节码重写逻辑，例如以下代码，对okhttp请求前做处理，打印日志信息
```java
import io.github.proxy.annotation.ProxyRecodeCfg;
import io.github.proxy.annotation.ReCodeType;
import io.github.proxy.service.ProxyReCode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TestReCoder implements ProxyReCode {
    @SneakyThrows
    @ProxyRecodeCfg(proxyClassName="okhttp3.RealCall", method="getResponseWithInterceptorChain", type = ReCodeType.BEFORE)
    public Response getResponseWithInterceptorChainProxy() {
        log.info(">>>>>>>>>>>>>>> proxy okhttp");
        return null;
    }
}
```
第三步：启动服务，执行代码
服务启动控制台打印：
```bash
[proxy-agent] rewrite class:[okhttp3.RealCall]
[proxy-agent] redefine loaded class complete, cost:xxxms
[proxy-agent] load proxy-agent success. cost:xxxms
```
说明字节码重写完成。使用okhttp发起http请求时会执行重写的逻辑，在控制台打印"proxy okhttp"。
# 功能介绍
通过类实现ProxyReCode接口，并注册为springBean，这个字节码重写逻辑即可在springBoot启动后自动生效。
@ProxyRecodeCfg注解标记的方法表示为字节码重写后的逻辑。
例如有一个类TestClass，有个方法methodA，这个类我们需要对它做字节码重写。
```java
public class TestClass {
	public String methodA() {
		return "A";
	}
}
```
直接覆盖式重写，调用TestClass.methodA()后执行重写后的逻辑，返回字符串"OVERRIDER"。
- proxyClass：代理目标类class
- proxyClassName: 代理目标类class名称
- method：代理类的需要重写的方法
- type：重写类型
```java
@Component
public class TestReCoder implements ProxyReCode {
    @SneakyThrows
    @ProxyRecodeCfg(proxyClass=TestClass.class, method="methodA", type = ReCodeType.OVERRIDER)
    public String methodA() {
        return "OVERRIDER";
    }
}
```
添加前置逻辑，重写逻辑在methodA方法前执行
```java
@Component
public class TestReCoder implements ProxyReCode {
    @SneakyThrows
    @ProxyRecodeCfg(proxyClass=TestClass.class, method="methodA", type = ReCodeType.BEFORE)
    public String methodA() {
	    //获取代理类执行过程中的this对象
	    Object ref = ProxyReferenceCtx.getRef();
        if (ref.getClass().getName().contains("Test")) {
		    //返回值不为null，直接返回值，原始方法逻辑不会执行
            return "BEFORE";
        }
		//前置返回null，则继续执行原始方法逻辑
        return null;
    }
}
```
添加后置逻辑，重写逻辑在methodA方法后执行
```java
@Component
public class TestReCoder implements ProxyReCode {
    @SneakyThrows
    @ProxyRecodeCfg(proxyClass=TestClass.class, method="methodA", type = ReCodeType.AFTER)
    public String methodA() {
	    //获取原始逻辑的返回值
	    String result = ProxyReferenceCtx.getAfterInvokeResult();
        if (result.equals("A")) {
		    //返回值不为null，方法执行获取当前返回值
            return "test after";
        }
		//返回null，方法执行得到原始逻辑值
        return null;
    }
}
```
## 需注意
### 代理方法的方法签名需要和被代理方法一致，这样才能代理成功
例如代理方法为org.apache.commons.lang.StringUtils类的isNotEmpty方法
//原方法 org.apache.commons.lang.StringUtils
public static boolean isNotEmpty(String str);

        //代理方法定义，方法签名与被代理方法保持一致
        @ProxyRecodeCfg(proxyClass=StringUtils.class, method="isNotEmpty")
        public boolean isNotEmpty(String str);
    
        //代理方法定义，方法名称可以不同，但是参数、返回值需要保持一致
        @ProxyRecodeCfg(proxyClass=StringUtils.class, method="isNotEmpty")
        public boolean proxyMethod(String str);
    
        //代理方法定义，也可定义为static方法，但需要是public
        @ProxyRecodeCfg(proxyClass=StringUtils.class, method="isNotEmpty")
        public static boolean isNotEmptyProxy(String str);
## Springboot打包配置
代理本地IDE执行可以直接生效，但是使用springboot打包为fatJar，spring-boot-maven-plugin插件需要添加配置includeSystemScope配置
否则启动加载agent会出现异常：java.lang.NoClassDefFoundError: com/sun/tools/attach/VirtualMachine
```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <version>2.1.3.RELEASE</version>
    <configuration>
        <!--设置为true，打包时添加system的jar-->
        <includeSystemScope>true</includeSystemScope>
    </configuration>
</plugin>
```