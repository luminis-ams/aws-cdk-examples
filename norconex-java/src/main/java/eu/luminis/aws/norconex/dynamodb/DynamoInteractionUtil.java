package eu.luminis.aws.norconex.dynamodb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;

import java.util.ArrayList;
import java.util.List;

public class DynamoInteractionUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamoInteractionUtil.class);

    public static List<String> listAllTables(DynamoDbClient client) {
        List<String> tables = new ArrayList<>();
        boolean more = true;
        String lastTableName = null;
        while (more) {
            ListTablesResponse response = null;
            ListTablesRequest request;
            if (lastTableName == null) {
                request = ListTablesRequest.builder().build();
            } else {
                request = ListTablesRequest.builder().exclusiveStartTableName(lastTableName).build();
            }
            response = client.listTables(request);

            List<String> tableNames = response.tableNames();
            if (tableNames.size() > 0) {
                tables.addAll(tableNames);
            }
            lastTableName = response.lastEvaluatedTableName();
            if (lastTableName == null) {
                more = false;
            }
        }
        return tables;
    }

    public static String createTable(DynamoDbClient client, CreateTableRequest request) {
        DynamoDbWaiter dbWaiter = client.waiter();

        String newTable = "";
        try {
            CreateTableResponse response = client.createTable(request);
            DescribeTableRequest tableRequest = DescribeTableRequest.builder()
                    .tableName(request.tableName())
                    .build();

            // Wait until the Amazon DynamoDB table is created
            WaiterResponse<DescribeTableResponse> waiterResponse = dbWaiter.waitUntilTableExists(tableRequest);
            waiterResponse.matched().response().ifPresent(System.out::println);

            newTable = response.tableDescription().tableName();
            return newTable;

        } catch (DynamoDbException e) {
            LOGGER.error("Problem while creating a table in DynamoDB", e);
            throw new RuntimeException("Problem creating a table in DynamoDB");
        }
    }

}
