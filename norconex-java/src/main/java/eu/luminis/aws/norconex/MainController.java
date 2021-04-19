package eu.luminis.aws.norconex;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MainController {

    private final NorconexService norconexService;

    public MainController(NorconexService norconexService) {
        this.norconexService = norconexService;
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
}
