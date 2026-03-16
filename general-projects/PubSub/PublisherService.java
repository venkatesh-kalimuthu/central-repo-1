package com.ford.decisionplatform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ford.decisionplatform.config.AlfaResponsePubSubOutboundGateway;
import com.ford.decisionplatform.config.PubSubOutboundGateway;
import com.ford.decisionplatform.utils.ApplicationConstants;
import com.ford.decisionplatform.utils.DecisionPlatformEnum;
import com.ford.decisionplatform.utils.GenericPlatformRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZonedDateTime;

/**
 * Service for publishing messages to Google Pub/Sub topics using the configured outbound gateway.
 * <p>
 * This service serializes {@link GenericPlatformRequest} objects and sends them to the specified topic.
 * It provides logging for both successful and failed publishing attempts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PublisherService {

    private final PubSubOutboundGateway generalPubSubGateway;
    private final AlfaResponsePubSubOutboundGateway alfaResponsePubSubGateway;

    @Autowired
    private org.springframework.integration.core.MessagingTemplate messagingTemplate;


    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String LOG_PREFIX = "[PublisherService] ";


    /**
     * Publishes a payload (either a String or an Object) to the specified Google Pub/Sub topic asynchronously using Spring's @Async.
     * <p>
     * If the payload is not a String, it will be serialized to JSON using Jackson's ObjectMapper.
     * The method validates that both payload and topic are not null or empty before publishing.
     * All actions and errors are logged with a context string for easier traceability.
     *
     * @param payload    the message payload to publish (String or Object)
     * @param topic      the Pub/Sub topic to publish to
     * @param logContext a context string to prepend to all log messages for this operation
     */
    @Async
    public void publishToPubSubAsync(Object payload, String topic, String logContext) {
        log.info("this method to be deprecated");
        if (payload == null) {
            log.error(LOG_PREFIX + logContext + "Cannot publish: payload is null");
            return;
        }
        if (topic == null || topic.isEmpty()) {
            log.error(LOG_PREFIX + logContext + "Cannot publish: topic is null or empty");
            return;
        }
        String payloadStr;
        try {
            if (payload instanceof String) {
                payloadStr = (String) payload;
            } else {
                payloadStr = objectMapper.writeValueAsString(payload);
            }
            log.info(LOG_PREFIX + logContext + "Payload to publish: {}", payloadStr);
            // Call the appropriate channel method based on topic
            if (topic.equalsIgnoreCase("fc_lending_decision_response_topic")) {
                publishToAlfaResponseChannel(payloadStr);
            } else {
                publishToOutputChannel(payloadStr);
            }
            log.info(LOG_PREFIX + logContext + "Payload published successfully to channel for topic: {}", topic);
        } catch (JsonProcessingException e) {
            log.error(LOG_PREFIX + logContext + "Message not published due to serialization error", e);
        } catch (Exception e) {
            log.error(LOG_PREFIX + logContext + "Message not published due to unexpected error", e);
        }
    }

    public void publishToPubSubAsync(GenericPlatformRequest payload,  
                                     DecisionPlatformEnum source, DecisionPlatformEnum targetApi, DecisionPlatformEnum component,
                                     String logContext) {
        try {
            GenericPlatformRequest clonedRequest =
                    objectMapper.readValue(objectMapper.writeValueAsString(payload), GenericPlatformRequest.class);
            clonedRequest.setSource(source);
            clonedRequest.setTargetApi(targetApi);
            clonedRequest.setComponent(component);
            clonedRequest.setTimestamp(ZonedDateTime.now().toString());
            publishToOutputChannel(objectMapper.writeValueAsString(clonedRequest));
            log.info(LOG_PREFIX + logContext + "Payload published successfully to outputMessageChannel");
        } catch (JsonProcessingException e) {
            log.info(LOG_PREFIX + logContext + "Message not published due to serialization error", e);
        } catch (Exception e) {
            log.info(LOG_PREFIX + logContext + "Message not published due to unexpected error", e);
        }
    }

    @Async
    public void publishToPubSubAsync(GenericPlatformRequest payload,
                                     DecisionPlatformEnum source, DecisionPlatformEnum targetApi, DecisionPlatformEnum component,
                                     String logContext, JsonNode data, Double duration) {
        try {
            GenericPlatformRequest clonedRequest = new GenericPlatformRequest(payload);
            clonedRequest.setData(data);
            clonedRequest.setSource(source);
            clonedRequest.setTargetApi(targetApi);
            clonedRequest.setComponent(component);
            clonedRequest.setTimestamp(ZonedDateTime.now().toString());
            clonedRequest.setDuration(duration);
            publishToOutputChannel(objectMapper.writeValueAsString(clonedRequest));
            log.info(LOG_PREFIX + logContext + "Payload published successfully to outputMessageChannel");
        } catch (JsonProcessingException e) {
            log.info(LOG_PREFIX + logContext + "Message not published due to serialization error", e);
        } catch (Exception e) {
            log.info(LOG_PREFIX + logContext + "Message not published due to unexpected error", e);
        }
    }

    /**
     * Publishes a message to the outputMessageChannel for general Pub/Sub publishing.
     * @param payload the message payload to publish
     */
    public void publishToOutputChannel(String payload) {
        messagingTemplate.send("outputMessageChannel", org.springframework.messaging.support.MessageBuilder.withPayload(payload).build());
    }

    /**
     * Publishes a message to the alfaResponseOutputMessageChannel for Alfa response Pub/Sub publishing.
     * @param payload the message payload to publish
     */

    public void publishToAlfaResponseChannel(String payload) {
        try {
            messagingTemplate.send("alfaResponseOutputMessageChannel", org.springframework.messaging.support.MessageBuilder.withPayload(payload).build());
            log.info(LOG_PREFIX + ApplicationConstants.RESPONSE_TO_ORGINATIONS + "Message published successfully to alfaresponsetopic");

        } catch (Exception e) {
            log.error(LOG_PREFIX + ApplicationConstants.RESPONSE_TO_ORGINATIONS + "Message not published due to unexpected error", e);
        }
    }


}
