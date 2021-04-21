package eu.luminis.aws.norconex.dynamodb;

import com.norconex.collector.core.store.IDataStore;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Optional;
import java.util.function.BiPredicate;

public class DynamoDBStore<T> implements IDataStore<T> {

    private String name;
    private Class<T> type;
    private DynamoDbClient client;

    public DynamoDBStore(DynamoDbClient client, String name, Class<T> type) {
        this.client = client;
        this.name = name;
        this.type = type;

        // TODO do we need to create the Table here? Or should it be present already
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void save(String id, T object) {

    }

    @Override
    public Optional<T> find(String id) {
        return Optional.empty();
    }

    @Override
    public Optional<T> findFirst() {
        return Optional.empty();
    }

    @Override
    public boolean exists(String id) {
        return false;
    }

    @Override
    public long count() {
        return 0;
    }

    @Override
    public boolean delete(String id) {
        return false;
    }

    @Override
    public Optional<T> deleteFirst() {
        return Optional.empty();
    }

    @Override
    public void clear() {

    }

    @Override
    public void close() {

    }

    @Override
    public boolean forEach(BiPredicate<String, T> predicate) {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}
