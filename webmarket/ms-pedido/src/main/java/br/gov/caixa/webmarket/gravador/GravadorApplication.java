package br.gov.caixa.webmarket.gravador;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GravadorApplication {
    public static void main(String[] args) {
        SpringApplication.run(GravadorApplication.class, args);
    }
}