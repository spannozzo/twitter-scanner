package org.acme.service;

import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Singleton;
import org.acme.config.BieberConfig;
import org.acme.dto.Token;
import org.acme.entity.TokenEntity;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.opentracing.Traced;
import org.jboss.logging.Logger;

import io.opentracing.Tracer;

@Traced
@Singleton
public class TokenService {

	static final Logger LOG = Logger.getLogger(TokenService.class);
	
	@ConfigProperty(name = "tokenfile")
	String tokenFile;

	@Inject
	BieberConfig twitterConfig;
	
	@Inject
	Tracer tracer;
	
	public String getTokenFile() {
		return tokenFile;
	}
	
	@ConfigProperty(name = "user")
	String userName;

	Properties getPropertiesFromFile() throws Exception {
		Properties tokenProperties = new Properties();

		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(tokenFile);

		tokenProperties.load(inputStream);

		return tokenProperties;
	}

	
	
	public Token getToken() {
		
		Token tokenDb=TokenEntity.getTokenFromDb(userName).orElseGet(()->{
			
			tracer.activeSpan().log("acquiring token from Twitter");
			
			Optional<Token> fromTwitter=twitterConfig.getTokenFromTwitter();
			
			if (fromTwitter.isPresent()) {
				
				if (!TokenEntity.saveTokenIntoDb(userName, fromTwitter.get())) {
					LOG.error("Error saving token");
					
					tracer.activeSpan().setTag("error", "Cannot save token file");
					tracer.activeSpan().setBaggageItem("save-token-for", userName);
				}
				return fromTwitter.get();
			}
			else {
				return new Token();
			}
			
		});
		
		

		return tokenDb;
	}



}
