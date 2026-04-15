package com.loopers.domain.rank;

import static org.assertj.core.api.Assertions.assertThat;

import com.loopers.domain.rank.dto.RankInfo;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

class RankServiceTest {

    private RankService rankService;
    private FakeRankRepository fakeRepository;

    private static final LocalDate TODAY = LocalDate.of(2026, 4, 5);

    @BeforeEach
    void setUp() {
        fakeRepository = new FakeRankRepository();
        rankService = new RankService(fakeRepository);
    }

    @DisplayName("순위가 포함된 랭킹을 조회할 때, ")
    @Nested
    class GetTopRankedByDate {

        @DisplayName("동점 상품은 같은 순위를 갖는다.")
        @Test
        void getTopRankedByDate_whenTiedScores_thenSameRank() {
            // arrange
            fakeRepository.save(RankModel.create(1L, TODAY, 7000.0));
            fakeRepository.save(RankModel.create(2L, TODAY, 7000.0));
            fakeRepository.save(RankModel.create(3L, TODAY, 0.1));

            // act
            List<RankInfo.RankedScore> ranked = rankService
                    .getTopRankedByDate(TODAY, PageRequest.of(0, 10));

            // assert
            assertThat(ranked).hasSize(3);
            assertThat(ranked.get(0).rank()).isEqualTo(1);
            assertThat(ranked.get(1).rank()).isEqualTo(1);
            assertThat(ranked.get(2).rank()).isEqualTo(3);
        }
    }
}
