package com.loopers.domain.rank;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "ranking.weights")
public class RankWeightProperties {

    private String defaultVersion;
    private List<RankWeightVersion> versions;

    public RankWeightVersion getDefaultWeightVersion() {
        return versions.stream()
                .filter(v -> v.versionKey().equals(defaultVersion))
                .findFirst()
                .orElseGet(() -> versions.get(0));
    }
}
