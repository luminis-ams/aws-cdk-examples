package eu.luminis.aws.norconex.dynamodb;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
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

import java.util.*;

import static com.amazonaws.services.dynamodbv2.model.ScalarAttributeType.S;
import static eu.luminis.aws.norconex.dynamodb.DynamoInteractionUtil.createDataStoreTable;

/**
 * <p>
 * This engine manages the Data stores in DynamoDB. A datastore consists of a name and a table name. DynamoDB tables
 * are reused when renaming a DataStore.
 * </p>
 * <p>
 * One table contains references to the other tables, this the one with the configured key: --storetypes
 * We should read this collection and keep the data locally available and up to date. All table names are prefixed
 * with the provided prefix.
 * </p>
 * <p>
 * https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-dynamodb-items.html
 * https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/JavaDocumentAPICRUDExample.html
 * </p>
 */
public class DynamoDataStoreEngine implements IDataStoreEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamoDataStoreEngine.class);
    public static final String KEY_NAME = "name";
    public static final String KEY_TABLE_NAME = "table-name";
    public static final String KEY_TYPE = "type";

    private final DynamoDBProperties dynamoDBProperties;

    private AmazonDynamoDB client; // Used to connect to AWS DynamoDB
    private DynamoDB dynamoDB; // Make some functionalities easier available than the raw client

    public DynamoDataStoreEngine(DynamoDBProperties dynamoDBProperties) {
        this.dynamoDBProperties = dynamoDBProperties;
    }

    @Override
    public void init(Crawler crawler) {
        if (dynamoDBProperties.getUseLocal()) {
            this.client = AmazonDynamoDBClientBuilder.standard()
                    .withEndpointConfiguration(new AwsClientBuilder
                            .EndpointConfiguration(dynamoDBProperties.getLocalUri(), dynamoDBProperties.getRegion()))
                    .build();
        } else {
            this.client = AmazonDynamoDBClientBuilder
                    .standard()
                    .withRegion(Regions.fromName(dynamoDBProperties.getRegion()))
                    .build();
        }
        this.dynamoDB = new DynamoDB(this.client);

        DynamoInteractionUtil.logAllTables(this.dynamoDB);

        if (!checkIfTableExists()) {
            createStoreTypeTable();
        } else {
            LOGGER.info("Table for store types is available.");
            logAllDataStoresFromType();
        }
    }

    @Override
    public boolean clean() {
        LOGGER.info("About to clean all tables starting with {}", dynamoDBProperties.getTablePrefix());
        TableCollection<ListTablesResult> tables = dynamoDB.listTables();
        tables.forEach(table -> {
            if (table.getTableName().startsWith(dynamoDBProperties.getTablePrefix())) {
                this.dynamoDB.getTable(table.getTableName()).delete();
            }
        });
        return tables.iterator().hasNext();
    }

    @Override
    public void close() {
        LOGGER.info("DynamoDB data store engine closed.");
        logAllDataStoresFromType();
//        this.client.shutdown();
    }

    @Override
    public <T> IDataStore<T> openStore(String name, Class<T> type) {
        LOGGER.info("Open data store for name {} with type {}", name, type);

        Table dataStoreTypesTable = this.dynamoDB.getTable(obtainDataStoreTypeTableName());
        String tableName;

        // If there is a data store with the name, obtain the table name, else create it
        Item foundDataStore = dataStoreTypesTable.getItem(KEY_NAME, name);
        if (null != foundDataStore) {
            LOGGER.info("Existing Data store {} opened.", name);
            tableName = foundDataStore.getString(KEY_TABLE_NAME);
        } else {
            LOGGER.info("Data store with not yet available, need to create it.");
            tableName = newTableName();
            Item item = new Item()
                    .withPrimaryKey("name", name)
                    .withString("type", type.getName())
                    .withString("table-name", tableName);
            dataStoreTypesTable.putItem(item);
        }

        if (!checkIfTableExists(tableName)) {
            createDataStoreTable(this.dynamoDB, tableName);
        } else {
            LOGGER.info("Table {} is already available.", name);
        }

        return new DynamoDBStore<>(this.client, name, type, tableName);
    }

    @Override
    public boolean dropStore(String name) {
        LOGGER.info("Open data store with name {} ", name);

        Table storeTable = this.dynamoDB.getTable(obtainDataStoreTypeTableName());
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

        Table dataStoresTypeTable = dynamoDB.getTable(obtainDataStoreTypeTableName());
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
                oldTableName = newTableName();
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
        ScanRequest scanRequest = new ScanRequest().withTableName(obtainDataStoreTypeTableName());

        ScanResult result = client.scan(scanRequest);
        Set<String> names = new HashSet<>();
        for (Map<String, AttributeValue> item : result.getItems()) {
            names.add(item.get("name").getS());
        }
        return names;
    }

    @Override
    public Optional<Class<?>> getStoreType(String name) {
        Table table = dynamoDB.getTable(obtainDataStoreTypeTableName());
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
    private String newTableName() {
        return this.dynamoDBProperties.getTablePrefix() + UUID.randomUUID();
    }

    private void createStoreTypeTable() {
        LOGGER.info("Table for store types is not yet available, about to create it.");

        List<AttributeDefinition> attributeDefinitions = new ArrayList<>();
        attributeDefinitions.add(new AttributeDefinition().withAttributeName(KEY_NAME).withAttributeType(S));

        List<KeySchemaElement> keySchema = new ArrayList<>();
        keySchema.add(new KeySchemaElement().withAttributeName(KEY_NAME).withKeyType(KeyType.HASH));

        CreateTableRequest request = new CreateTableRequest()
                .withTableName(obtainDataStoreTypeTableName())
                .withKeySchema(keySchema)
                .withAttributeDefinitions(attributeDefinitions)
                .withBillingMode(BillingMode.PAY_PER_REQUEST);

        Table table = dynamoDB.createTable(request);
        try {
            table.waitForActive();
        } catch (InterruptedException e) {
            LOGGER.warn("Got interrupted while waiting for table to be created.");
        }
        LOGGER.info("Table for store types is created.");
    }

    public boolean checkIfTableExists() {
        String tableName = obtainDataStoreTypeTableName();
        return checkIfTableExists(tableName);
    }

    public boolean checkIfTableExists(String tableName) {
        TableCollection<ListTablesResult> tables = dynamoDB.listTables();
        boolean tableAvailable = false;
        for (Table table : tables) {
            if (table.getTableName().equals(tableName)) {
                tableAvailable = true;
                break;
            }
        }
        return tableAvailable;
    }

    private void logAllDataStoresFromType() {
        ScanRequest scanRequest = new ScanRequest().withTableName(obtainDataStoreTypeTableName());

        ScanResult result = client.scan(scanRequest);
        for (Map<String, AttributeValue> item : result.getItems()) {
            LOGGER.info("Found item with name {}, table {} and type {}",
                    item.get(KEY_NAME), item.get(KEY_TABLE_NAME), item.get(KEY_TYPE));
        }
    }

    private String obtainDataStoreTypeTableName() {
        return dynamoDBProperties.getTablePrefix() + "--storetypes";
    }
}
