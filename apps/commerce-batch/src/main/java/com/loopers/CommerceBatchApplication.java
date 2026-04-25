package com.loopers;

import com.loopers.domain.rank.RankingScorePolicy;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@ConfigurationPropertiesScan(basePackages = {"com.loopers.batch", "com.loopers.domain.rank"})
@EnableScheduling
@Import(RankingScorePolicy.class)
@SpringBootApplication(scanBasePackages = {
        "com.loopers.batch",
        "com.loopers.config"
})
public class CommerceBatchApplication {

    @PostConstruct
    public void started() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(CommerceBatchApplication.class, args);
        String webType = ctx.getEnvironment().getProperty("spring.main.web-application-type", "none");
        if ("none".equalsIgnoreCase(webType)) {
            int exitCode = SpringApplication.exit(ctx);
            System.exit(exitCode);
        }
    }
}
