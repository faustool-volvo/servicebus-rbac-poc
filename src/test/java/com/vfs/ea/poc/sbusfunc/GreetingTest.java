package com.vfs.ea.poc.sbusfunc;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
public class GreetingTest
{

    @Test
    public void testFunqy() {
        RestAssured.when().get("/funqyHello").then()
                .statusCode(200)
                .contentType("application/json")
                .body(equalTo("\"hello funqy\""));
    }
}
