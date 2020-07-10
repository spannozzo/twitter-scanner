package org.acme.config;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.acme.controller.MessageController;
import org.acme.dto.Token;
import org.acme.microprofile.health.BieberTwitterConnectionCheck;
import org.acme.service.MessageService;
import org.acme.service.TokenService;
import org.acme.service.TwitterAuthService;
import org.acme.service.TwitterService;
import org.eclipse.microprofile.opentracing.Traced;
import org.jboss.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.opentracing.Tracer;
import io.quarkus.runtime.Startup;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.configuration.ProfileManager;

@Traced

@Startup
@ApplicationScoped
public class BieberConfig {

	@Inject
	TwitterAuthService authService;

	@Inject
	TwitterService twitterService;

	@Inject
	TokenService tokenService;

	Boolean ready = true;

	@Produces
	public Boolean getReady() {
		return ready;
	}


	@Inject
	Tracer tracer;

	static final Logger LOG = Logger.getLogger(BieberConfig.class);
	
	@Transactional
	void onStart(@Observes StartupEvent ev) {
		LOG.info("The application is starting with profile " + ProfileManager.getActiveProfile());

	
		ready = twitterService.checkAuthentication();

		tracer.activeSpan().setBaggageItem("ready", ready.toString());

		LOG.info("Application is ready to retrieve data: " + ready);

	}

	public Optional<Token> getTokenFromTwitter() {

		return authService.getToken();

	}

}
