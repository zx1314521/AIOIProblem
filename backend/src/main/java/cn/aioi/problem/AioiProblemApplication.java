package cn.aioi.problem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AioiProblemApplication {
    public static void main(String[] args) {
        SpringApplication.run(AioiProblemApplication.class, args);
    }
}

