package org.acme.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import org.junit.jupiter.api.Test;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class BieberHealthCheckTest {

	@Test
	public void testEndpoint() {

		given().when().get("/health/live").then().statusCode(200).body("status", is("UP"));

	}

}