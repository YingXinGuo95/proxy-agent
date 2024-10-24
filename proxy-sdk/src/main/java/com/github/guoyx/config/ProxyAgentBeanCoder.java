package com.github.guoyx.config;

import com.github.guoyx.utils.AgentAttachUtils;
import com.github.guoyx.utils.AgentSpringCtxHolder;
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
