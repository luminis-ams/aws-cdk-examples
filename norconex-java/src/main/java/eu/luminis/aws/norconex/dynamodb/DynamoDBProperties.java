package eu.luminis.aws.norconex.dynamodb;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("aws")
public class DynamoDBProperties {
    private String region;
    private Boolean useLocal;
    private String localUri;
    private String profileName;
    private String tablePrefix;

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Boolean getUseLocal() {
        return useLocal;
    }

    public void setUseLocal(Boolean useLocal) {
        this.useLocal = useLocal;
    }

    public String getLocalUri() {
        return localUri;
    }

    public void setLocalUri(String localUri) {
        this.localUri = localUri;
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public String getTablePrefix() {
        return tablePrefix;
    }

    public void setTablePrefix(String tablePrefix) {
        this.tablePrefix = tablePrefix;
    }
}
