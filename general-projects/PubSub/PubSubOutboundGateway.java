package com.ford.decisionplatform.config;

import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.messaging.handler.annotation.Payload; // Import Payload annotation

// Gateway for the 'outputMessageChannel' (publishing to 'data-factory.pubsub.topic')
@MessagingGateway(defaultRequestChannel = "outputMessageChannel")
public interface PubSubOutboundGateway {
    // The method signature only needs the payload, as the channel and topic are fixed by the gateway.
    void sendToPubSub(@Payload String payload);
    // You can add other methods if you need to send different object types
    // <T> void sendToPubSub(@Payload T object);
}
