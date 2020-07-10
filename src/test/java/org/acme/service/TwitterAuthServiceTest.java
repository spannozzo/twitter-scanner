package org.acme.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.acme.dto.Token;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@Tag("integration")
@TestMethodOrder(OrderAnnotation.class)
public class TwitterAuthServiceTest {

	@Inject
	TwitterAuthService twitterConfig;
		
	static Token token;
	
	@ConfigProperty(name = "user")
	String userName;
	
	@Test
	@Order(1)
	@Transactional
	public void retrieve_token_from_twitter() throws Exception {		
				
		token=twitterConfig.getToken().get();
		
		assertNotNull(token);
		
		assertTrue(!token.isEmpty());
	}
	


}