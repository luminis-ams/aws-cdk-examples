package eu.luminis.aws.norconex.dynamodb;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.model.*;
import com.norconex.collector.core.crawler.Crawler;
import com.norconex.collector.core.store.IDataStore;
import com.norconex.collector.core.store.IDataStoreEngine;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;

import static com.amazonaws.services.dynamodbv2.model.ScalarAttributeType.S;
import static eu.luminis.aws.norconex.dynamodb.DynamoDBStore.KEY_ID;
import static eu.luminis.aws.norconex.dynamodb.DynamoDBStore.KEY_OBJECT;
import static eu.luminis.aws.norconex.dynamodb.DynamoInteractionUtil.*;

/**
 * The engine connects to DynamoDB with the right credentials. Dynamo contains a number of collections. We might want to
 * make them easier to identify as belonging to each other.
 * <p>
 * One table contains references to the other tables, this the one with the configured key: --storetypes
 * We should read this collection and keep the data locally available and up to date
 * <p>
 * https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-dynamodb-items.html
 * https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/JavaDocumentAPICRUDExample.html
 */
public class DynamoDataStoreEngine implements IDataStoreEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamoDataStoreEngine.class);
    public static final String KEY_NAME = "name";
    public static final String KEY_TABLE_NAME = "table-name";
    public static final String KEY_TYPE = "type";

    private final DynamoDBProperties dynamoDBProperties;

    public static final String STORE_TYPES_TABLE = DynamoDataStoreEngine.class.getName() + "--storetypes";

    private AmazonDynamoDB client;
    private DynamoDB dynamoDB;

    public DynamoDataStoreEngine(DynamoDBProperties dynamoDBProperties) {
        this.dynamoDBProperties = dynamoDBProperties;
    }

    @Override
    public void init(Crawler crawler) {
        if (dynamoDBProperties.getUseLocal()) {
            this.client = AmazonDynamoDBClientBuilder.standard()
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://localhost:8000", "us-west-1"))
                    .build();
            this.dynamoDB = new DynamoDB(this.client);
        } else {
            // TODO decide what to do when we want to use a remote dynamodb
            throw new IllegalArgumentException("Remote not supported yet");
        }

        boolean storeTypesTableAvailable = checkIfTableExists(dynamoDB, STORE_TYPES_TABLE);
        if (!storeTypesTableAvailable) {
            createStoreTypeTable();
        } else {
            LOGGER.info("Table for store types is available.");
        }
    }

    @Override
    public boolean clean() {
        LOGGER.info("About to clean all tables from ");
        TableCollection<ListTablesResult> tables = dynamoDB.listTables();
        Table table = this.dynamoDB.getTable(STORE_TYPES_TABLE);
        table.delete();
        return tables.iterator().hasNext();
    }

    @Override
    public void close() {
        // TODO check if we need to do some cleaning up here
        LOGGER.info("DynamoDB data store engine closed.");
    }

    @Override
    public <T> IDataStore<T> openStore(String name, Class<T> type) {
        LOGGER.info("Open data store for name {} with type {}", name, type);

        Table table = this.dynamoDB.getTable(STORE_TYPES_TABLE);
        String tableName;

        // If there is a data store with the name, obtain the table name, else create it
        Item foundDataStore = table.getItem(KEY_NAME, name);
        if (null != foundDataStore) {
            tableName = foundDataStore.getString(KEY_TABLE_NAME);
        } else {
            LOGGER.info("Data store with not yet available, need to create it.");
            tableName = newTableName(name);
            Item item = new Item()
                    .withPrimaryKey("name", name)
                    .withString("type", type.getName())
                    .withString("table-name", tableName);
            table.putItem(item);
        }

        if (!checkIfTableExists(this.dynamoDB, tableName)) {
            createDataStoreTable(this.dynamoDB, tableName);
        } else {
            LOGGER.info("Table {} is already available.", name);
        }

        return new DynamoDBStore<>(this.client, name, type, tableName);
    }

    @Override
    public boolean dropStore(String name) {
        LOGGER.info("Open data store with name {} ", name);

        Table storeTable = this.dynamoDB.getTable(STORE_TYPES_TABLE);
        Item dataStoreToRemove = storeTable.getItem(KEY_NAME, name);
        String tableNameToRemove = dataStoreToRemove.getString(KEY_TABLE_NAME);
        Table table = this.dynamoDB.getTable(tableNameToRemove);
        try {
            storeTable.deleteItem(KEY_NAME, name);
            table.delete();
            return true;
        } catch (Exception e) {
            LOGGER.error("Could not delete the table {}", name);
        }
        return false;
    }

    @Override
    public boolean renameStore(IDataStore<?> dataStore, String newName) {
        LOGGER.info("Rename data store for name {} to name {}", dataStore.getName(), newName);

        boolean targetExists = false;

        Table dataStoresTypeTable = dynamoDB.getTable(STORE_TYPES_TABLE);
        // Check if entry exists for new name
        try {
            Item existingDataStoreWithNewName = dataStoresTypeTable.getItem(KEY_NAME, newName);
            if (null != existingDataStoreWithNewName) {
                targetExists = true;
            }
        } catch (ResourceNotFoundException e) {
            // nothing special
            LOGGER.info("The target name for a resource does not yet exist: {}", newName);
        }

        try {
            String oldTableName;
            Item oldDataStoreItem = dataStoresTypeTable.getItem(KEY_NAME, dataStore.getName());
            if (oldDataStoreItem != null) {
                oldTableName = oldDataStoreItem.getString(KEY_TABLE_NAME);
            } else {
                oldTableName = newTableName(newName);
                createDataStoreTable(dynamoDB, oldTableName);
            }
            dataStoresTypeTable.deleteItem(KEY_NAME, dataStore.getName());

            DynamoDBStore<?> dynamoDBStore = (DynamoDBStore<?>) dataStore;
            Item item = new Item().withPrimaryKey(KEY_NAME, newName)
                    .withString(KEY_TYPE, dynamoDBStore.getType().getName())
                    .withString(KEY_TABLE_NAME, oldTableName);
            dataStoresTypeTable.putItem(item);

            dynamoDBStore.setName(newName);
        } catch (AmazonDynamoDBException e) {
            LOGGER.info("Most likely we cannot find the datastore to rename");
        }

        return targetExists;
    }

    @Override
    public Set<String> getStoreNames() {
        ScanRequest scanRequest = new ScanRequest().withTableName(STORE_TYPES_TABLE);

        ScanResult result = client.scan(scanRequest);
        Set<String> names = new HashSet<>();
        for (Map<String, AttributeValue> item : result.getItems()) {
            names.add(item.get("name").getS());
        }
        return names;
    }

    @Override
    public Optional<Class<?>> getStoreType(String name) {
        Table table = dynamoDB.getTable(STORE_TYPES_TABLE);
        Item oldItem = table.getItem(new PrimaryKey("name", name));
        String typeAsString = oldItem.getString("type");
        try {
            return Optional.of(Class.forName(typeAsString));
        } catch (ClassNotFoundException e) {
            LOGGER.error("Could not find class for name {}", name);
        }
        return Optional.empty();
    }

    @NotNull
    private String newTableName(String name) {
        String tableName;
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        tableName = name + "-" + timeStamp;
        return tableName;
    }

    private void createStoreTypeTable() {
        LOGGER.info("Table for store types is not yet available, about to create it.");
        List<AttributeDefinition> attributeDefinitions = new ArrayList<>();
        attributeDefinitions.add(new AttributeDefinition().withAttributeName(KEY_NAME).withAttributeType(S));

        List<KeySchemaElement> keySchema = new ArrayList<>();
        keySchema.add(new KeySchemaElement().withAttributeName(KEY_NAME).withKeyType(KeyType.HASH));

        CreateTableRequest request = new CreateTableRequest()
                .withTableName(STORE_TYPES_TABLE)
                .withKeySchema(keySchema)
                .withAttributeDefinitions(attributeDefinitions)
                .withProvisionedThroughput(
                        new ProvisionedThroughput()
                                .withReadCapacityUnits(1L)
                                .withWriteCapacityUnits(1L)
                );

        Table table = dynamoDB.createTable(request);
        try {
            table.waitForActive();
        } catch (InterruptedException e) {
            LOGGER.warn("Got interrupted while waiting for table to be created.");
        }
        LOGGER.info("Table for store types is created.");
    }
}
