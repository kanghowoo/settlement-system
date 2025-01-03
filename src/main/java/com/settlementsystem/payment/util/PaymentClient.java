package com.settlementsystem.payment.util;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.settlementsystem.payment.service.PortOneRequestUrl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentClient {
    private final RestClient restClient;

    @Value("${payment.imp-key}")
    private String impKey;

    @Value("${payment.imp-secret}")
    private String impSecret;

    private static final String BASE_URL = "https://api.iamport.kr";

    /**
     * Get Access Token
     * @return Access token as String
     */
    public Map getAccessToken() {
        String url = BASE_URL + PortOneRequestUrl.ACCESS_TOKEN_URL.getUrl();
        try {
            // Construct the request body
            String requestBody = String.format("{\"imp_key\": \"%s\", \"imp_secret\": \"%s\"}", impKey, impSecret);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Send POST request
            return restClient
                    .post()
                    .uri(url)
                    .headers(h -> h.addAll(headers))
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

        } catch (RestClientException e) {
            throw new RuntimeException("Failed to get access token", e);
        }
    }

    /**
     * Cancel Payment
     * @param impUid imp_uid of the payment to cancel
     * @return Response from PortOne API
     */
    @Retryable(
            value = RestClientException.class,  // 재시도할 예외 지정
            maxAttempts = 2,                    // 최초 호출 1회 + 재시도 1회
            backoff = @Backoff(delay = 1000),    // 1초 대기 후 재시도
            recover = "handlePaymentCancellationFailure"
    )
    public String cancelPayment(String impUid) {

        String accessToken = ((LinkedHashMap)getAccessToken().get("response")).get("access_token").toString();

        String url = BASE_URL + PortOneRequestUrl.CANCEL_PAYMENT_URL.getUrl();

        try {
            String requestBody = String.format("{\"imp_uid\": \"%s\"}",impUid);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            return restClient
                    .post()
                    .uri(url)
                    .headers(h -> h.addAll(headers))
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

        } catch (RestClientException e) {
            throw new RuntimeException("Failed to cancel payment", e);
        }

    }

    @Recover
    public String handlePaymentCancellationFailure(RestClientException e, String impUid) {
        log.error("RestClientException 예외로 인해 실패: " + e.getClass().getName() + " impUid : "+impUid);
        // 실패에 대한 처리 로직 구현 필요.
        // 사전작업에서 처리한 디비 수정값 등의 데이터들을 원복 처리
        // 담당자(개발자,운영자,기획자)에서 해당 이슈를 전달 - email , team 메신저 webhook 처리
        return HttpStatus.INTERNAL_SERVER_ERROR.toString(); // 실패 후 대체 반환 값
    }


    /**
     * Create Payment (Example: if you want to add payment creation)
     * @param paymentRequest Payment request data
     * @param token Access token
     * @return Response from PortOne API
     */
    public String createPayment(String paymentRequest, String token) {
        String url = BASE_URL + PortOneRequestUrl.CREATE_PAYMENT_URL.getUrl();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);

            // Send POST request
            return restClient
                    .post()
                    .uri(url)
                    .headers(h -> h.addAll(headers))
                    .body(paymentRequest)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            throw new RuntimeException("Failed to create payment", e);
        }
    }



}
