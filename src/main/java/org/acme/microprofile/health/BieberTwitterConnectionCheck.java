package org.acme.microprofile.health;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.acme.dto.Token;
import org.acme.service.TokenService;
import org.acme.service.TwitterService;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;
import org.eclipse.microprofile.opentracing.Traced;

import io.opentracing.Tracer;

@Traced
@Readiness
@ApplicationScoped
public class BieberTwitterConnectionCheck implements HealthCheck {

	@Inject
	TwitterService twitterService;
	
	@Inject
	TokenService tokenService;

	@ConfigProperty(name = "user")
	String user;
	

	@Inject
	Tracer tracer;
	
	@Transactional
	@Override
	public HealthCheckResponse call() {

		HealthCheckResponseBuilder responseBuilder = HealthCheckResponse.named("Twitter connection health check");
	
		
		Token token = tokenService.getToken();
		

		if (token.isEmpty()) {
			return responseBuilder.down().withData("user", user).build();
		}
		
		Boolean ready = twitterService.checkAuthentication();

		tracer.activeSpan().setBaggageItem("ready",ready.toString());
		
		if (!ready) {
			return responseBuilder.down().withData("user", user).build();
		}
		
		
		return responseBuilder.up().withData("user", user).build();
		
	}

}
