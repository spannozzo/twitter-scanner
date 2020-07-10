package org.acme.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.acme.dto.Token;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.opentracing.Traced;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;

import com.google.api.client.auth.oauth.OAuthAuthorizeTemporaryTokenUrl;
import com.google.api.client.auth.oauth.OAuthCredentialsResponse;
import com.google.api.client.auth.oauth.OAuthGetAccessToken;
import com.google.api.client.auth.oauth.OAuthGetTemporaryToken;
import com.google.api.client.auth.oauth.OAuthHmacSigner;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;

import io.opentracing.Tracer;

import org.jboss.logging.Logger;

@Traced
@Dependent
public class TwitterAuthService {

	@ConfigProperty(name = "consumerKey")
	String key;

	@ConfigProperty(name = "consumerSecret")
	String sec;

	@ConfigProperty(name = "user")
	String user;

	@ConfigProperty(name = "password")
	String password;

	final HttpTransport transport = new NetHttpTransport();

	@ConfigProperty(name = "authorizeUrl")
	String authorizeUrlString;

	@ConfigProperty(name = "accessTokenUrl")
	String accessTokenUrlString;

	@ConfigProperty(name = "requestTokenUrl")
	String requestTokenUrlString;

    private static final Logger LOG = Logger.getLogger(TwitterAuthService.class);

    @Inject
	Tracer tracer;
    
	Optional<OAuthCredentialsResponse> retrieveAccessTokens(String providedPin, OAuthHmacSigner signer, String token) {
				
		OAuthGetAccessToken accessToken = new OAuthGetAccessToken(accessTokenUrlString);
		accessToken.verifier = providedPin;
		accessToken.consumerKey = sec;
		accessToken.signer = signer;
		accessToken.transport = transport;
		accessToken.temporaryToken = token;

		OAuthCredentialsResponse accessTokenResponse = null;
		try {
			accessTokenResponse = accessToken.execute();
		} catch (IOException e) {
			
			tracer.activeSpan().setTag("error-retrieveAccessTokens", "Problem retrieving oauth token from "+accessTokenUrlString);
			tracer.activeSpan().setBaggageItem("access-token-error", e.getMessage());
			
			LOG.error("Problem retrieving oauth token from "+accessTokenUrlString);
		}

		return Optional.ofNullable(accessTokenResponse);
	}

	OAuthAuthorizeTemporaryTokenUrl getAuthTempTokenUrl(OAuthHmacSigner signer ) {
				
		signer.clientSharedSecret = sec;

		OAuthCredentialsResponse requestTokenResponse = getTemporaryToken(signer);

		signer.tokenSharedSecret = requestTokenResponse.tokenSecret;

		OAuthAuthorizeTemporaryTokenUrl authorizeUrl = new OAuthAuthorizeTemporaryTokenUrl(authorizeUrlString);
		authorizeUrl.temporaryToken = requestTokenResponse.token;

		return authorizeUrl;

	}

	/*
	 * uncomment file writing lines to see on browser what is the page retrieved when pin is blank (twitter user could be blocked)
	 */
	String retrievePin(OAuthAuthorizeTemporaryTokenUrl authorizeUrl) {
		String returnValue = "";

		Path tempDir;
		try {
//			tempDir = Files.createTempDirectory(Path.of("./"), "jsoup-temp");

			String userAgent = "Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6";

//			File f1 = File.createTempFile("aut-get", ".html", tempDir.toFile());

			Response form = Jsoup.connect(authorizeUrl.build()).method(Connection.Method.GET).userAgent(userAgent)
					.execute();

//			Files.writeString(f1.toPath(), form.body(), StandardOpenOption.CREATE);

			Document formDoc = Jsoup.parse(form.body());
			String authenticityToken = formDoc.select("input[name=authenticity_token]").val();
			String redirectAfterLogin = formDoc.select("input[name=redirect_after_login]").val();

			String uiMetrics = "ui_metrics: {\"rf\":{\"ad259377aade9bf684fc569e1a3c1d3a7773af33929b2d1c90cad283fc5a5e8a\":-5,\"a50c6eff0d724f59aadca9b7c7bc2fa5c9304f5364755103aaa95aaeb7b47e86\":7,\"a47b0189299a40ffb41084d09fdb94d599e098eaddf42dee8ea372a4e3e77f2a\":-5,\"ab0e251f59d63dbd22b0c9e5ebfc8a953196352e50ec8ab656d21ec7cfc668ce\":4},\"s\":\"TT20zj8jTBhx2v7p9M04rdhr1uGyY5SflIDYFXsTmazB_DJyQmVcRPPk4cR3VD5OfB_E689SZkU7BXpDkE_Lgsyc9AzhBGTafbkZP56fvTdgzvm0Zg6iP8RRCU-Q5t7a6h9JOKcZJ0ucpeCeuv2q0knB3P_n0p7Eaund-XZgyRyCTP0bWJqcbRlAjfjPciOVrU9_agzg0XfUYNcb8nlnAsFMvvkUL9PdpznwgefEimdx0Ysdf_E8j3bgAqxEhiQzwGJPKiu0ZzoefHhPN68d5GPahWaJRhCkw6ePUCECiS8jNt6zIv9KuopIaD_G360dP4iHdT5dqArD--2RmJF6swAAAXMTpOBm\"}";

			Response pinHtml = Jsoup.connect(authorizeUrlString).data("authenticity_token", authenticityToken)
					.data("redirect_after_login", redirectAfterLogin).data("oauth_token", authorizeUrl.temporaryToken)

					.data("session[username_or_email]", user).data("session[password]", password)
					.data("ui_metrics", uiMetrics).cookies(form.cookies()).userAgent(userAgent)
					.method(Connection.Method.POST).execute();

//			File f2 = File.createTempFile("aut-post", ".html", tempDir.toFile());
//			
//			Files.writeString(f2.toPath(), pinHtml.body(), StandardOpenOption.CREATE);

			returnValue = pinHtml.parse().select("code").text();

		} catch (IOException e) {
			
			tracer.activeSpan().setTag("error-retrievePin", "Problem retrieving pin from "+authorizeUrl.build());
			tracer.activeSpan().setBaggageItem("retrievePin-error", e.getMessage());
			
			LOG.error("Problem retrieving pin from "+authorizeUrl.build()+". Try to save retrieved html to see what is the output.");
		}

		return returnValue;
	}

	OAuthCredentialsResponse getTemporaryToken(OAuthHmacSigner signer) {
		OAuthGetTemporaryToken requestToken = new OAuthGetTemporaryToken(requestTokenUrlString);
		requestToken.consumerKey = key;
		requestToken.transport = transport;
		requestToken.signer = signer;

		OAuthCredentialsResponse requestTokenResponse = null;
		try {
			requestTokenResponse = requestToken.execute();
		} catch (IOException e) {
			
			tracer.activeSpan().setTag("error-getTemporaryToken", "Problem retrieving temp token from "+requestTokenUrlString);
			tracer.activeSpan().setBaggageItem("temp-token-error", e.getMessage());
			
			LOG.error("Problem retrieving temp token from "+requestTokenUrlString);

		}

		return requestTokenResponse;
	}

	public Optional<Token> getToken() {
		
		Optional<Token> maybeFromTwitter=Optional.ofNullable(null);
				
		OAuthHmacSigner signer = new OAuthHmacSigner();
		
		OAuthAuthorizeTemporaryTokenUrl tempTokenUrl = getAuthTempTokenUrl(signer);

		String providedPin = this.retrievePin(tempTokenUrl);

		Optional<OAuthCredentialsResponse> potentialAccessResponse = retrieveAccessTokens(providedPin, signer,
				tempTokenUrl.temporaryToken);
				
		if (potentialAccessResponse.isPresent()) {
			maybeFromTwitter=Optional.of(fromCredentialResponse(potentialAccessResponse.get()));
		}

		
		return maybeFromTwitter;
	}

	Token fromCredentialResponse(OAuthCredentialsResponse resp) {
		Token fromTwitter=new Token(resp.tokenSecret,resp.token);
		return fromTwitter;
	}
	
}
