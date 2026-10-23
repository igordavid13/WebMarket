package br.gov.caixa.webmarket.produto.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
public class MessagingConfig {
    public static final String EXCHANGE = "webmarket.commands";
    public static final String ROUTING_KEY = "estoque.baixar";
    public static final String QUEUE = "estoque.baixar.q";
    public static final String DEAD_LETTER_EXCHANGE = "webmarket.commands.dlx";
    public static final String DEAD_LETTER_QUEUE = "estoque.baixar.dlq";

    @Bean
    DirectExchange comandoExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE);
    }

    @Bean
    Queue baixaEstoqueQueue() {
        return QueueBuilder.durable(QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(ROUTING_KEY)
                .build();
    }

    @Bean
    Queue baixaEstoqueDeadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
        Binding baixaEstoqueBinding(
            @Qualifier("baixaEstoqueQueue") Queue baixaEstoqueQueue,
            @Qualifier("comandoExchange") DirectExchange comandoExchange) {
        return BindingBuilder.bind(baixaEstoqueQueue).to(comandoExchange).with(ROUTING_KEY);
    }

    @Bean
        Binding baixaEstoqueDeadLetterBinding(
            @Qualifier("baixaEstoqueDeadLetterQueue") Queue baixaEstoqueDeadLetterQueue,
            @Qualifier("deadLetterExchange") DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(baixaEstoqueDeadLetterQueue).to(deadLetterExchange).with(ROUTING_KEY);
    }

    @Bean
    MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
