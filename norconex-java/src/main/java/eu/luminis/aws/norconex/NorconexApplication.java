package eu.luminis.aws.norconex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class NorconexApplication {
    public static void main(String[] args) {
        SpringApplication.run(NorconexApplication.class, args);
    }
}
