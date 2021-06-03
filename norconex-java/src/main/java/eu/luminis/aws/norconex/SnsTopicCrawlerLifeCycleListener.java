package eu.luminis.aws.norconex;

import com.norconex.collector.core.crawler.CrawlerEvent;
import com.norconex.collector.core.crawler.CrawlerLifeCycleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SnsTopicCrawlerLifeCycleListener extends CrawlerLifeCycleListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(SnsTopicCrawlerLifeCycleListener.class);

    private final SnsPublisher snsPublisher;

    public SnsTopicCrawlerLifeCycleListener(SnsPublisher snsPublisher) {
        super();
        this.snsPublisher = snsPublisher;
    }

    @Override
    protected void onCrawlerShutdown(CrawlerEvent event) {
        snsPublisher.publishToTopic("CRAWLER_SHUTDOWN", event.getMessage());
    }

    @Override
    protected void onCrawlerInitBegin(CrawlerEvent event) {
        snsPublisher.publishToTopic("CRAWLER_INIT_BEGIN", event.getMessage());
    }

    @Override
    protected void onCrawlerInitEnd(CrawlerEvent event) {
        snsPublisher.publishToTopic("CRAWLER_INIT_END", event.getMessage());
    }

    @Override
    protected void onCrawlerRunBegin(CrawlerEvent event) {
        snsPublisher.publishToTopic("CRAWLER_RUN_BEGIN", event.getMessage());
    }

    @Override
    protected void onCrawlerRunEnd(CrawlerEvent event) {
        snsPublisher.publishToTopic("CRAWLER_RUN_END", event.getMessage());
    }

    @Override
    protected void onCrawlerStopBegin(CrawlerEvent event) {
        snsPublisher.publishToTopic("CRAWLER_STOP_BEGIN", event.getMessage());
    }

    @Override
    protected void onCrawlerStopEnd(CrawlerEvent event) {
        snsPublisher.publishToTopic("CRAWLER_STOP_END", event.getMessage());
    }

    @Override
    protected void onCrawlerCleanBegin(CrawlerEvent event) {
        snsPublisher.publishToTopic("CRAWLER_CLEAN_BEGIN", event.getMessage());
    }

    @Override
    protected void onCrawlerCleanEnd(CrawlerEvent event) {
        snsPublisher.publishToTopic("CRAWLER_CLEAN_END", event.getMessage());
    }
}
