package com.loopers.interfaces.rank;

import com.loopers.application.rank.RankFacade;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.rank.dto.RankV1Dto;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rankings")
@RequiredArgsConstructor
public class RankV1Controller implements RankV1ApiSpec {

    private final RankFacade rankFacade;

    @Override
    @GetMapping
    public ApiResponse<RankV1Dto.ListResponse> getRankings(
            @RequestParam(defaultValue = "v1") String version,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyyMMdd") LocalDate date,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(RankV1Dto.ListResponse.from(
                rankFacade.getTopRankings(
                        version,
                        date != null ? date : LocalDate.now(),
                        PageRequest.of(page - 1, size))));
    }
}
