package com.loopers.infrastructure.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.loopers.domain.payment.PgOrderTransactions;
import com.loopers.domain.payment.PgPaymentClient;
import com.loopers.domain.payment.PgPaymentRequest;
import com.loopers.domain.payment.PgPaymentResult;
import com.loopers.domain.payment.PgTransactionDetail;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
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
            String pgMessage = response.path("meta").path("message").asText("결제 요청에 실패했습니다.");
            return new PgPaymentResult(false, null, null, pgMessage);
        } catch (WebClientResponseException e) {
            String pgMessage = parsePgErrorMessage(e.getResponseBodyAsString());
            log.warn("PG 결제 요청 실패: status={}, message={}", e.getStatusCode(), pgMessage);
            boolean isServerError = e.getStatusCode().is5xxServerError();
            String errorMessage = isServerError
                    ? "결제 서비스가 일시적으로 불안정합니다. 잠시 후 다시 시도해주세요."
                    : pgMessage;
            return new PgPaymentResult(false, null,
                    isServerError ? "PG_UNAVAILABLE" : "PG_BAD_REQUEST", errorMessage);
        } catch (Exception e) {
            log.warn("PG 결제 요청 실패 (연결 오류): {}", e.getMessage());
            return new PgPaymentResult(false, null, "PG_UNAVAILABLE",
                    "결제 서비스에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.");
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

    private String parsePgErrorMessage(String responseBody) {
        try {
            JsonNode node = new ObjectMapper().readTree(responseBody);
            String message = node.path("meta").path("message").asText(null);
            if (message != null && !message.isBlank()) {
                return message;
            }
        } catch (Exception ignored) {
        }
        return "결제 서비스에 일시적인 문제가 발생했습니다.";
    }
}
