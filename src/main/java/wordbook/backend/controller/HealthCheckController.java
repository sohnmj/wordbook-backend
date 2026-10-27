package wordbook.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {
    @Value("${server.env:local}")
    private String env;

    @GetMapping("/healthcheck/env")
    public String env() {
        return env;
    }
}
