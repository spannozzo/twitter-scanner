package org.acme.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.acme.dto.Token;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;

import io.quarkus.test.junit.QuarkusTest;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import javax.persistence.LockModeType;
import javax.transaction.Transactional;
import javax.validation.constraints.AssertTrue;

@QuarkusTest
@TestMethodOrder(OrderAnnotation.class)

public class TokenEntityTest {

	
	@Test
	@Order(1)
	@Transactional
	public void check_getTokenFromDb() {
		
		TokenEntity.deleteAll();
		
		TokenEntity tokenEntity1=new TokenEntity("user_x","secret","token_x");
		TokenEntity tokenEntity2=new TokenEntity("user_y","secret","token_y");
		
		tokenEntity1.persist();
		tokenEntity2.persist();
		
		List<TokenEntity> tokens=TokenEntity.listAll();

		assertTrue(tokens.size()==2);
		
		Optional<Token> maybe=TokenEntity.getTokenFromDb("user_x");
		
		assertTrue(maybe.isPresent());
		
		assertEquals(tokenEntity1.getToken(), maybe.get().getToken());
		
		
		TokenEntity.deleteAll();
		
		tokens=TokenEntity.listAll();
		
		assertTrue(tokens.isEmpty());
	}
	@Test
	@Order(2)
	@Transactional
	public void check_setTokenIntoDb() {
		
		TokenEntity.deleteAll();
		
		Token token=new Token("secret","token_x");
		
		String userName1="user_x";
				
		assertTrue(TokenEntity.saveTokenIntoDb(userName1, token));
		
		List<TokenEntity> tokens=TokenEntity.listAll();

		assertTrue(!tokens.isEmpty());
		
		Optional<Token> maybe=TokenEntity.getTokenFromDb("user_x");
		
		assertTrue(maybe.isPresent());
		
		assertEquals(token.getToken(), maybe.get().getToken());
		
		TokenEntity.deleteAll();
		tokens=TokenEntity.listAll();
		
		assertTrue(tokens.size()==0);
	}

}