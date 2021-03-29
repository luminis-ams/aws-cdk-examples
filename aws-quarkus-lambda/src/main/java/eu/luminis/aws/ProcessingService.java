package eu.luminis.aws;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProcessingService {

    public String process(String greeting, String name) {
        return greeting + " " + name;
    }
}
