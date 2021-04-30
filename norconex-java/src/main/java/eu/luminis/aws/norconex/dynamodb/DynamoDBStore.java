package eu.luminis.aws.norconex.dynamodb;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.norconex.collector.core.store.IDataStore;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;

import static eu.luminis.aws.norconex.dynamodb.DynamoInteractionUtil.createDataStoreTable;

/**
 * Might need to do sorting on insert date or something.
 * Search for DynamoDB and FIFO
 *
 * @param <T>
 */
public class DynamoDBStore<T> implements IDataStore<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamoDBStore.class);
    public static final String KEY_ID = "id";
    public static final String KEY_OBJECT = "object";
    public static final String KEY_ACTUAL_TYPE = "actual-type";

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeConverter())
            .create();

    private String name;
    private Class<T> type;
    private AmazonDynamoDB client;
    private DynamoDB dynamoDB;
    private String tableName;


    public DynamoDBStore(AmazonDynamoDB client, String name, Class<T> type, String tableName) {
        this.client = client;
        this.name = name;
        this.type = type;
        this.dynamoDB = new DynamoDB(client);
        this.tableName = tableName;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void save(String id, T object) {
        try {
            Table table = this.dynamoDB.getTable(this.tableName);
            Item item = new Item().withPrimaryKey(KEY_ID, id)
                    .withString(KEY_OBJECT, GSON.toJson(object))
                    .withString(KEY_ACTUAL_TYPE, object.getClass().getName());
            table.putItem(item);
        } catch (ResourceNotFoundException e) {
            LOGGER.warn("Trying to store an item in a data store that does not have a table yet {} {}", this.name, this.tableName);
        }
    }

    @Override
    public Optional<T> find(String id) {
        Table table = this.dynamoDB.getTable(this.tableName);
        Item item = table.getItem(KEY_ID, id);
        if (null == item) {
            return Optional.empty();
        }
        Class<T> actualType = extractActualType(item.getString(KEY_ACTUAL_TYPE));
        // TODO check if we can use the JSON stuff that is apparently in here
        return Optional.of(GSON.fromJson(item.getString(KEY_OBJECT), actualType));
    }

    @Override
    public Optional<T> findFirst() {
        ScanRequest scanRequest = new ScanRequest().withTableName(this.tableName).withLimit(1);
        ScanResult result = client.scan(scanRequest);

        Optional<Map<String, AttributeValue>> first = result.getItems().stream().findFirst();
        if (first.isPresent()) {
            String objectAsString = first.get().get(KEY_OBJECT).getS();
            String actualTypeAsString = first.get().get(KEY_ACTUAL_TYPE).getS();
            Class<T> actualType = extractActualType(actualTypeAsString);
            return Optional.of(GSON.fromJson(objectAsString, actualType));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public boolean exists(String id) {
        Table table = this.dynamoDB.getTable(this.tableName);
        try {
            Item item = table.getItem(KEY_ID, id);
            return item != null;
        } catch (ResourceNotFoundException e) {
            LOGGER.warn("The table with name {} {} cannot be found to check existing item {}", this.name, this.tableName, id);
        }
        return false;
    }

    @Override
    public long count() {

        Table table = this.dynamoDB.getTable(this.tableName);
        if (null != table) {
            TableDescription description = table.getDescription();
            if (null != description) {
                return description.getItemCount();
            }
        }

        return 0;
    }

    @Override
    public boolean delete(String id) {
        Table table = this.dynamoDB.getTable(this.tableName);
        table.deleteItem(KEY_ID, id);
        return true;
    }

    @Override
    public Optional<T> deleteFirst() {
        ScanRequest scanRequest = new ScanRequest().withTableName(this.tableName).withLimit(1);
        try {
            ScanResult result = client.scan(scanRequest);
            Optional<Map<String, AttributeValue>> first = result.getItems().stream().findFirst();

            if (first.isPresent()) {
                String id = first.get().get(KEY_ID).getS();
                this.delete(id);

                String objectAsString = first.get().get(KEY_OBJECT).getS();
                String actualTypeAsString = first.get().get(KEY_ACTUAL_TYPE).getS();
                Class<T> actualType = extractActualType(actualTypeAsString);
                return Optional.of(GSON.fromJson(objectAsString, actualType));
            }

        } catch (ResourceNotFoundException e) {
            LOGGER.info("Cannot find table to remove first item from {} {}", this.name, this.tableName);
        }
        return Optional.empty();
    }

    @Override
    public void clear() {
        LOGGER.info("About to clear the table with name {} {}", this.name, this.getTableName());
        Table table = this.dynamoDB.getTable(this.tableName);
        table.delete();
        try {
            table.waitForDelete();
        } catch (InterruptedException e) {
            LOGGER.warn("Got interrupted while waiting for deleting the table {}", tableName);
        }

        createDataStoreTable(dynamoDB, this.tableName);
    }

    @Override
    public void close() {
        // nothing specific to do here
    }

    @Override
    public boolean forEach(BiPredicate<String, T> predicate) {
        ScanRequest scanRequest = new ScanRequest().withTableName(this.tableName);
        ScanResult result = client.scan(scanRequest);
        for (Map<String, AttributeValue> item : result.getItems()) {
            String objectAsString = item.get(KEY_OBJECT).getS();
            String actualTypeAsString = item.get(KEY_ACTUAL_TYPE).getS();
            Class<T> actualType = extractActualType(actualTypeAsString);
            T t = GSON.fromJson(objectAsString, actualType);

            if (!predicate.test(item.get(KEY_ID).getS(), t)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isEmpty() {
        ScanRequest scanRequest = new ScanRequest().withTableName(this.tableName).withLimit(1);
        try {
            ScanResult result = client.scan(scanRequest);
            return result.getCount() == 0; // TODO check if this returns the overall number of documents
        } catch (ResourceNotFoundException e) {
            LOGGER.info("The table with name {} {} does not exist and so is empty", this.name, this.tableName);
            return true;
        }
    }

    public Class<T> getType() {
        return type;
    }

    public String getTableName() {
        return tableName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    @NotNull
    private Class<T> extractActualType(String typeAsString) {
        try {
            return (Class<T>) Class.forName(typeAsString);
        } catch (ClassNotFoundException e) {
            LOGGER.info("Cannot load the class {} while parsing from JSON", typeAsString);
            throw new RuntimeException("Cannot parse class: " + typeAsString);
        }
    }
}
