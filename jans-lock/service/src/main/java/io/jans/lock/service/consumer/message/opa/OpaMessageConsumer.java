package io.jans.lock.service.consumer.message.opa;

import java.io.IOException;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.service.TokenService;
import io.jans.lock.service.external.ExternalLockService;
import io.jans.lock.service.external.context.ExternalLockContext;
import io.jans.model.token.TokenEntity;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.qualifier.Implementation;
import io.jans.service.message.consumer.MessageConsumer;
import io.jans.service.net.BaseHttpService;
import io.jans.util.StringHelper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.jodah.expiringmap.ExpirationListener;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;

/**
 * OPA message consumer
 *
 * @author Yuriy Movchan Date: 12/25/2023
 */
@Implementation
@ApplicationScoped
public class OpaMessageConsumer extends MessageConsumer {

	public static String MESSAGE_CONSUMER_TYPE = "OPA";

	@Inject
	private Logger log;

    @Inject
	private AppConfiguration appConfiguration;

	@Inject
	private ExternalLockService externalLockService;

	@Inject
	private BaseHttpService httpService;
	
	@Inject
	private TokenService tokenService;

	private ObjectMapper objectMapper;

    private ExpiringMap<String, String> loadedTokens;
	private OpaExpirationListener expirationListener;

	@PostConstruct
	public void init() {
        this.objectMapper = new ObjectMapper();
        this.expirationListener = new OpaExpirationListener();
        this.loadedTokens = ExpiringMap.builder().expirationPolicy(ExpirationPolicy.CREATED).variableExpiration().expirationListener(expirationListener).build();
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
				putData(message, messageNode);
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

	private boolean putData(String message, JsonNode messageNode) {
		ExternalLockContext lockContext = new ExternalLockContext();

		String tknTyp = messageNode.get("tknTyp").asText();
		String tknCde = messageNode.get("tknCde").asText();
		
		TokenEntity tokenEntity = tokenService.findToken(tknCde);
		log.debug("Token {} loaded successfully", tokenEntity);

		/*
		 * Data: {token_entry_as_json}
		 */
		ObjectNode dataNode = objectMapper.createObjectNode();
		dataNode.put("entry", "{\"test\" : 1}");
		
		externalLockService.beforeDataPut(messageNode, dataNode, lockContext);
		
		if (lockContext.isCancelPdpOperation()) {
			log.debug("DataPut was canceled by script");
			return true;
		}

		// Send rest request to OPA
		
		String baseUrl = appConfiguration.getOpaConfiguration().getBaseUrl();

		HttpPut request = new HttpPut(String.format("%s/data/%s/%s", baseUrl, tknTyp, tknCde));
		request.addHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
		request.addHeader("If-None-Match", "*");
		
		StringEntity stringEntity = new StringEntity("{}", ContentType.APPLICATION_JSON);
		request.setEntity(stringEntity);

		boolean result = false;
		try {
			CloseableHttpClient httpClient = httpService.getHttpsClient();
			HttpResponse httpResponse = httpClient.execute(request);
			
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			log.debug("Get OPA add data for token '{}' response with status code '{}'", tknCde, statusCode);

			result = statusCode == HttpStatus.SC_NO_CONTENT;
		} catch (IOException ex) {
	    	log.error("Failed to execute put data request", ex);
		}

		if (result) {
			loadedTokens.put(tknCde, message, ExpirationPolicy.CREATED, getExpirationInSeconds(tokenEntity), TimeUnit.SECONDS);
		}
		
		return result;
	}

	private boolean removeData(JsonNode messageNode) {
		ExternalLockContext lockContext = new ExternalLockContext();

		externalLockService.beforeDataRemoval(messageNode, lockContext);
		
		if (lockContext.isCancelPdpOperation()) {
			log.debug("DataRemoval was canceled by script");
			return true;
		}

		// Send rest request to OPA
		String tknTyp = messageNode.get("tknTyp").asText();
		String tknCde = messageNode.get("tknCde").asText();

		String baseUrl = appConfiguration.getOpaConfiguration().getBaseUrl();

		HttpDelete request = new HttpDelete(String.format("%s/data/%s/%s", baseUrl, tknTyp, tknCde));

		boolean result = false;
		try {
			CloseableHttpClient httpClient = httpService.getHttpsClient();
			HttpResponse httpResponse = httpClient.execute(request);

			int statusCode = httpResponse.getStatusLine().getStatusCode();
			log.debug("Get OPA remove data for token '{}' response with status code '{}'", tknCde, statusCode);

			result = statusCode == HttpStatus.SC_NO_CONTENT;
		} catch (IOException ex) {
	    	log.error("Failed to execute delete data request", ex);
		}

		loadedTokens.remove(tknCde);
		
		return result;
	}

	public long getExpirationInSeconds(TokenEntity tokenEntity) {
        final Long duration = Duration.between(new Date().toInstant(), tokenEntity.getExpirationDate().toInstant()).getSeconds();

        return duration;
    }
	private class OpaExpirationListener implements ExpirationListener<String, String> {

		public void expired(String key, String message) {
	    	log.debug("Deleting expired token {}", key);
			JsonNode messageNode;
			try {
				messageNode = objectMapper.readTree(message);
				removeData(messageNode);
			} catch (JacksonException ex) {
				log.error("Failed to parse messge: '{}'", message, ex);
			}
		}
	}

}
