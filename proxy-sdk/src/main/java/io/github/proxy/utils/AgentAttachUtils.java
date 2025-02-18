package io.github.proxy.utils;

import com.sun.tools.attach.VirtualMachine;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;

@Slf4j
public class AgentAttachUtils {

    private static final String AGENT_JAR = "proxy-core-0.0.1.jar";
    private static final String AGENT_CLASSPATH = "agent/" + AGENT_JAR;
    private static final String AGENT_SAVE_PATH = "/data/agent";

    public static void attachAgent() {
        try {
            long start = System.currentTimeMillis();
            String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];

            String agentPath = saveAgentPath();

            loadAgent(pid, agentPath);
            log.info("[proxy-agent] load proxy-agent success. cost:{}ms", System.currentTimeMillis() - start);

        } catch (Exception e) {
            log.error("[proxy-agent] load proxy-agent failed", e);

        }
    }

    @SneakyThrows
    private static String saveAgentPath() {
        String dir = System.getProperty("proxyAgent.save.path", AGENT_SAVE_PATH);
        String savePath = dir + "/" + AGENT_JAR;

        File folderPath = new File(dir);
        Files.createDirectories(folderPath.toPath());

        try (InputStream inputStream = AgentAttachUtils.class.getClassLoader().getResourceAsStream(AGENT_CLASSPATH);
             OutputStream outputStream = Files.newOutputStream(new File(savePath).toPath())) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
        }

        log.info("[proxy-agent] extract agent jar file to [{}]", savePath);
        return savePath;
    }

    @SneakyThrows
    private static void loadAgent(String pid, String agentPath) {
        VirtualMachine vm = VirtualMachine.attach(pid);
        vm.loadAgent(agentPath);
        vm.detach();
    }

}
