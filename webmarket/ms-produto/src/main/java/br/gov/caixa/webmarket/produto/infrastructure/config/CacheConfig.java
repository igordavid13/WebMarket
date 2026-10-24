package br.gov.caixa.webmarket.produto.infrastructure.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class CacheConfig {

    @Bean
    Cache<String, String> produtoCache() {
        return Caffeine.newBuilder()
                .maximumSize(1_000)
                .expireAfterAccess(Duration.ofMinutes(30))
                .build();
    }
}
