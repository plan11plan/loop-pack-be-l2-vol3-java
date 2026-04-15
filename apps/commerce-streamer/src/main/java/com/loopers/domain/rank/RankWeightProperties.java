package com.loopers.domain.rank;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "ranking.weights")
public class RankWeightProperties {

    private List<RankWeightVersion> versions;
    private String defaultVersion;

    public RankWeightVersion getVersion(String key) {
        return versions.stream()
                .filter(v -> v.versionKey().equals(key))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "알 수 없는 가중치 버전: " + key));
    }
}
