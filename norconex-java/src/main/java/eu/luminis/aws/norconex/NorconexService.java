package eu.luminis.aws.norconex;

import com.norconex.collector.core.CollectorEvent;
import com.norconex.collector.core.CollectorLifeCycleListener;
import com.norconex.collector.core.crawler.CrawlerEvent;
import com.norconex.collector.core.crawler.CrawlerLifeCycleListener;
import com.norconex.collector.core.monitor.CrawlerMonitor;
import com.norconex.collector.http.HttpCollector;
import com.norconex.collector.http.HttpCollectorConfig;
import com.norconex.collector.http.crawler.HttpCrawlerConfig;
import com.norconex.collector.http.crawler.URLCrawlScopeStrategy;
import com.norconex.collector.http.delay.impl.GenericDelayResolver;
import com.norconex.commons.lang.map.PropertySetter;
import com.norconex.importer.ImporterConfig;
import com.norconex.importer.handler.tagger.impl.DOMTagger;
import eu.luminis.committer.opensearch.OpenSearchCommitter;
import eu.luminis.norconex.datastore.dynamodb.DynamoDBProperties;
import eu.luminis.norconex.datastore.dynamodb.DynamoDBRepository;
import eu.luminis.norconex.datastore.dynamodb.DynamoDBTableUtil;
import eu.luminis.norconex.datastore.dynamodb.DynamoDataStoreEngine;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sns.SnsClient;

import javax.annotation.PreDestroy;
import java.util.Arrays;

@Service
public class NorconexService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NorconexService.class);
    private final NorconexProperties norconexProperties;
    private final DynamoDBProperties dynamoDBProperties;
    private final DynamoDBRepository dynamoDBRepository;
    private final DynamoDbClient dynamoDbClient;
    private final SnsProperties snsProperties;
    private final SnsClient snsClient;

    private HttpCollector collector;

    private ApplicationContext context;

    @Autowired
    public NorconexService(NorconexProperties norconexProperties,
                           DynamoDBProperties dynamoDBProperties,
                           DynamoDBRepository dynamoDBRepository,
                           DynamoDbClient dynamoDbClient,
                           SnsProperties snsProperties,
                           SnsClient snsClient,
                           ApplicationContext context) {
        this.norconexProperties = norconexProperties;
        this.dynamoDBProperties = dynamoDBProperties;
        this.dynamoDBRepository = dynamoDBRepository;
        this.snsProperties = snsProperties;
        this.dynamoDbClient = dynamoDbClient;
        this.snsClient = snsClient;
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
        crawlerConfig.setImporterConfig(createImporterConfiguration());
        crawlerConfig.setUrlCrawlScopeStrategy(createCrawlerDomainSrategy());
        crawlerConfig.setMaxDepth(norconexProperties.getMaxDepth());
        crawlerConfig.setDataStoreEngine(new DynamoDataStoreEngine(dynamoDBProperties, dynamoDbClient));
        crawlerConfig.setCommitters(createOpenSearchCommitter());
        crawlerConfig.setDelayResolver(createdelayResolver());

        HttpCollectorConfig collectorConfig = new HttpCollectorConfig();
        collectorConfig.setId(norconexProperties.getName() + "Collector");
        collectorConfig.setCrawlerConfigs(crawlerConfig);

        String statusUpdateTopicName = this.snsProperties.getStatusUpdateTopicName();
        if (StringUtils.hasLength(statusUpdateTopicName)) {
            LOGGER.info("Use SNS Topic {} to send lifecycle events", statusUpdateTopicName);
            collectorConfig.addEventListeners(createCrawlerSNSNotificationListener(this.snsProperties));
        } else {
            LOGGER.info("No SNS Topic is specified");
        }

        collectorConfig.addEventListeners(createCrawlerLifeCycleListener());

        // Make sure we stop when the run of the collector is done
        collectorConfig.addEventListeners(new CollectorLifeCycleListener() {
            @Override
            protected void onCollectorRunEnd(CollectorEvent event) {
                int exit = SpringApplication.exit(context, () -> -1);
                System.exit(exit);
            }
        });

        this.collector = new HttpCollector(collectorConfig);
        this.collector.start();
    }

    @NotNull
    private GenericDelayResolver createdelayResolver() {
        GenericDelayResolver genericDelayResolver = new GenericDelayResolver();
        genericDelayResolver.setDefaultDelay(2000);
        return genericDelayResolver;
    }

    public void clean() {
        doClean();
        int exit = SpringApplication.exit(context, () -> -1);
        System.exit(exit);
    }

    @PreDestroy
    public void destroy() {
        if (null != this.collector) {
            this.collector.stop();
        }
    }

    private void doClean() {
        LOGGER.warn("About to clean Everything in DynamoDB");
        DynamoDBTableUtil.cleanAllTables(dynamoDbClient, dynamoDBProperties);
    }

    @NotNull
    private CrawlerLifeCycleListener createCrawlerLifeCycleListener() {
        return new CrawlerLifeCycleListener() {

            @Override
            protected void onCrawlerShutdown(CrawlerEvent event) {
                CrawlerMonitor monitor = event.getSource().getMonitor();
                dynamoDBRepository.storeCrawlerStats(norconexProperties.getName(), monitor.getEventCounts());
            }
        };
    }

    private CrawlerLifeCycleListener createCrawlerSNSNotificationListener(SnsProperties snsProperties) {
        return new SnsTopicCrawlerLifeCycleListener(snsClient, snsProperties, norconexProperties.getElasticsearchIndexName());
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

    @NotNull
    private ImporterConfig createImporterConfiguration() {
        ImporterConfig importerConfig = new ImporterConfig();
        DOMTagger domTagger = new DOMTagger();
        domTagger.addDOMExtractDetails(new DOMTagger.DOMExtractDetails("div#content", "content_2", PropertySetter.REPLACE));
        importerConfig.setPreParseHandlers(Arrays.asList(domTagger));
        return importerConfig;
    }
}
