package br.gov.caixa.webmarket.gravador.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String TOPICO = "pedido-gravado";

    @Bean
    public NewTopic pedidoGravadoTopic() {
        return TopicBuilder.name(TOPICO).partitions(3).replicas(1).build();
    }
}
