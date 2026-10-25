package br.gov.caixa.webmarket.gravador;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;

@SpringBootApplication
@EnableScheduling
public class GravadorApplication {
    public static void main(String[] args) {
        SpringApplication.run(GravadorApplication.class, args);
    }

/*
    @Bean
    CommandLineRunner checkWorkDir() {
        return args -> {
            File dir = new File("target/embedded-qpid");
            System.out.println("Files:");
            for (File f : dir.listFiles()) {
                System.out.println(" - " + f.getAbsolutePath());
            }
        };
    }
*/
}