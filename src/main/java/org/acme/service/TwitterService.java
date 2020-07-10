package org.acme.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.acme.dto.Author;
import org.acme.dto.Message;
import org.acme.dto.Token;
import org.acme.entity.TokenEntity;
import org.acme.service.util.ServiceUtil;
import org.apache.http.HttpHost;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.opentracing.Traced;
import org.jboss.logging.Logger;
import org.json.JSONObject;

import com.google.api.client.auth.oauth.OAuthHmacSigner;
import com.google.api.client.auth.oauth.OAuthParameters;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.opentracing.Tracer;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.signature.AuthorizationHeaderSigningStrategy;

@Traced
@Singleton
public class TwitterService {

	@ConfigProperty(name = "consumerKey", defaultValue = "")
	String twitterConsumerKey;

	@ConfigProperty(name = "consumerSecret", defaultValue = "")
	String twitterSecret;

	@ConfigProperty(name = "verifyCredentialsUrl", defaultValue = "")
	String verifyCredentialsUrl;

	@ConfigProperty(name = "streamApiUrl", defaultValue = "")
	String streamApiUrl;

	@ConfigProperty(name = "user", defaultValue = "")
	String user;

	@Inject
	TokenService tokenService;
	
	
	Gson gsonObject = new GsonBuilder().setDateFormat("EEE MMM dd HH:mm:ss ZZZZZ yyyy").create();

	@Inject
	ServiceUtil serviceUtil;
	
	@Inject
	Tracer tracer;
	
	static final Logger LOG = Logger.getLogger(TwitterService.class);

	
	public Boolean checkAuthentication() {
		return checkAuthentication(tokenService.getToken());
	}
	
	public Boolean checkAuthentication(Token token) {

		Boolean authStatus = this.getAuthenticationStatus(token);

		if (!authStatus) {
			
			TokenEntity.deleteById(user);
			
			tracer.activeSpan().log(
					"authentication with existing token failed: trying retrieving a new one from Twitter and retry");
			token = tokenService.getToken();
			
			if (!token.isEmpty()) {
				authStatus = this.getAuthenticationStatus(token);

				if (!authStatus) {
					tracer.activeSpan().setTag("auth-error","cannot retrieve token from twitter");
				}
			}
			
			

		}

		return authStatus;
	}

	Boolean getAuthenticationStatus(Token token) {
		Boolean returnValue = false;

		HttpRequestFactory factory = getfactory(token);

		HttpResponse twResponse = null;
		HttpRequest twRequest = null;
		int statusCode = 0;

		try {

			twRequest = factory.buildGetRequest(new GenericUrl(verifyCredentialsUrl));

			twRequest.setConnectTimeout(5000);

			twResponse = twRequest.execute();

			statusCode = twResponse.getStatusCode();

			if (statusCode != 200) {
				returnValue = false;
			}

			LOG.debug("Twitter authentication API, request done. Status code is "+twResponse.getStatusCode());
			
			InputStream inputStream = twResponse.getContent();

			BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));

			String line = br.readLine();

			JSONObject x = new JSONObject(line);

			if (x.getString("name").equals(user) || x.getString("screen_name").equals(user)) {
				returnValue = true;
			}


		} catch (IOException e) {
			returnValue = false;
			LOG.error("Error calling Twitter authentication API");
			
			tracer.activeSpan().setTag("error","Error calling Twitter authentication API");
			tracer.activeSpan().setBaggageItem("twitter-auth-error", e.getMessage());
			

		} finally {
			try {
				if (twResponse != null) {
					twResponse.disconnect();
				}
			} catch (Exception e) {
				LOG.error("problems closing connection of Twitter authentication API.");
				
				tracer.activeSpan().setTag("error","problems closing connection of Twitter authentication API.");
				tracer.activeSpan().setBaggageItem("stream-error", e.getMessage());
			}
		}
		return returnValue;
	}

	public Map<Author, List<Message>> getMessagesFromTwitterAndGroupByUser(String text, Token token, int timeLimit,
			int twitLimit) throws URISyntaxException, OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException {

		Map<Author, List<Message>> sortedResults = null;
		
		// set client and url
		

		CloseableHttpClient client = getClosableClient();
		
		URI uri = new URI(streamApiUrl+serviceUtil.encodeValue(text));
		
        HttpRequestBase httpRequest= getPostRequest(uri);
        
        // set force closed task after time limit
        TimerTask forceCloseConnectionTask = forceCloseConnection(httpRequest,timeLimit);
        
        // set authentication with auth token and app secrets
        OAuthConsumer oauth = getOauth(token);

        oauth.sign(httpRequest);
        
        
        InputStream inputStream=null;
        CloseableHttpResponse httpResponse = null;
                
        Set<String> lines = new LinkedHashSet<String>();
        
        // start timer for force close the connections in case is too much time for any reason
        Timer t=new Timer(true);
        t.schedule(forceCloseConnectionTask, timeLimit * 1000);
        try {
        	// connect to stream api
            httpResponse = getResponse(client, uri, httpRequest);

            // retrieve data
            inputStream= httpResponse.getEntity().getContent();

            lines=getData(twitLimit, inputStream);
            
           
        }catch(Exception e){
        	LOG.error("Error calling Twitter streaming API.");
			tracer.activeSpan().setTag("error","Error calling Twitter streaming API.");
			tracer.activeSpan().setBaggageItem("stream-error", e.getMessage());

        }finally {
        	// important : close connections or twitter will block the next requests
        	try {
        		if (httpResponse!=null) {
        			httpResponse.close();
				}
        		if (client!=null) {
        			client.close();
        		}
			} catch (IOException e) {
				
				tracer.activeSpan().setTag("error","Error closing the connection on Twitter streaming API.");
				tracer.activeSpan().setBaggageItem("connection-closing-error", e.getMessage());
				
				LOG.error("Error closing the connection on Twitter streaming API."+e.getMessage());
			}
        	
		}
        try {
        	t.cancel();
		} catch (Exception e) {
			// TODO: handle exception
		}
		
        tracer.activeSpan().log("Connection ended, number of twits : " + lines.size());
        
        // from list to message objects;

		List<Message> messages = new ArrayList<>();
		
		try {
			messages = lines.parallelStream().map(x -> {
			return gsonObject.fromJson(new JSONObject(x).toString(), Message.class);
		}).collect(Collectors.toList());
		} catch (Exception e) {
			// TODO: handle exception
		}
		
			
		
		sortedResults=groupMessagesByAuthor(messages);
		
		return sortedResults;
	}


	Map<Author, List<Message>> groupMessagesByAuthor(List<Message> messages) {
		// group messages and store in a map
		Map<Author, List<Message>> result = new HashMap<Author, List<Message>>();
		
		result = messages.parallelStream().filter(m -> !m.cannotBeMapped())
			.collect(Collectors.groupingBy(m -> m.getAuthor()));
		
		Map<Author, List<Message>> sortedResults = result.entrySet().parallelStream().sorted(Map.Entry.comparingByKey())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
						(oldValue, newValue) -> oldValue, LinkedHashMap::new));
			
		return sortedResults;
	}

	Set<String> getData(int twitLimit, InputStream inputStream) throws Exception {
		
		Set<String> lines=new HashSet<String>();
		
		BufferedReader br;
		String line="";
		br=new BufferedReader(new InputStreamReader(inputStream),240);
		
		
		line=br.readLine();
         
		while (line != null && lines.size() < twitLimit) {

			lines.add(line);

			line = br.readLine();

		}
		return lines;
	}

	CloseableHttpResponse getResponse(CloseableHttpClient client, URI uri, HttpRequestBase httpRequest)
			throws IOException, ClientProtocolException {
		
		CloseableHttpResponse httpResponse;
		HttpHost target = new HttpHost(uri.getHost(), -1, uri.getScheme());
		httpResponse = client.execute(target, httpRequest);
		
		tracer.activeSpan().log("Connection status : " + httpResponse.getStatusLine());

		
		return httpResponse;
	}

	OAuthConsumer getOauth(Token token) {
		OAuthConsumer oauth=new CommonsHttpOAuthConsumer(twitterConsumerKey, twitterSecret);
        oauth.setTokenWithSecret(token.getToken(), token.getSecret());
        
        oauth.setSigningStrategy(new AuthorizationHeaderSigningStrategy());

        
		return oauth;
	}

	TimerTask forceCloseConnection(HttpRequestBase httpRequest, int timeLimit) {
		TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (httpRequest != null) {
            	
                	LOG.warn("time limit reached: if the application is still connected on the API, it will abort the connection");
                	// 	if request is pending it will be aborted causing error and forcing exiting from the loop. 
//            		if is already closed, nothing will happen
                	
                	httpRequest.abort();
                }
            }
        };
		return task;
	}

	HttpRequestBase getPostRequest(URI uri) {
		

		HttpRequestBase httpRequest = new HttpPost(uri);
		httpRequest.addHeader("content-type", "application/json");
        httpRequest.addHeader("Accept","application/json");
        
        return httpRequest;
	}

	CloseableHttpClient getClosableClient() {
		int timeout = 3;
		RequestConfig config = RequestConfig.custom()
		  .setConnectTimeout(timeout * 1000)
		  .setConnectionRequestTimeout(timeout * 10 * 1000)
		  .setSocketTimeout(timeout * 10 * 1000).build();
		
		CloseableHttpClient client = 
		  HttpClientBuilder.create().setDefaultRequestConfig(config).build();
		return client;
	}

	HttpRequestFactory getfactory(Token token) {
		OAuthHmacSigner signer = new OAuthHmacSigner();

		signer.clientSharedSecret = twitterSecret;
		signer.tokenSharedSecret = token.getSecret();

		OAuthParameters parameters = new OAuthParameters();
		parameters.consumerKey = twitterConsumerKey;
		parameters.token = token.getToken();
		parameters.signer = signer;

		HttpTransport transport = new NetHttpTransport();

		HttpRequestFactory factory = transport.createRequestFactory(parameters);
		return factory;
	}

}
