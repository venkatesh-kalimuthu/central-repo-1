

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.outbound.PubSubMessageHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.messaging.MessageHandler;

@Configuration
public class PubSubConfig {

    @Value("${data-factory.pubsub.topic}")
    private String topic;

    @Value("${data-factory.pubsub.alfa-response-topic}")
    private String alfaResponseTopic;

    @Bean
    public MessagingTemplate messagingTemplate() {
        return new MessagingTemplate();
    }

    private MessageHandler pubSubMessageHandler(PubSubTemplate pubsubTemplate, String topic) {
        return new PubSubMessageHandler(pubsubTemplate, topic);
    }

    @Bean
    @ConditionalOnProperty(prefix = "spring.cloud.gcp.pubsub", name = "enabled", havingValue = "true")
    @ServiceActivator(inputChannel = "outputMessageChannel")
    public MessageHandler messageSender(PubSubTemplate pubsubTemplate) {
        return pubSubMessageHandler(pubsubTemplate, topic);
    }

    @Bean
    @ConditionalOnProperty(prefix = "spring.cloud.gcp.pubsub", name = "enabled", havingValue = "true")
    @ServiceActivator(inputChannel = "alfaResponseOutputMessageChannel")
    public MessageHandler alfaResponseMessageSender(PubSubTemplate pubsubTemplate) {
        return pubSubMessageHandler(pubsubTemplate, alfaResponseTopic);
    }
}
