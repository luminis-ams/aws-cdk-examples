package eu.luminis.aws.norconex;

import com.norconex.collector.core.Collector;
import com.norconex.collector.core.CollectorEvent;
import com.norconex.collector.core.CollectorLifeCycleListener;
import com.norconex.collector.http.HttpCollector;
import com.norconex.collector.http.HttpCollectorConfig;
import com.norconex.collector.http.crawler.HttpCrawlerConfig;
import com.norconex.collector.http.crawler.URLCrawlScopeStrategy;
import com.norconex.collector.http.delay.impl.GenericDelayResolver;
import com.norconex.importer.ImporterConfig;
import eu.luminis.committer.opensearch.OpenSearchCommitter;
import eu.luminis.norconex.datastore.dynamodb.DynamoDBProperties;
import eu.luminis.norconex.datastore.dynamodb.DynamoDBTableUtil;
import eu.luminis.norconex.datastore.dynamodb.DynamoDataStoreEngine;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import javax.annotation.PreDestroy;

import java.time.LocalDateTime;
import java.util.HashMap;

import static eu.luminis.aws.norconex.CollectorStatus.FINISHED;
import static eu.luminis.aws.norconex.CollectorStatus.RUNNING;

@Service
public class NorconexService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NorconexService.class);
    private final NorconexProperties norconexProperties;
    private final DynamoDBProperties dynamoDBProperties;
    private final DynamoDbClient dynamoDbClient;
    private final SnsPublisher snsPublisher;
    private final ImporterConfig importerConfig;

    private HttpCollector collector;

    private ApplicationContext context;

    @Autowired
    public NorconexService(NorconexProperties norconexProperties,
                           DynamoDBProperties dynamoDBProperties,
                           DynamoDbClient dynamoDbClient,
                           SnsProperties snsProperties,
                           SnsPublisher snsPublisher,
                           ImporterConfig importerConfig,
                           ApplicationContext context) {
        this.norconexProperties = norconexProperties;
        this.dynamoDBProperties = dynamoDBProperties;
        this.dynamoDbClient = dynamoDbClient;
        this.snsPublisher = snsPublisher;
        this.importerConfig = importerConfig;
        this.context = context;

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Norconex action: {}", norconexProperties.getAction());
            LOGGER.info("Norconex Elasticsearch Index Name : {}", norconexProperties.getElasticsearchIndexName());
            LOGGER.info("Norconex Elasticsearch Nodes: {}", norconexProperties.getElasticsearchNodes());
            LOGGER.info("Norconex Max depth: {}", norconexProperties.getMaxDepth());
            LOGGER.info("Norconex Start Urls: {}", norconexProperties.getStartUrls());

            LOGGER.info("Dynamo Local URI: {}", dynamoDBProperties.getLocalUri());
            LOGGER.info("Dynamo Use local: {}", dynamoDBProperties.getUseLocal());
            LOGGER.info("Dynamo Profile name: {}", dynamoDBProperties.getProfileName());
            LOGGER.info("Dynamo Region: {}", dynamoDBProperties.getRegion());
            LOGGER.info("Dynamo Table prefix: {}", dynamoDBProperties.getTablePrefix());

            LOGGER.info("SNS Topic name: {}", snsProperties.getStatusUpdateTopicName());
            LOGGER.info("SNS Message group: {}", snsProperties.getMessageGroup());
            LOGGER.info("SNS Region: {}", snsProperties.getRegion());
        }
    }

    public void execute() {
        switch (norconexProperties.getAction()) {
            case START:
                this.start();
                break;
            case CLEAN:
                this.clean();
                break;
            case CLEAN_START:
                this.doClean();
                this.start();
                break;
            default:
                LOGGER.info("Action '{}' is unrecognized", norconexProperties.getAction());
        }
    }

    public void start() {
        HttpCrawlerConfig crawlerConfig = new HttpCrawlerConfig();
        crawlerConfig.setId(norconexProperties.getName() + "Crawler");
        if (!CollectionUtils.isEmpty(norconexProperties.getStartUrls())) {
            crawlerConfig.setStartURLs(norconexProperties.getStartUrls());
        }
        if (!CollectionUtils.isEmpty(norconexProperties.getSitemapUrls())) {
            crawlerConfig.setStartSitemapURLs();
        }
        crawlerConfig.setImporterConfig(this.importerConfig);
        crawlerConfig.setUrlCrawlScopeStrategy(createCrawlerDomainSrategy());
        crawlerConfig.setMaxDepth(norconexProperties.getMaxDepth());
        crawlerConfig.setDataStoreEngine(new DynamoDataStoreEngine(dynamoDBProperties, dynamoDbClient));
        crawlerConfig.setCommitters(createOpenSearchCommitter());
        crawlerConfig.setDelayResolver(createDelayResolver());

        HttpCollectorConfig collectorConfig = new HttpCollectorConfig();
        collectorConfig.setId(norconexProperties.getName() + "Collector");
        collectorConfig.setCrawlerConfigs(crawlerConfig);

        // Make sure we stop when the run of the collector is done
        collectorConfig.addEventListeners(new CollectorLifeCycleListener() {
            @Override
            protected void onCollectorRunBegin(CollectorEvent event) {
                new StatusObject(RUNNING,
                        norconexProperties.getElasticsearchIndexName(),
                        LocalDateTime.now(), new HashMap<>()
                        );

                snsPublisher.publishToTopic("START", "START: " + norconexProperties.getElasticsearchIndexName());
            }

            @Override
            protected void onCollectorRunEnd(CollectorEvent event) {
                doPublishCrawlerStats(FINISHED, collector);
                int exit = SpringApplication.exit(context, () -> -1);
                System.exit(exit);
            }
        });

        this.collector = new HttpCollector(collectorConfig);
        this.collector.start();
    }

    @NotNull
    private GenericDelayResolver createDelayResolver() {
        GenericDelayResolver genericDelayResolver = new GenericDelayResolver();
        genericDelayResolver.setDefaultDelay(norconexProperties.getDelay().getDefaultDelay());
        return genericDelayResolver;
    }

    public void clean() {
        doClean();
        int exit = SpringApplication.exit(context, () -> -1);
        System.exit(exit);
    }

    @Scheduled(fixedDelay = 10000)
    public void logMonitoringInfo() {
        doPublishCrawlerStats(RUNNING, this.collector);
    }

    @PreDestroy
    public void destroy() {
        if (null != this.collector) {
            this.collector.stop();
        }
    }

    private void doPublishCrawlerStats(CollectorStatus status, Collector collector) {
        if (collector == null || collector.getCrawlers() == null) {
            return;
        }
        collector.getCrawlers().forEach(crawler -> {
            if (crawler.getMonitor() != null) {
                this.snsPublisher.publishToTopic(norconexProperties.getName() + "CRAWLER_STATS",
                        new StatusObject(status,
                                norconexProperties.getElasticsearchIndexName(),
                                LocalDateTime.now(),
                                crawler.getMonitor().getEventCounts()));
            }
        });
    }

    private void doClean() {
        LOGGER.warn("About to clean Everything in DynamoDB");
        DynamoDBTableUtil.cleanAllTables(dynamoDbClient, dynamoDBProperties);
    }

    @NotNull
    private OpenSearchCommitter createOpenSearchCommitter() {
        OpenSearchCommitter openSearchCommitter = new OpenSearchCommitter();
        openSearchCommitter.setNodes(norconexProperties.getElasticsearchNodes());
        openSearchCommitter.setIndexName(norconexProperties.getElasticsearchIndexName());
        return openSearchCommitter;
    }

    @NotNull
    private URLCrawlScopeStrategy createCrawlerDomainSrategy() {
        URLCrawlScopeStrategy strategy = new URLCrawlScopeStrategy();
        strategy.setStayOnDomain(true);
        return strategy;
    }
}
