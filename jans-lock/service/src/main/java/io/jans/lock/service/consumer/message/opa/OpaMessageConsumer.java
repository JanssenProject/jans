package io.jans.lock.service.consumer.message.opa;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.jans.lock.service.external.ExternalLockService;
import io.jans.lock.service.external.context.ExternalLockContext;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.message.consumer.MessageConsumer;
import io.jans.service.net.BaseHttpService;
import io.jans.util.StringHelper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * OPA message consumer
 *
 * @author Yuriy Movchan Date: 12/25/2023
 */
@ApplicationScoped
public class OpaMessageConsumer extends MessageConsumer {
	
	public static String MESSAGE_CONSUMER_TYPE = "OPA";

	@Inject
	private Logger log;

	@Inject
	private ExternalLockService externalLockService;

	@Inject
	private BaseHttpService httpService;

	private ObjectMapper objectMapper;
	
	@PostConstruct
	public void init() {
        this.objectMapper = new ObjectMapper();		
	}

	/*
	 * Message: {"tknTyp" : "id_token", "tknCde": "UUID"}
	 */
	@Override
	@Asynchronous
	public void onMessage(String channel, String message) {
		log.info("onMessage {} : {}", channel, message);

		try {
			JsonNode messageNode = objectMapper.readTree(message);
			
			if (!(messageNode.hasNonNull("tknTyp") && messageNode.hasNonNull("tknCde") && messageNode.hasNonNull("tknOp"))) {
				log.error("Message has missing tknOp or tknTyp, or tknTyp: '{}'", message);
				return;
			}

			String tknOp = messageNode.get("tknOp").asText();
			if (StringHelper.equalsIgnoreCase(tknOp, "add")) {
				putData(messageNode);
			} else if (StringHelper.equalsIgnoreCase(tknOp, "del")) {
				removeData(messageNode);
			} else {
				log.error("Message has unsupported operation: '{}'", message);
			}
		} catch (JacksonException ex) {
			log.error("Failed to parse messge: '{}'", message, ex);
		}
	}

	@Override
	public void onSubscribe(String channel, int subscribedChannels) {
		log.debug("onSubscribe {} : {}", channel, subscribedChannels);
	}

	@Override
	public void onUnsubscribe(String channel, int subscribedChannels) {
		log.debug("onUnsubscribe {} : {}", channel, subscribedChannels);
	}

	@Override
	public String getMessageConsumerType() {
		return MESSAGE_CONSUMER_TYPE;
	}

	private boolean putData(JsonNode messageNode) {
		ExternalLockContext lockContext = new ExternalLockContext();

		/*
		 * Data: {token_entry_as_json}
		 */
		ObjectNode dataNode = objectMapper.createObjectNode();
		dataNode.put("entry", "{}");
		
		externalLockService.beforeDataPut(messageNode, dataNode, lockContext);
		
		if (lockContext.isCancelPdpOperation()) {
			log.debug("DataPut was canceled by script");
			return true;
		}

		// Send rest request to OPA
		String tknTyp = messageNode.get("tknTyp").asText();
		String tknCde = messageNode.get("tknCde").asText();

		HttpPut request = new HttpPut(String.format("http://localhost:8181/v1/data/%s/%s", tknTyp, tknCde));
//		request.addHeader("Content-Type", "application/json");
		request.addHeader("If-None-Match", "*");
		
		StringEntity stringEntity = new StringEntity("{}", "application/json");
		request.setEntity(stringEntity);

		try (CloseableHttpClient httpClient = httpService.getHttpsClient();) {
			HttpResponse httpResponse = httpClient.execute(request);
			System.out.println(httpResponse);
			System.out.println(httpResponse.getStatusLine());
		} catch (IOException ex) {
	    	log.error("Failed to execute put data request", ex);
	    	return false;
		}

		return true;
	}

	public boolean removeData(JsonNode messageNode) {
		ExternalLockContext lockContext = new ExternalLockContext();

		externalLockService.beforeDataRemoval(messageNode, lockContext);
		
		if (lockContext.isCancelPdpOperation()) {
			log.debug("DataRemoval was canceled by script");
			return true;
		}

		// Send rest request to OPA
		String tknTyp = messageNode.get("tknTyp").asText();
		String tknCde = messageNode.get("tknCde").asText();

		HttpDelete request = new HttpDelete(String.format("http://localhost:8181/v1/data/%s/%s", tknTyp, tknCde));

		try (CloseableHttpClient httpClient = httpService.getHttpsClient();) {
			HttpResponse httpResponse = httpClient.execute(request);
			System.out.println(httpResponse);
			System.out.println(httpResponse.getStatusLine());
		} catch (IOException ex) {
	    	log.error("Failed to execute delete data request", ex);
	    	return false;
		}

		// Sent rest request to OPA
		return true;
	}

}
