package eu.luminis.aws.norconex;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class SnsPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(SnsPublisher.class);

    private final String messageGroupId;
    private final String snsTopicName;
    private final SnsClient snsClient;
    private final String topicArn;
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class,new LocalDateAdapter()).create();

    @Autowired
    public SnsPublisher(SnsClient snsClient, SnsProperties snsProperties) {
        this.snsClient = snsClient;
        this.messageGroupId = snsProperties.getMessageGroup();
        this.snsTopicName = snsProperties.getStatusUpdateTopicName();
        if (isActive()) {
            this.topicArn = findSnsTopicArn();
        } else {
            this.topicArn = null;
        }
    }

    public void publishToTopic(String subject, StatusObject statusObject) {
        publishToTopic(subject, gson.toJson(statusObject));
    }

    public void publishToTopic(String subject, String originalMessage) {
        String message = StringUtils.hasLength(originalMessage) ? originalMessage : subject;
        try {
            PublishRequest request = PublishRequest.builder()
                    .topicArn(this.topicArn)
                    .subject(subject)
                    .message(message)
                    .messageGroupId(messageGroupId)
                    .build();

            PublishResponse result = snsClient.publish(request);
            LOGGER.info("Message sent, status was {}", result.sdkHttpResponse().statusCode());
        } catch (SnsException e) {
            LOGGER.warn("Problem sending status messages to topic {}", this.topicArn, e);
        }
    }

    public boolean isActive() {
        return StringUtils.hasLength(this.snsTopicName);
    }

    public String getCurrentTopic() {
        return this.topicArn;
    }

    private String findSnsTopicArn() {
        List<Topic> topics = listSNSTopics();
        Optional<Topic> foundTopic = topics.stream()
                .filter(topic -> topic.topicArn().contains(this.snsTopicName))
                .findFirst();

        if (foundTopic.isPresent()) {
            LOGGER.debug("Found the topic with name {}, it has arn {}", this.snsTopicName, topicArn);
            return foundTopic.get().topicArn();
        }
        throw new RuntimeException("Cannot find topic with name " + this.snsTopicName);
    }

    private List<Topic> listSNSTopics() {
        try {
            ListTopicsRequest request = ListTopicsRequest.builder()
                    .build();

            ListTopicsResponse result = snsClient.listTopics(request);
            return result.topics();
        } catch (SnsException e) {
            LOGGER.error("Cannot talk to SNS to obtain all topics", e);
        }
        return Collections.emptyList();
    }

}
