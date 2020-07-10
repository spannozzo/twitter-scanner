package org.acme.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.acme.dto.Author;
import org.acme.dto.Message;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.junit.QuarkusTest;


@QuarkusTest
@TestMethodOrder(OrderAnnotation.class)
public class MessageServiceTest {

	@Inject
	MessageService service;
	
	@Test
	@Order(3)
	@Transactional
	public void can_retrieve_data() throws Exception {
		
		
		Map<Author, List<Message>> sortedResults=service.getmessages(1, "twitter");
		
		assertNotNull(sortedResults);
		assertTrue(!sortedResults.isEmpty());
		
		Message x=sortedResults.values().stream().findFirst().get().get(0);
		
		
		assertNotNull(x.getCreatedAt());
		assertNotNull(x.getId());
		assertNotNull(x.getText());
		assertNotNull(x.getAuthor());
		
		assertNotNull(x.getAuthor().getCreatedAt());
		assertNotNull(x.getAuthor().getId());
		assertNotNull(x.getAuthor().getName());
		assertNotNull(x.getAuthor().getScreenName());
		
		assertTrue(!x.getId().isEmpty());
		assertTrue(!x.getText().isEmpty());
		
		assertTrue(!x.getAuthor().getId().isEmpty());
		assertTrue(!x.getAuthor().getName().isEmpty());
		assertTrue(!x.getAuthor().getScreenName().isEmpty());
		
	}

}