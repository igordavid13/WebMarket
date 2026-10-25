/*package br.gov.caixa.webmarket.gravador.infrastructure.config;

import org.apache.qpid.server.SystemLauncher;
import org.apache.qpid.server.SystemLauncherListener;
import org.apache.qpid.server.model.JsonSystemConfigImpl;
import org.apache.qpid.server.model.SystemConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Configuration
@Profile("local")
public class EmbeddedBrokerConfig {

    private SystemLauncher broker;

    @PostConstruct
    public void startBroker() throws Exception {
        String workDirectory = new File("target/embedded-qpid").getAbsolutePath();
        System.setProperty("qpid.work_dir", workDirectory);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put(SystemConfig.TYPE, JsonSystemConfigImpl.SYSTEM_CONFIG_TYPE);
        attributes.put(SystemConfig.INITIAL_CONFIGURATION_LOCATION,
                "classpath:embedded-qpid-config.json");
        attributes.put(SystemConfig.CONTEXT, Map.of(
                "qpid.amqp_port", "5672",
                "qpid.http_port", "15672",
                "qpid.work_dir", workDirectory));

        broker = new SystemLauncher(new SystemLauncherListener.DefaultSystemLauncherListener());
        broker.startup(attributes);
    }

    @PreDestroy
    public void stopBroker() {
        if (broker != null) {
            broker.shutdown();
        }
    }
}*/
