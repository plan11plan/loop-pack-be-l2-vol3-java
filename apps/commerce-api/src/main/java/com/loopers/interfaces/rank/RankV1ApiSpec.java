package com.loopers.interfaces.rank;

import com.loopers.domain.rank.RankPeriod;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.rank.dto.RankV1Dto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;

@Tag(name = "Ranking V1 API", description = "상품 랭킹 API 입니다.")
public interface RankV1ApiSpec {

    @Operation(
            summary = "인기상품 랭킹 조회",
            description = "날짜 및 기간 단위별 인기상품 랭킹을 페이지 단위로 조회합니다.")
    ApiResponse<RankV1Dto.ListResponse> getRankings(
            @Parameter(description = "가중치 버전 (기본값: v1, DAILY에만 적용)")
            String version,
            @Parameter(description = "조회 날짜 (yyyyMMdd, 미입력 시 오늘)")
            LocalDate date,
            @Parameter(description = "기간 단위 (DAILY, WEEKLY, MONTHLY, 기본값: DAILY)")
            RankPeriod period,
            @Parameter(description = "페이지 번호 (1부터 시작)")
            int page,
            @Parameter(description = "페이지당 항목 수")
            int size);
}
