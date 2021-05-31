package eu.luminis.aws.norconex;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@ConfigurationProperties(prefix = "norconex")
@Component
public class NorconexProperties {
    private String name;
    private List<String> startUrls;
    private List<String> sitemapUrls;
    private List<String> elasticsearchNodes;
    private String elasticsearchIndexName;
    private Integer maxDepth;
    private NorconexAction action;
    private Delay delay;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getStartUrls() {
        return startUrls;
    }

    public void setStartUrls(List<String> startUrls) {
        this.startUrls = startUrls;
    }

    public List<String> getElasticsearchNodes() {
        return elasticsearchNodes;
    }

    public void setElasticsearchNodes(List<String> elasticsearchNodes) {
        this.elasticsearchNodes = elasticsearchNodes;
    }

    public String getElasticsearchIndexName() {
        return elasticsearchIndexName;
    }

    public void setElasticsearchIndexName(String elasticsearchIndexName) {
        this.elasticsearchIndexName = elasticsearchIndexName;
    }

    public Integer getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(Integer maxDepth) {
        this.maxDepth = maxDepth;
    }

    public NorconexAction getAction() {
        return action;
    }

    public void setAction(NorconexAction action) {
        this.action = action;
    }

    public List<String> getSitemapUrls() {
        return sitemapUrls;
    }

    public void setSitemapUrls(List<String> sitemapUrls) {
        this.sitemapUrls = sitemapUrls;
    }

    public Delay getDelay() {
        return delay;
    }

    public void setDelay(Delay delay) {
        this.delay = delay;
    }

    public static class Delay {
        private long defaultDelay;
        private Boolean ignoreRobotsCrawlDelay;
        private DelayScope scope;

        public long getDefaultDelay() {
            return defaultDelay;
        }

        public void setDefaultDelay(long defaultDelay) {
            this.defaultDelay = defaultDelay;
        }

        public Boolean getIgnoreRobotsCrawlDelay() {
            return ignoreRobotsCrawlDelay;
        }

        public void setIgnoreRobotsCrawlDelay(Boolean ignoreRobotsCrawlDelay) {
            this.ignoreRobotsCrawlDelay = ignoreRobotsCrawlDelay;
        }

        public DelayScope getScope() {
            return scope;
        }

        public void setScope(DelayScope scope) {
            this.scope = scope;
        }
    }

}
