package eu.luminis.aws.norconex.dynamodb;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.amazonaws.services.dynamodbv2.model.ScalarAttributeType.S;
import static eu.luminis.aws.norconex.dynamodb.DynamoInteractionUtil.createDynamoDBClient;
import static eu.luminis.aws.norconex.dynamodb.DynamoInteractionUtil.doCreateTable;

@Repository
public class DynamoDBRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamoDBRepository.class);
    private static final String KEY_ID_HASH = "crawler";
    private static final String KEY_ID_RANGE = "timestamp";
    private static final String KEY_EVENTS = "events";

    private final DynamoDBProperties dynamoDBProperties;

    private AmazonDynamoDB client; // Used to connect to AWS DynamoDB
    private DynamoDB dynamoDB; // Make some functionalities easier available than the raw client
    private static final Gson GSON = new GsonBuilder().create();


    public DynamoDBRepository(DynamoDBProperties dynamoDBProperties) {
        this.dynamoDBProperties = dynamoDBProperties;
    }

    public void storeCrawlerStats(String crawlerName, Map<String, Long> crawlerStats) {
        String tableName = getCrawlerStatsTableName();
        String timeStamp = ZonedDateTime
                .now( ZoneId.systemDefault() )
                .format( DateTimeFormatter.ofPattern( "uuuuMMddHHmmss" ) );
        try {
            String json = GSON.toJson(crawlerStats);
            Table table = this.dynamoDB.getTable(tableName);
            Item item = new Item()
                    .withPrimaryKey(KEY_ID_HASH, crawlerName)
                    .withPrimaryKey(KEY_ID_RANGE, timeStamp)
                    .withString(KEY_EVENTS, json);
            table.putItem(item);
        } catch (ResourceNotFoundException e) {
            LOGGER.warn("Trying to store crawler stats, but get an error", e);
        }

    }

    @PostConstruct
    public void init() {
        this.client = createDynamoDBClient(this.dynamoDBProperties);
        this.dynamoDB = new DynamoDB(this.client);

        Table table = dynamoDB.getTable(getCrawlerStatsTableName());
        try {
            if (null != table && table.describe() != null) {
                return;
            }
        } catch (ResourceNotFoundException e) {
            LOGGER.info("Table for crawler stats does not exist.");
        }
        createCrawlerStatsTable();
    }

    @PreDestroy
    public void destroy() {
        this.client.shutdown();
    }

    private void createCrawlerStatsTable() {
        String tableName = getCrawlerStatsTableName();

        List<AttributeDefinition> attributeDefinitions = new ArrayList<>();
        attributeDefinitions.add(new AttributeDefinition().withAttributeName(KEY_ID_HASH).withAttributeType(S));
        attributeDefinitions.add(new AttributeDefinition().withAttributeName(KEY_ID_RANGE).withAttributeType(S));

        List<KeySchemaElement> keySchema = new ArrayList<>();
        keySchema.add(new KeySchemaElement().withAttributeName(KEY_ID_HASH).withKeyType(KeyType.HASH));
        keySchema.add(new KeySchemaElement().withAttributeName(KEY_ID_RANGE).withKeyType(KeyType.RANGE));

        doCreateTable(tableName, attributeDefinitions, keySchema, dynamoDB);
    }

    @NotNull
    private String getCrawlerStatsTableName() {
        return dynamoDBProperties.getTablePrefix() + "--crawler-stats";
    }

}
