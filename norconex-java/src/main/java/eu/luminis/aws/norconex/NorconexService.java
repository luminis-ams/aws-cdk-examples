package eu.luminis.aws.norconex;

import com.norconex.collector.core.crawler.Crawler;
import com.norconex.collector.core.crawler.CrawlerConfig;
import com.norconex.collector.core.store.impl.mvstore.MVStoreDataStoreEngine;
import com.norconex.collector.http.HttpCollector;
import com.norconex.collector.http.HttpCollectorConfig;
import com.norconex.collector.http.crawler.HttpCrawlerConfig;
import com.norconex.collector.http.crawler.URLCrawlScopeStrategy;
import com.norconex.committer.core3.fs.impl.JSONFileCommitter;
import com.norconex.committer.elasticsearch.ElasticsearchCommitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;

@Service
public class NorconexService {

    private final NorconexProperties norconexProperties;

    private HttpCollector collector;

    @Autowired
    public NorconexService(NorconexProperties norconexProperties) {
        this.norconexProperties = norconexProperties;
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

    public void startNoLinks() {
        List<Crawler> crawlers = this.collector.getCrawlers();
        crawlers.forEach(crawler -> {
            CrawlerConfig crawlerConfig = crawler.getCrawlerConfig();
            if (crawlerConfig instanceof HttpCrawlerConfig) {
                ((HttpCrawlerConfig) crawlerConfig).setMaxDepth(0);
            }
        });
        this.start();
    }

    @PostConstruct
    public void afterConstruct() {
        HttpCollectorConfig collectorConfig = new HttpCollectorConfig();
        collectorConfig.setId(norconexProperties.getName() + "Collector");

        HttpCrawlerConfig crawlerConfig = new HttpCrawlerConfig();
        crawlerConfig.setId(norconexProperties.getName() + "Crawler");
        crawlerConfig.setStartURLsProviders();
        crawlerConfig.setStartURLs(norconexProperties.getStartUrls());
        URLCrawlScopeStrategy strategy = new URLCrawlScopeStrategy();
        strategy.setStayOnDomain(true);

        crawlerConfig.setUrlCrawlScopeStrategy(strategy);
        crawlerConfig.setMaxDepth(1);
        crawlerConfig.setDataStoreEngine(new MVStoreDataStoreEngine());
        ElasticsearchCommitter elasticsearchCommitter = new ElasticsearchCommitter();
        elasticsearchCommitter.setNodes(norconexProperties.getElasticsearchNodes());
        elasticsearchCommitter.setIndexName(norconexProperties.getElasticsearchIndexName());
        crawlerConfig.setCommitters(new JSONFileCommitter(), elasticsearchCommitter);

        crawlerConfig.addEventListeners(event -> {
            System.out.println("EVENT: " + event.getName() + ",  " + event.getMessage());
        });

        collectorConfig.setCrawlerConfigs(crawlerConfig);

        this.collector = new HttpCollector(collectorConfig);

//        JdbcDataStoreEngine dataStoreEngine = new JdbcDataStoreEngine();
//        dataStoreEngine.setTablePrefix("norconex");
//        Properties configProperties = new Properties();
//        configProperties.add("jdbcUrl","jdbc:mysql://localhost:3306/norconex");
//        configProperties.add("dataSource.user", "norconex");
//        configProperties.add("dataSource.password", "geheim");
//
//        dataStoreEngine.setConfigProperties(configProperties);
//        crawlerConfig.setDataStoreEngine(dataStoreEngine);
    }

    @PreDestroy
    public void destroy() {
        if (null != this.collector) {
            this.collector.stop();
        }
    }
}
