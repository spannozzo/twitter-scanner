package org.acme.service;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.acme.config.BieberConfig;
import org.acme.dto.Token;
import org.acme.entity.TokenEntity;
import org.acme.service.TokenService;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;


@QuarkusTest
@TestMethodOrder(OrderAnnotation.class)
public class TokenServiceTest {

	@Inject
	TokenService service;

	@InjectMock
	BieberConfig twitterConfig;

	private static Token token;
	
	@ConfigProperty(name = "user")
	String userName;
	
	@Test
	@Order(1)
	@Transactional
	public void check_get_token_if_is_stored() throws Exception {
		Mockito.when(twitterConfig.getTokenFromTwitter()).thenReturn(Optional.ofNullable(null));
		
		assertTrue(TokenEntity.saveTokenIntoDb(userName, new Token("secret1", "token1")));
		
		token=service.getToken();
		
		assertNotNull(token);
		assertTrue(!token.isEmpty());
		

	}
	@Test
	@Order(2)
	@Transactional
	public void check_get_token_when_is_not_stored() throws Exception {
		
		// init - add a token from DB
		
		TokenEntity.deleteAll();
		
		List<TokenEntity> tokens=TokenEntity.listAll();
		
		assertTrue(tokens.isEmpty());
		
		
		Mockito.when(twitterConfig.getTokenFromTwitter()).thenReturn(Optional.ofNullable(token));
		
		// check if can retrieve the token from twitter service when is not possible to retrieve from token properties file (that have been deleted)
		
		Token token=service.getToken();
		
		assertNotNull(token);
		assertTrue(!token.isEmpty());
		
		// check if after retrieving the token from Twitter, it will be saved
		
		Optional<Token> savedFromTwitterCheck = TokenEntity.getTokenFromDb(userName);
		
		assertTrue(savedFromTwitterCheck.isPresent());
		assertEquals(token.getToken(), savedFromTwitterCheck.get().getToken());
		assertEquals(token.getSecret(), savedFromTwitterCheck.get().getSecret());
		
	}

}