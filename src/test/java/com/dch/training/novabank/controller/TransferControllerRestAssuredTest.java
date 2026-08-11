package com.dch.training.novabank.controller;

import com.dch.training.novabank.AbstractIntegrationTest;
import com.dch.training.novabank.dto.TransferRequest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.port;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransferControllerRestAssuredTest extends AbstractIntegrationTest {

    private static final String SOURCE_ACCOUNT_ID = "ACC-SOURCE-RA";
    private static final String TARGET_ACCOUNT_ID = "ACC-TARGET-RA";
    private static final BigDecimal AMOUNT = new BigDecimal("75.00");
    private static final String CURRENCY = "PLN";
    private static final String API_VERSION_HEADER = "X-API-Version";

    @LocalServerPort
    private int localServerPort;

    @BeforeEach
    void setUpRestAssuredPort() {
        port = localServerPort;
    }

    @Test
    void shouldReturn202WithTransferIdWhenRequestIsValid() {
        TransferRequest request = new TransferRequest(SOURCE_ACCOUNT_ID, TARGET_ACCOUNT_ID, AMOUNT, CURRENCY);

        given()
                .header(API_VERSION_HEADER, "1")
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/transfers")
                .then()
                .statusCode(202)
                .body("transferId", notNullValue())
                .body("status", equalTo("ACCEPTED"));
    }

    @Test
    void shouldReturn400WithProblemDetailWhenSourceAccountIdIsBlank() {
        TransferRequest invalidRequest = new TransferRequest("", TARGET_ACCOUNT_ID, AMOUNT, CURRENCY);

        given()
                .header(API_VERSION_HEADER, "1")
                .contentType(ContentType.JSON)
                .body(invalidRequest)
                .when()
                .post("/api/transfers")
                .then()
                .statusCode(400)
                .body("title", equalTo("Validation failed"))
                .body("errors.sourceAccountId", notNullValue());
    }
}