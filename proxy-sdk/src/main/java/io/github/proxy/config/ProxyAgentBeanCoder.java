package io.github.proxy.config;

import io.github.proxy.utils.AgentAttachUtils;
import io.github.proxy.utils.AgentSpringCtxHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * 服务启动连接agent
 */
@Slf4j
@Service
public class ProxyAgentBeanCoder {

    @PostConstruct
    public void init() {
        //读取所有ProxyRecodeCfg实现bean的代理配置
        AgentSpringCtxHolder.initProxyBean();
        //代理agent连接jvm
        AgentAttachUtils.attachAgent();
    }

}
