package eu.luminis.aws.norconex;

import java.time.LocalDateTime;
import java.util.Map;

public class StatusObject {
    private CollectorStatus status;
    private String jobName;
    private LocalDateTime timestamp;
    private Stats stats;

    public StatusObject(CollectorStatus status, String jobName, LocalDateTime timestamp, Map<String, Long> stats) {
        this.status = status;
        this.jobName = jobName;
        this.timestamp = timestamp;
        this.stats = statsBuilder(stats);
    }

    public CollectorStatus getStatus() {
        return status;
    }

    public void setStatus(CollectorStatus status) {
        this.status = status;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Stats getStats() {
        return stats;
    }

    public StatusObject setStats(Stats stats) {
        this.stats = stats;
        return this;
    }

    static Stats statsBuilder(Map<String, Long> stats) {
        return new Stats()
                .setCommitted(stats.getOrDefault("DOCUMENT_COMMITTED_UPSERT", 0L))
                .setFetched(stats.getOrDefault("DOCUMENT_FETCHED", 0L))
                .setImported(stats.getOrDefault("DOCUMENT_IMPORTED", 0L))
                .setProcessed(stats.getOrDefault("DOCUMENT_PROCESSED", 0L))
                .setQueued(stats.getOrDefault("DOCUMENT_QUEUED", 0L))
                .setRejectedNotFound(stats.getOrDefault("REJECTED_NOTFOUND", 0L))
                .setRejectedNonCanonical(stats.getOrDefault("REJECTED_NONCANONICAL", 0L))
                .setUrlsExtracted(stats.getOrDefault("URLS_EXTRACTED", 0L));
    }

    public static class Stats {
        private long committed;
        private long fetched;
        private long imported;
        private long processed;
        private long queued;
        private long rejectedNonCanonical;
        private long rejectedNotFound;
        private long urlsExtracted;

        public long getCommitted() {
            return committed;
        }

        public Stats setCommitted(long committed) {
            this.committed = committed;
            return this;
        }

        public long getFetched() {
            return fetched;
        }

        public Stats setFetched(long fetched) {
            this.fetched = fetched;
            return this;
        }

        public long getImported() {
            return imported;
        }

        public Stats setImported(long imported) {
            this.imported = imported;
            return this;
        }

        public long getProcessed() {
            return processed;
        }

        public Stats setProcessed(long processed) {
            this.processed = processed;
            return this;
        }

        public long getQueued() {
            return queued;
        }

        public Stats setQueued(long queued) {
            this.queued = queued;
            return this;
        }

        public long getRejectedNonCanonical() {
            return rejectedNonCanonical;
        }

        public Stats setRejectedNonCanonical(long rejectedNonCanonical) {
            this.rejectedNonCanonical = rejectedNonCanonical;
            return this;
        }

        public long getRejectedNotFound() {
            return rejectedNotFound;
        }

        public Stats setRejectedNotFound(long rejectedNotFound) {
            this.rejectedNotFound = rejectedNotFound;
            return this;
        }

        public long getUrlsExtracted() {
            return urlsExtracted;
        }

        public Stats setUrlsExtracted(long urlsExtracted) {
            this.urlsExtracted = urlsExtracted;
            return this;
        }
    }
}
