package org.acme.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.everyItem;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class BieberTwitterConnectionCheckTest {

	@Test
	public void testEndpoint() {

		given().when().get("/health/ready")
			.then().statusCode(200)
			.body("status", is("UP"))
			.body("checks.status", everyItem(is("UP")))
			;

	}

}