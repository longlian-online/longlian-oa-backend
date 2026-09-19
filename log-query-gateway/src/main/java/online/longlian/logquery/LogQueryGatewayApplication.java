package online.longlian.logquery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LogQueryGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogQueryGatewayApplication.class, args);
    }
}
