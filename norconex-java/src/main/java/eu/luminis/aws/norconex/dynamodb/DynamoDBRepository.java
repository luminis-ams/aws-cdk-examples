package eu.luminis.aws.norconex.dynamodb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Repository
public class DynamoDBRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamoDBRepository.class);

    private final DynamoDBProperties dynamoDBProperties;

    private DynamoDbClient client;

    public DynamoDBRepository(DynamoDBProperties dynamoDBProperties) {
        this.dynamoDBProperties = dynamoDBProperties;
    }

    @PostConstruct
    public void initDynamo() throws URISyntaxException {
        Region region = Region.of(dynamoDBProperties.getRegion());
        if (dynamoDBProperties.getUseLocal()) {
            this.client = createLocalClient(region);
        } else {
            throw new IllegalArgumentException("Remote not supported yet");
        }
    }

    @PreDestroy
    public void destroy() {
        client.close();
    }

    private DynamoDbClient createLocalClient(Region region) {
        try {
            return DynamoDbClient.builder()
                    .region(region)
                    .credentialsProvider(ProfileCredentialsProvider.builder().profileName(dynamoDBProperties.getProfileName()).build())
                    .endpointOverride(new URI(dynamoDBProperties.getLocalUri()))
                    .build();
        } catch (URISyntaxException e) {
            LOGGER.warn("Configured uri {} is not valid", dynamoDBProperties.getLocalUri());
            throw new IllegalArgumentException("URI is not valid: " + dynamoDBProperties.getLocalUri());
        }
    }

    public List<String> listTables() {
        return DynamoInteractionUtil.listAllTables(this.client);
    }
}
