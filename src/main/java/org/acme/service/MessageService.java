package org.acme.service;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.acme.config.BieberConfig;
import org.acme.dto.Author;
import org.acme.dto.Message;
import org.acme.dto.Token;
import org.eclipse.microprofile.opentracing.Traced;
import org.jboss.logging.Logger;

import io.opentracing.Tracer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

@Traced
@Dependent
public class MessageService {
	
	@Inject
	TwitterService twitterService;
	
	@Inject
	TokenService tokenService;
	
	static final Logger LOG = Logger.getLogger(MessageService.class);

	@Inject
	Tracer tracer;
	
	
	@Inject
	Boolean ready;
	
	public Map<Author, List<Message>> getmessages(int maxNumbOfTweets, String text) throws URISyntaxException,
			OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException {
		Map<Author, List<Message>> map = new HashMap<Author, List<Message>>();
		
		if (ready) {
			Token token=tokenService.getToken();
			map = twitterService.getMessagesFromTwitterAndGroupByUser(text, token, 30, maxNumbOfTweets);
		}
		

		return map;
	}
}
