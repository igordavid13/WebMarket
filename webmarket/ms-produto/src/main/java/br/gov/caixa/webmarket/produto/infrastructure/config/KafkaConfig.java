package br.gov.caixa.webmarket.produto.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {
    @Bean
    NewTopic estoqueBaixadoTopic() {
        return TopicBuilder.name("estoque-baixado").partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic baixaEstoqueFalhouTopic() {
        return TopicBuilder.name("baixa-estoque-falhou").partitions(3).replicas(1).build();
    }
}
