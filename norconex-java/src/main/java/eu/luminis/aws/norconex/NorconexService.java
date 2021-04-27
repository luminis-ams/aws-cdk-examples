package eu.luminis.aws.norconex;

import com.norconex.collector.core.crawler.Crawler;
import com.norconex.collector.core.crawler.CrawlerConfig;
import com.norconex.collector.core.store.impl.jdbc.JdbcDataStoreEngine;
import com.norconex.collector.http.HttpCollector;
import com.norconex.collector.http.HttpCollectorConfig;
import com.norconex.collector.http.crawler.HttpCrawlerConfig;
import com.norconex.collector.http.crawler.URLCrawlScopeStrategy;
import com.norconex.committer.core3.fs.impl.JSONFileCommitter;
import com.norconex.committer.elasticsearch.ElasticsearchCommitter;
import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.map.PropertySetter;
import com.norconex.importer.ImporterConfig;
import com.norconex.importer.handler.tagger.impl.DOMTagger;
import eu.luminis.aws.norconex.dynamodb.DynamoDBProperties;
import eu.luminis.aws.norconex.dynamodb.DynamoDataStoreEngine;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Arrays;
import java.util.List;

import static eu.luminis.aws.norconex.NorconexAction.CLEAN;
import static eu.luminis.aws.norconex.NorconexAction.START;

@Service
public class NorconexService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NorconexService.class);
    private final NorconexProperties norconexProperties;
    private final DynamoDBProperties dynamoDBProperties;

    private HttpCollector collector;

    @Autowired
    public NorconexService(NorconexProperties norconexProperties, DynamoDBProperties dynamoDBProperties) {
        this.norconexProperties = norconexProperties;
        this.dynamoDBProperties = dynamoDBProperties;
    }

    public void start() {
        if (null != this.collector) {
            this.collector.start();
        }
    }

    public ProcesInfo info() {
        ProcesInfo procesInfo = new ProcesInfo();
        if (null != this.collector) {
            procesInfo.setRunning(this.collector.isRunning());
            procesInfo.setWorkPath(this.collector.getWorkDir().toString());
            procesInfo.setTempPath(this.collector.getTempDir().toString());
        }
        return procesInfo;
    }

    public void stop() {
        if (null != this.collector) {
            this.collector.stop();
        }
    }

    public void clean() {
        if (null != this.collector) {
            this.collector.clean();
        }

    }

    @PostConstruct
    public void afterConstruct() {

        HttpCrawlerConfig crawlerConfig = new HttpCrawlerConfig();
        crawlerConfig.setId(norconexProperties.getName() + "Crawler");
        crawlerConfig.setStartURLsProviders();
        crawlerConfig.setStartURLs(norconexProperties.getStartUrls());
        crawlerConfig.setImporterConfig(createImporterConfiguration());
        crawlerConfig.setUrlCrawlScopeStrategy(createCrawlerDomainSrategy());
        crawlerConfig.setMaxDepth(norconexProperties.getMaxDepth());
        crawlerConfig.setDataStoreEngine(new DynamoDataStoreEngine(dynamoDBProperties));
        crawlerConfig.setCommitters(createElasticsearchCommitter());

        HttpCollectorConfig collectorConfig = new HttpCollectorConfig();
        collectorConfig.setId(norconexProperties.getName() + "Collector");
        collectorConfig.setCrawlerConfigs(crawlerConfig);

        this.collector = new HttpCollector(collectorConfig);

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

    @PreDestroy
    public void destroy() {
        if (null != this.collector) {
            this.collector.stop();
        }
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
