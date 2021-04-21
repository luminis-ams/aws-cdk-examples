package eu.luminis.aws.norconex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(
        exclude = {MongoAutoConfiguration.class}
)
@EnableConfigurationProperties
public class NorconexApplication {
    public static void main(String[] args) {
        SpringApplication.run(NorconexApplication.class, args);
    }
}
