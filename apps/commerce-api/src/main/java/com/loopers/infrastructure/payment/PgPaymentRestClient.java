package com.loopers.infrastructure.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.loopers.domain.payment.PgOrderTransactions;
import com.loopers.domain.payment.PgPaymentClient;
import com.loopers.domain.payment.PgPaymentRequest;
import com.loopers.domain.payment.PgPaymentResult;
import com.loopers.domain.payment.PgTransactionDetail;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

public class PgPaymentRestClient implements PgPaymentClient {

    private final WebClient webClient;

    public PgPaymentRestClient(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public PgPaymentResult requestPayment(PgPaymentRequest request) {
        try {
            JsonNode response = webClient.post()
                    .uri("/api/v1/payments")
                    .header("X-USER-ID", request.userId())
                    .bodyValue(Map.of(
                            "orderId", request.orderId(),
                            "cardType", request.cardType(),
                            "cardNo", request.cardNo(),
                            "amount", request.amount(),
                            "callbackUrl", request.callbackUrl()))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            String metaResult = response.path("meta").path("result").asText();
            if ("SUCCESS".equals(metaResult)) {
                JsonNode data = response.path("data");
                return new PgPaymentResult(
                        true,
                        data.path("transactionKey").asText(),
                        data.path("status").asText());
            }
            return new PgPaymentResult(false, null, null);
        } catch (WebClientResponseException e) {
            return new PgPaymentResult(false, null, null);
        }
    }

    @Override
    public PgTransactionDetail getPaymentStatus(String transactionKey, String userId) {
        JsonNode response = webClient.get()
                .uri("/api/v1/payments/{transactionKey}", transactionKey)
                .header("X-USER-ID", userId)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        JsonNode data = response.path("data");
        return new PgTransactionDetail(
                data.path("transactionKey").asText(),
                data.path("orderId").asText(),
                data.path("cardType").asText(),
                data.path("cardNo").asText(),
                data.path("amount").asLong(),
                data.path("status").asText(),
                data.path("reason").asText(null));
    }

    @Override
    public PgOrderTransactions getPaymentsByOrder(String orderId, String userId) {
        JsonNode response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/payments")
                        .queryParam("orderId", orderId)
                        .build())
                .header("X-USER-ID", userId)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        JsonNode data = response.path("data");
        List<PgOrderTransactions.PgTransactionSummary> transactions = new ArrayList<>();
        for (JsonNode tx : data.path("transactions")) {
            transactions.add(new PgOrderTransactions.PgTransactionSummary(
                    tx.path("transactionKey").asText(),
                    tx.path("status").asText(),
                    tx.path("reason").asText(null)));
        }
        return new PgOrderTransactions(
                data.path("orderId").asText(), transactions);
    }
}
