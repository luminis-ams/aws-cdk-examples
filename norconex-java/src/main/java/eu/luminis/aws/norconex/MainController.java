package eu.luminis.aws.norconex;

import eu.luminis.aws.norconex.dynamodb.DynamoDBRepository;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@RestController
public class MainController {

    private final NorconexService norconexService;
    private final DynamoDBRepository dynamoDBRepository;

    public MainController(NorconexService norconexService, DynamoDBRepository dynamoDBRepository) {
        this.norconexService = norconexService;
        this.dynamoDBRepository = dynamoDBRepository;
    }

    @GetMapping
    public ProcesInfo root() {
        return norconexService.info();
    }

    @PostMapping("/start")
    public void start() {
        norconexService.start();
    }

    @PostMapping("/start/no_links")
    public void startNoLinks() {
        norconexService.startNoLinks();
    }

    @PostMapping("/stop")
    public void stop() {
        norconexService.stop();
    }

    @PostMapping("/clean")
    public void clean() {
        norconexService.clean();
    }

    @GetMapping("/dynamodb")
    public List<String> listTables() {
        return dynamoDBRepository.listTables();
    }

}
