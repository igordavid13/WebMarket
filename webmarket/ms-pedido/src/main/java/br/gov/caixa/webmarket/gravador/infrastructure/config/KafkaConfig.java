package br.gov.caixa.webmarket.gravador.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String TOPICO_ESTOQUE_BAIXADO = "estoque-baixado";

    @Bean
    public NewTopic estoqueBaixadoTopic() {
        return TopicBuilder.name(TOPICO_ESTOQUE_BAIXADO).partitions(3).replicas(1).build();
    }
}
