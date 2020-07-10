package org.acme.service;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.acme.config.BieberConfig;
import org.acme.dto.Token;
import org.acme.entity.TokenEntity;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

@QuarkusTest
@Tag("integration")
@TestMethodOrder(OrderAnnotation.class)
public class TwitterServiceTest {

	@Inject
	TwitterService service;

	@Inject
	TokenService tokenService;

	@InjectMock
	BieberConfig twitterConfig;

	@Inject
	TwitterAuthService authService;

	static Token token;

	@ConfigProperty(name = "user")
	String userName;

	@Test
	@Order(1)
	@Transactional
	public void cannot_authenticate_when_token_is_not_valid_and_twitter_auth_is_failing() throws Exception {

		token = new Token("secret1", "token1");

		Mockito.when(twitterConfig.getTokenFromTwitter()).thenReturn(Optional.ofNullable(token));

		Boolean canAuthenticate = service.checkAuthentication(token);

		assertTrue(!canAuthenticate);

	}

	@Test
	@Order(2)
	@Transactional
	public void can_authenticate_when_token_is_not_valid() throws Exception {
		token = new Token("secret1", "token1");

		assertTrue(TokenEntity.saveTokenIntoDb(userName, token));

		Mockito.when(twitterConfig.getTokenFromTwitter()).thenReturn(authService.getToken());

		// When the system has stored a bad token check if is able to retrieve a good
		// token from twitter and it will be stored.

		Boolean canAuthenticate = service.checkAuthentication(token);

		assertTrue(canAuthenticate);

		Optional<Token> goodToken = TokenEntity.getTokenFromDb(userName);

		assertTrue(goodToken.isPresent());

		assertNotEquals(token.getToken(), goodToken.get().getToken());

		

	}

	@Test()
	@Order(3)
	@Transactional
	public void can_authenticate_when_stored_token_is_valid() throws Exception {
//		token have been stored in db so now token should be able to authenticate WITHOUT going request a new token from twitter
		
		Optional<Token> maybeIsStored= TokenEntity.getTokenFromDb(userName);
		
		assertTrue(maybeIsStored.isPresent());
		
		Assertions.assertThrows(Exception.class, () -> {
			given(twitterConfig.getTokenFromTwitter()).willThrow(new Exception());
		});

		Boolean canAuthenticate = service.checkAuthentication(maybeIsStored.get());

		assertTrue(canAuthenticate);

		TokenEntity.deleteAll();

		assertTrue(TokenEntity.listAll().isEmpty());
		
	}

}