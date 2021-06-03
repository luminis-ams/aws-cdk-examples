package eu.luminis.aws.norconex;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(
        exclude = {MongoAutoConfiguration.class}
)
@EnableConfigurationProperties
@EnableScheduling
@ComponentScan({"eu.luminis.aws.norconex", "eu.luminis.norconex.datastore.dynamodb"})
public class NorconexApplication implements CommandLineRunner {

    private final NorconexService norconexService;

    public NorconexApplication(NorconexService norconexService) {
        this.norconexService = norconexService;
    }

    public static void main(String[] args) {
        SpringApplication.run(NorconexApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        this.norconexService.execute();
    }
}
