package io.github.proxy.config;

import io.github.proxy.utils.AgentSpringCtxHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

@Slf4j
public class AgentApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        //上下文配置
        AgentSpringCtxHolder.initCtx(applicationContext);
        //优先加载bean
        applicationContext.addBeanFactoryPostProcessor(new ProxyBeanDefinitionRegistryPostProcessor());
    }

}


