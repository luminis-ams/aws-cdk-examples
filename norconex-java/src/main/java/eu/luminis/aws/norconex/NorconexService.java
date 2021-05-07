package eu.luminis.aws.norconex;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.norconex.collector.core.CollectorEvent;
import com.norconex.collector.core.CollectorLifeCycleListener;
import com.norconex.collector.core.crawler.CrawlerEvent;
import com.norconex.collector.core.crawler.CrawlerLifeCycleListener;
import com.norconex.collector.core.monitor.CrawlerMonitor;
import com.norconex.collector.http.HttpCollector;
import com.norconex.collector.http.HttpCollectorConfig;
import com.norconex.collector.http.crawler.HttpCrawlerConfig;
import com.norconex.collector.http.crawler.URLCrawlScopeStrategy;
import com.norconex.committer.elasticsearch.ElasticsearchCommitter;
import com.norconex.commons.lang.map.PropertySetter;
import com.norconex.importer.ImporterConfig;
import com.norconex.importer.handler.tagger.impl.DOMTagger;
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

import javax.annotation.PreDestroy;
import java.util.Arrays;

@Service
public class NorconexService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NorconexService.class);
    private final NorconexProperties norconexProperties;
    private final DynamoDBProperties dynamoDBProperties;
    private final DynamoDBRepository dynamoDBRepository;
    private final AmazonDynamoDB client;

    private HttpCollector collector;

    private ApplicationContext context;

    @Autowired
    public NorconexService(NorconexProperties norconexProperties,
                           DynamoDBProperties dynamoDBProperties,
                           DynamoDBRepository dynamoDBRepository,
                           AmazonDynamoDB client,
                           ApplicationContext context) {
        this.norconexProperties = norconexProperties;
        this.dynamoDBProperties = dynamoDBProperties;
        this.dynamoDBRepository = dynamoDBRepository;
        this.client = client;
        this.context = context;

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Norconex action: {}", norconexProperties.getAction());
            LOGGER.info("Norconex Elasticsearch Index Name : {}", norconexProperties.getElasticsearchIndexName());
            LOGGER.info("Norconex Elasticsearch Nodes: {}", norconexProperties.getElasticsearchNodes());
            LOGGER.info("Norconex Max depth: {}", norconexProperties.getMaxDepth());
            LOGGER.info("Norconex Start Urls: {}", norconexProperties.getStartUrls());

            LOGGER.info("Local URI: {}", dynamoDBProperties.getLocalUri());
            LOGGER.info("Use local: {}", dynamoDBProperties.getUseLocal());
            LOGGER.info("Profile name: {}", dynamoDBProperties.getProfileName());
            LOGGER.info("Region: {}", dynamoDBProperties.getRegion());
            LOGGER.info("Table prefix: {}", dynamoDBProperties.getTablePrefix());
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
            default:
                LOGGER.info("Action '{}' is unrecognized", norconexProperties.getAction());
        }
    }

    public void start() {
        HttpCrawlerConfig crawlerConfig = new HttpCrawlerConfig();
        crawlerConfig.setId(norconexProperties.getName() + "Crawler");
        crawlerConfig.setStartURLs(norconexProperties.getStartUrls());
        crawlerConfig.setImporterConfig(createImporterConfiguration());
        crawlerConfig.setUrlCrawlScopeStrategy(createCrawlerDomainSrategy());
        crawlerConfig.setMaxDepth(norconexProperties.getMaxDepth());
        crawlerConfig.setDataStoreEngine(new DynamoDataStoreEngine(dynamoDBProperties, client));
        crawlerConfig.setCommitters(createElasticsearchCommitter());

        HttpCollectorConfig collectorConfig = new HttpCollectorConfig();
        collectorConfig.setId(norconexProperties.getName() + "Collector");
        collectorConfig.setCrawlerConfigs(crawlerConfig);

        collectorConfig.addEventListeners(createCrawlerLifeCycleListener());
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

    public void clean() {
        DynamoDBTableUtil.cleanAllTables(new DynamoDB(client),dynamoDBProperties);
        int exit = SpringApplication.exit(context, () -> -1);
        System.exit(exit);
    }

    @PreDestroy
    public void destroy() {
        if (null != this.collector) {
            this.collector.stop();
        }
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

    @NotNull
    private ElasticsearchCommitter createElasticsearchCommitter() {
        ElasticsearchCommitter elasticsearchCommitter = new ElasticsearchCommitter();
        elasticsearchCommitter.setNodes(norconexProperties.getElasticsearchNodes());
        elasticsearchCommitter.setIndexName(norconexProperties.getElasticsearchIndexName());
        return elasticsearchCommitter;
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
