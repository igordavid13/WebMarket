package br.gov.caixa.webmarket.produto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableScheduling
@EnableCaching
public class MsProdutoApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsProdutoApplication.class, args);
    }
}
