package eu.luminis.aws.norconex;

import com.norconex.collector.core.crawler.CrawlerEvent;
import com.norconex.collector.core.crawler.CrawlerLifeCycleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class SnsTopicCrawlerLifeCycleListener extends CrawlerLifeCycleListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(SnsTopicCrawlerLifeCycleListener.class);

    private final String messageGroupId;
    private final String snsTopicName;
    private final SnsClient snsClient;
    private final String topicArn;

    public SnsTopicCrawlerLifeCycleListener(SnsClient snsClient, SnsProperties snsProperties, String messageGroupId) {
        super();
        this.snsTopicName = snsProperties.getStatusUpdateTopicName();
        this.snsClient = snsClient;
        this.messageGroupId = messageGroupId;
        this.topicArn = findSnsTopicArn();
    }

    @Override
    protected void onCrawlerShutdown(CrawlerEvent event) {
        publishToTopic("CRAWLER_SHUTDOWN", event.getMessage());
    }

    @Override
    protected void onCrawlerInitBegin(CrawlerEvent event) {
        publishToTopic("CRAWLER_INIT_BEGIN", event.getMessage());
    }

    @Override
    protected void onCrawlerInitEnd(CrawlerEvent event) {
        publishToTopic("CRAWLER_INIT_END", event.getMessage());
    }

    @Override
    protected void onCrawlerRunBegin(CrawlerEvent event) {
        publishToTopic("CRAWLER_RUN_BEGIN", event.getMessage());
    }

    @Override
    protected void onCrawlerRunEnd(CrawlerEvent event) {
        publishToTopic("CRAWLER_RUN_END", event.getMessage());
    }

    @Override
    protected void onCrawlerStopBegin(CrawlerEvent event) {
        publishToTopic("CRAWLER_STOP_BEGIN", event.getMessage());
    }

    @Override
    protected void onCrawlerStopEnd(CrawlerEvent event) {
        publishToTopic("CRAWLER_STOP_END", event.getMessage());
    }

    @Override
    protected void onCrawlerCleanBegin(CrawlerEvent event) {
        publishToTopic("CRAWLER_CLEAN_BEGIN", event.getMessage());
    }

    @Override
    protected void onCrawlerCleanEnd(CrawlerEvent event) {
        publishToTopic("CRAWLER_CLEAN_END", event.getMessage());
    }

    private void publishToTopic(String subject, String originalMessage) {
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
