package org.acme.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.greaterThan;

import org.junit.jupiter.api.Test;
import org.apache.logging.log4j.util.Strings;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class MessageControllerTest {

	@Test
    public void testEndpoint() {
		
			given().
				when().get("/v2/messages/1/twitter")
				.then()
	            	.statusCode(200)
	            	.body("size()", is(greaterThan(0)))
	            	.body("traceId", is(not(Strings.EMPTY)))
	            ;
	             

    }

}