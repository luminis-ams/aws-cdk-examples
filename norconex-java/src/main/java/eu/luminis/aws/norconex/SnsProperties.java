package eu.luminis.aws.norconex;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "sns")
@Component
public class SnsProperties {
    /**
     * Name of the AWS topic to connect to
     */
    private String statusUpdateTopicName;

    /**
     * Default AWS region to use
     */
    private String region = "eu-west-1";

    public String getStatusUpdateTopicName() {
        return statusUpdateTopicName;
    }

    public void setStatusUpdateTopicName(String statusUpdateTopicName) {
        this.statusUpdateTopicName = statusUpdateTopicName;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}
