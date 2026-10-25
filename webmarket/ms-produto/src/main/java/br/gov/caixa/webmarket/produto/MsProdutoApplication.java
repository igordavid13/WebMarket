package br.gov.caixa.webmarket.produto;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MsProdutoApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsProdutoApplication.class, args);
    }

/*
    @Bean
    CommandLineRunner debugRabbit(
            org.springframework.amqp.rabbit.connection.CachingConnectionFactory cf) {
        return args -> {
            System.out.println("Rabbit host  : " + cf.getHost());
            System.out.println("Rabbit port  : " + cf.getPort());
            System.out.println("Rabbit vhost : " + cf.getVirtualHost());
        };
    }
*/
}
