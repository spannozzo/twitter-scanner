package org.acme.controller;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.acme.dto.Author;
import org.acme.dto.Message;
import org.acme.dto.ResponseDTO;
import org.acme.service.MessageService;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.eclipse.microprofile.opentracing.Traced;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

import io.opentracing.Tracer;

@ApplicationScoped
@Traced

@Path("v2/messages/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MessageController {

	@Inject
	Tracer tracer;
	
	@Inject
	MessageService messageService;

	long totalMessageRetrieved;

	private Map<Author, List<Message>> results;
	
	static final Logger LOG = Logger.getLogger(MessageController.class);
	
	AtomicBoolean checkMetrics1=new AtomicBoolean(false);
	AtomicBoolean checkMetrics2=new AtomicBoolean(false);
	
	long totalMSecondsPassed;

	private long mSecondsPassed;
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
	@Path("/{count}/{text}")
	@Counted(name = "performedSearchs", description = "How many search have been done.")
    @Timed(name = "timer", description = "how long it takes to search twits" )
    public Response getMessagesFromService(@PathParam int count,@PathParam String text) throws Exception{
		
		
		results=new HashMap<Author, List<Message>>();
		
		tracer.activeSpan().setTag("key-search", text);
		String traceid=tracer.activeSpan().context().toString().split(":")[0];
		
		if (text==null) {
			LOG.info("Strange error. text is null !?");
			return Response.ok(new ResponseDTO(results, traceid)).build();
		}
		
		
		long before=Instant.now().toEpochMilli();
		results=messageService.getmessages(count, text);
		long after=Instant.now().toEpochMilli();
		
		LOG.info(text+": found:"+results.size());
		
		tracer.activeSpan().setBaggageItem(text, results.toString());
		
		
		ResponseDTO responseDTO=new ResponseDTO(results,traceid);
		
		
//		begin metrics
		checkMetrics1.set(true);
		
		mSecondsPassed=after-before;
		this.getTotalMSecondPassed();
		checkMetrics1.set(false);
		
		checkMetrics2.set(true);
		this.getTotalMessageRetrieved();
		checkMetrics2.set(false);
//		end metrics

		
		
		
		
		return Response.ok(responseDTO).build();
	}

	@Gauge(name = "totalMessagesRetrieved", unit = MetricUnits.NONE, description = "number of total returned item")
	public long getTotalMessageRetrieved() {
				
		int retrievedMessages=0;
		
		if (checkMetrics2.get()) {
			try {
				retrievedMessages=results.values().parallelStream().flatMap(List::stream).collect(Collectors.toList()).size();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		totalMessageRetrieved+=retrievedMessages;
		
		
		
		return totalMessageRetrieved; 
		
    }
	@Gauge(name = "totSeconds", unit = MetricUnits.MILLISECONDS, description = "number of second passed")
	public long  getTotalMSecondPassed() {
		
		if (checkMetrics1.get()) {
			totalMSecondsPassed+=mSecondsPassed;
		}
		
		return totalMSecondsPassed;
	}
	@Gauge(name = "messagePerSeconds", unit = MetricUnits.PER_SECOND, description = "number of second passed")
	public double  getMessagePerSeconds() {
		if(totalMSecondsPassed!=0) {
			
			double totalSeconds = totalMSecondsPassed/1000;
			
			double messagesXSeconds= totalMessageRetrieved/ totalSeconds;
			
			return messagesXSeconds;
		}
		return 0;
		
	}
	
}