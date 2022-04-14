package com.vfs.ea.poc.sbusfunc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.RestAssured;

@QuarkusTest
public class ServiceBusRBACPOCFunctionTest {

    @InjectMock
    ServiceBusHelper sbus;

    @Test
    public void testHello() {
        RestAssured.when().get("/hello").then()
                .statusCode(200)
                .contentType("application/json")
                .body(containsString("Hi!"));
    }


    @Test
    public void testSend() {
        Mockito.when(sbus.send(any())).thenReturn("success!");
        RestAssured.when().get("/send").then()
                .statusCode(200)
                .contentType("application/json")
                .body("success", equalTo(true))
                .body("messages[0]", containsString("success!"));
    }

    @Test
    public void testSendError() {
        Mockito.when(sbus.send(any())).thenThrow(new RuntimeException("some error message"));
        RestAssured.when().get("/send").then()
                .statusCode(200)
                .contentType("application/json")
                .body("success", equalTo(false))
                .body("messages[0]", containsString("some error message"));
    }

    @Test
    public void testReceiveNoSubscription() {
        RestAssured.when().get("/receive").then()
                .statusCode(200)
                .contentType("application/json")
                .body("success", equalTo(false))
                .body("messages[0]", equalTo("Missing subscription"));
    }


    @Test
    public void testReceive() {
        Mockito.when(sbus.receive(any())).thenReturn(Arrays.asList("message 1", "message 2"));
        RestAssured.when().get("/receive?subscription=somevalue").then()
                .statusCode(200)
                .contentType("application/json")
                .body("success", equalTo(true))
                .body("messages[0]", equalTo("message 1"))
                .body("messages[1]", equalTo("message 2"));
    }

    @Test
    public void testReceiveError() {
        Mockito.when(sbus.receive(any())).thenThrow(new RuntimeException("some error message"));
        RestAssured.when().get("/receive?subscription=somevalue").then()
                .statusCode(200)
                .contentType("application/json")
                .body("success", equalTo(false))
                .body("messages[0]", containsString("some error message"));
    }
}
