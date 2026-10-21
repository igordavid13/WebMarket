package br.gov.caixa.webmarket.gravador.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessagingConfig {

    public static final String EXCHANGE = "gravacao.ex";
    public static final String ROUTING  = "gravacao";
    public static final String FILA     = "gravacao.q";
    public static final String DLX      = "gravacao.dlx";
    public static final String DLQ      = "gravacao.dlq";

    @Bean
    public DirectExchange gravacaoExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public Queue gravacaoQueue() {
        return QueueBuilder.durable(FILA)
                .withArgument("x-dead-letter-exchange", DLX)
                .build();
    }

    @Bean
    public Binding gravacaoBinding() {
        return BindingBuilder.bind(gravacaoQueue()).to(gravacaoExchange()).with(ROUTING);
    }

    @Bean
    public FanoutExchange deadLetterExchange() {
        return new FanoutExchange(DLX);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange());
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper mapper) {
        return new Jackson2JsonMessageConverter(mapper);
    }
}
