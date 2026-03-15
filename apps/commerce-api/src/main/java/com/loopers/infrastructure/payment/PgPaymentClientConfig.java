package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.PgPaymentClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class PgPaymentClientConfig {

    @Bean
    public PgPaymentClient pgPaymentClient(
            @Value("${pg-simulator.base-url:http://localhost:8082}") String baseUrl) {
        return new PgPaymentRestClient(
                WebClient.builder().baseUrl(baseUrl).build());
    }
}
