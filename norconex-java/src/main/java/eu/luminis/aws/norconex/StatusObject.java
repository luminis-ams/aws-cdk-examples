package eu.luminis.aws.norconex;

import java.time.LocalDateTime;
import java.util.Map;

public class StatusObject {
    private CollectorStatus status;
    private String jobName;
    private LocalDateTime timestamp;
    private Map<String, Long> stats;

    public StatusObject(CollectorStatus status, String jobName, LocalDateTime timestamp, Map<String, Long> stats) {
        this.status = status;
        this.jobName = jobName;
        this.timestamp = timestamp;
        this.stats = stats;
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

    public Map<String, Long> getStats() {
        return stats;
    }

    public void setStats(Map<String, Long> stats) {
        this.stats = stats;
    }
}
