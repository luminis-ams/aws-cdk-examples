package eu.luminis.aws.norconex;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

@Component
public class SnsClientFactory extends AbstractFactoryBean<SnsClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SnsClientFactory.class);
    private final SnsProperties snsProperties;

    public SnsClientFactory(SnsProperties snsProperties) {
        this.snsProperties = snsProperties;
    }

    @Override
    public Class<?> getObjectType() {
        return SnsClient.class;
    }

    @NotNull
    @Override
    protected SnsClient createInstance() throws Exception {
        return SnsClient.builder()
                .region(Region.of(snsProperties.getRegion()))
                .build();
    }
}
