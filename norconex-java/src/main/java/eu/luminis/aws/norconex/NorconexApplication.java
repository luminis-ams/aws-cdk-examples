package eu.luminis.aws.norconex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication(
        exclude = {MongoAutoConfiguration.class}
)
@EnableConfigurationProperties
public class NorconexApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext run = SpringApplication.run(NorconexApplication.class, args);
        NorconexService bean = run.getBean(NorconexService.class);
        bean.afterConstruct();
        SpringApplication.exit(run, () -> 0);
    }
}
