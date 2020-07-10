package org.acme.microprofile.health;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.opentracing.Traced;

@Traced
@Liveness
@ApplicationScoped
public class BieberHealthCheck implements HealthCheck{

	@Override
	public HealthCheckResponse call() {
		 return HealthCheckResponse.up("Application is running");
	}

}
