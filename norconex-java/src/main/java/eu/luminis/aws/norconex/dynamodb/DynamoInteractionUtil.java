package eu.luminis.aws.norconex.dynamodb;

import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.TableCollection;
import com.amazonaws.services.dynamodbv2.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static com.amazonaws.services.dynamodbv2.model.ScalarAttributeType.S;
import static eu.luminis.aws.norconex.dynamodb.DynamoDBStore.KEY_ID;

public class DynamoInteractionUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamoInteractionUtil.class);

    public static void createDataStoreTable(DynamoDB dynamoDB,String tableName) {
        LOGGER.info("Table for data store {} is not yet available, about to create it.", tableName);
        List<AttributeDefinition> attributeDefinitions = new ArrayList<>();
        attributeDefinitions.add(new AttributeDefinition().withAttributeName(KEY_ID).withAttributeType(S));

        List<KeySchemaElement> keySchema = new ArrayList<>();
        keySchema.add(new KeySchemaElement().withAttributeName(KEY_ID).withKeyType(KeyType.HASH));

        CreateTableRequest request = new CreateTableRequest()
                .withTableName(tableName)
                .withKeySchema(keySchema)
                .withAttributeDefinitions(attributeDefinitions)
                .withBillingMode(BillingMode.PAY_PER_REQUEST);

        Table table = dynamoDB.createTable(request);
        try {
            TableDescription tableDescription = table.waitForActive();
            LOGGER.info("Created the table with name {}", tableDescription.getTableName());
        } catch (InterruptedException e) {
            LOGGER.warn("Got interrupted while waiting for table to be created.");
        }
        LOGGER.info("Table for data store {} is created.", tableName);
    }

    public static void logAllTables(DynamoDB dynamoDB) {
        LOGGER.info("********** All tables ***************");
        TableCollection<ListTablesResult> tables = dynamoDB.listTables();
        for (Table table : tables) {
            LOGGER.info("Found table: {}", table.getTableName());
        }
        LOGGER.info("********** end of All tables ********");
    }

    public static void deleteAllTables(DynamoDB dynamoDB) {
        TableCollection<ListTablesResult> tables = dynamoDB.listTables();
        for (Table table : tables) {
            table.delete();
            try {
                table.waitForDelete();
            } catch (InterruptedException e) {
                LOGGER.error("Problem while waiting for delete", e);
            }
        }
    }
}
