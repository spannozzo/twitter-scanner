package org.acme.entity;

import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.LockModeType;
import javax.persistence.Table;
import javax.transaction.Transactional;

import org.acme.dto.Token;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

@Entity()
@Table(name = "token")
public class TokenEntity extends PanacheEntityBase {

	public TokenEntity() {
		// TODO Auto-generated constructor stub
	}

	public TokenEntity(String userName, String secret, String token) {
		super();
		this.userName = userName;
		this.secret = secret;
		this.token = token;
	}

	@Id
	String userName;

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	@Column()
	String secret;

	@Column(unique = true)
	String token;

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public Token toDTO() {
		Token t=new Token();
		
		t.setSecret(this.secret);
		t.setToken(this.token);
		
		return t;
	}

	@Transactional
	public static Optional<Token> getTokenFromDb(String userName) {

		Optional<Token> maybeToken = Optional.ofNullable(null);

		TokenEntity tokenX = TokenEntity.findById(userName, LockModeType.NONE);

		if (tokenX != null) {
			maybeToken = Optional.of(tokenX.toDTO());
		}

		return maybeToken;
	}
	@Transactional
	public static Boolean saveTokenIntoDb(String userName,Token t) {

		Optional<TokenEntity> fromDb = TokenEntity.findByIdOptional(userName);
		
		if (fromDb.isPresent()) {
			TokenEntity tokenFromDb=fromDb.get();
			
			tokenFromDb.persist();
			
			tokenFromDb.setToken(t.getToken());
			tokenFromDb.setSecret(t.getSecret());
			
		}else {
			TokenEntity newToken=new TokenEntity(userName, t.getSecret(), t.getToken());
			
			TokenEntity.persist(newToken);
			
		}
		
		return true; 
	}

}
