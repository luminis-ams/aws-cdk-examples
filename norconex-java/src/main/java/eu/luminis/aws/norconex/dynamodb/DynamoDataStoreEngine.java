package eu.luminis.aws.norconex.dynamodb;

import com.norconex.collector.core.crawler.Crawler;
import com.norconex.collector.core.store.IDataStore;
import com.norconex.collector.core.store.IDataStoreEngine;
import com.norconex.commons.lang.file.FileUtil;
import com.norconex.commons.lang.text.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * The engine connects to DynamoDB with the right credentials. Dynamo contains a number of collections. We might want to
 * make them easier to identify as belonging to each other.
 * <p>
 * One table contains references to the other tables, this the one with the configured key: --storetypes
 * We should read this collection and keep the data locally available and up to date
 * <p>
 * https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-dynamodb-items.html
 */
public class DynamoDataStoreEngine implements IDataStoreEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamoDataStoreEngine.class);

    private final DynamoDBProperties dynamoDBProperties;

    private static final String STORE_TYPES_KEY = DynamoDataStoreEngine.class.getName() + "--storetypes";

    private DynamoDbClient client;

    public DynamoDataStoreEngine(DynamoDBProperties dynamoDBProperties) {
        this.dynamoDBProperties = dynamoDBProperties;
    }


    @Override
    public void init(Crawler crawler) {
        // create a clean table name
        String tableName = crawler.getCollector().getId() + "_" + crawler.getId();
        tableName = FileUtil.toSafeFileName(tableName);
        tableName = StringUtil.truncateWithHash(tableName, 63);

        // Check somehow if table exists
        Region region = Region.of(dynamoDBProperties.getRegion());
        if (dynamoDBProperties.getUseLocal()) {
            this.client = createLocalClient(region);
        } else {
            throw new IllegalArgumentException("Remote not supported yet");
        }

        // Check for existence of the store types table, if not present, create it
        List<String> allTables = DynamoInteractionUtil.listAllTables(this.client);
        boolean storeTypesTableAvailable = allTables.contains(STORE_TYPES_KEY);
        if (!storeTypesTableAvailable) {
            LOGGER.info("Table for store types is not yet available, about to create it.");
            CreateTableRequest request = CreateTableRequest.builder()
                    .attributeDefinitions(
                            AttributeDefinition.builder()
                                    .attributeName("name")
                                    .attributeType(ScalarAttributeType.S)
                                    .build(),
                            AttributeDefinition.builder()
                                    .attributeName("type")
                                    .attributeType(ScalarAttributeType.S)
                                    .build()
                    )
                    .keySchema(
                            KeySchemaElement.builder()
                                    .attributeName("name")
                                    .keyType(KeyType.HASH)
                                    .build(),
                            KeySchemaElement.builder()
                                    .attributeName("type")
                                    .keyType(KeyType.RANGE)
                                    .build()
                    )
                    .provisionedThroughput(ProvisionedThroughput.builder()
                            .readCapacityUnits(1L)
                            .writeCapacityUnits(1L)
                            .build())
                    .tableName(STORE_TYPES_KEY)
                    .build();
            DynamoInteractionUtil.createTable(this.client, request);
            LOGGER.info("Table for store types is created.");
        } else {
            LOGGER.info("Table for store types is available.");
        }
    }

    @Override
    public boolean clean() {
        return false;
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.close();
        }
        LOGGER.info("DynamoDB data store engine closed.");
    }

    @Override
    public <T> IDataStore<T> openStore(String name, Class<T> type) {
        HashMap<String, AttributeValue> itemValues = new HashMap<>();
        itemValues.put("name", AttributeValue.builder().s(name).build());
        itemValues.put("type", AttributeValue.builder().s(type.getName()).build());

        PutItemRequest request = PutItemRequest.builder()
                .tableName(STORE_TYPES_KEY)
                .item(itemValues)
                .build();

        try {
            this.client.putItem(request);
            LOGGER.info("Store with name {} and type {} was successfully added", name, type);
        } catch (ResourceNotFoundException e) {
            LOGGER.error("Error: The Amazon DynamoDB table \"{}\" can't be found.", STORE_TYPES_KEY);
            throw new RuntimeException("Could not find table for store types");
        } catch (DynamoDbException e) {
            LOGGER.error("Error: creating store {} of type {}.", name, type);
            throw new RuntimeException("Could not create store type");
        }

        return new DynamoDBStore<>(this.client, name, type);
    }

    @Override
    public boolean dropStore(String name) {
        HashMap<String, AttributeValue> keyToGet = new HashMap<>();

        keyToGet.put("name", AttributeValue.builder()
                .s(name)
                .build());

        DeleteItemRequest deleteReq = DeleteItemRequest.builder()
                .tableName(STORE_TYPES_KEY)
                .key(keyToGet)
                .build();
        try {
            this.client.deleteItem(deleteReq);
            return true;
        } catch (DynamoDbException e) {
            LOGGER.error("Error: deleting store {}.", name);
        }
        return false;
    }

    @Override
    public boolean renameStore(IDataStore<?> dataStore, String newName) {
        HashMap<String, AttributeValue> itemKey = new HashMap<>();

        itemKey.put("name", AttributeValue.builder().s(dataStore.getName()).build());

        HashMap<String, AttributeValueUpdate> updatedValues =
                new HashMap<>();

        // Update the column specified by name with updatedVal
        updatedValues.put("name", AttributeValueUpdate.builder()
                .value(AttributeValue.builder().s(newName).build())
                .action(AttributeAction.PUT)
                .build());

        UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName(STORE_TYPES_KEY)
                .key(itemKey)
                .attributeUpdates(updatedValues)
                .build();

        try {
            this.client.updateItem(request);
            return true;
        } catch (ResourceNotFoundException e) {
            LOGGER.error("Error: The Amazon DynamoDB table \"{}\" can't be found.", STORE_TYPES_KEY);
            throw new RuntimeException("Could not find table for store types");
        } catch (DynamoDbException e) {
            LOGGER.error("Error: renaming store {} to {}.", dataStore.getName(), newName);
        }
        return false;
    }

    @Override
    public Set<String> getStoreNames() {

        return null;
    }

    @Override
    public Optional<Class<?>> getStoreType(String name) {
        return Optional.empty();
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


}
