package com.loopers.batch.job.ranking.reader;

import com.loopers.domain.metrics.ProductMetricsDailyEntity;
import com.loopers.domain.metrics.QProductMetricsDailyEntity;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.database.AbstractPagingItemReader;

public class QueryDslZeroOffsetItemReader
        extends AbstractPagingItemReader<ProductMetricsDailyEntity> {

    private static final QProductMetricsDailyEntity METRIC =
            QProductMetricsDailyEntity.productMetricsDailyEntity;
    private static final String LAST_ID_CTX_KEY = "lastId";

    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;
    private final LocalDate fromDate;
    private final LocalDate toDate;
    private long lastId = 0L;

    public QueryDslZeroOffsetItemReader(
            JPAQueryFactory queryFactory,
            EntityManager entityManager,
            int pageSize) {
        this(queryFactory, entityManager, pageSize, null, null);
    }

    public QueryDslZeroOffsetItemReader(
            JPAQueryFactory queryFactory,
            EntityManager entityManager,
            int pageSize,
            LocalDate fromDate,
            LocalDate toDate) {
        this.queryFactory = queryFactory;
        this.entityManager = entityManager;
        this.fromDate = fromDate;
        this.toDate = toDate;
        setPageSize(pageSize);
        setName("queryDslZeroOffsetItemReader");
        setSaveState(true);
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        super.open(executionContext);
        String key = getExecutionContextKey(LAST_ID_CTX_KEY);
        if (executionContext.containsKey(key)) {
            this.lastId = executionContext.getLong(key);
        }
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        super.update(executionContext);
        executionContext.putLong(getExecutionContextKey(LAST_ID_CTX_KEY), lastId);
    }

    @Override
    protected void doReadPage() {
        if (results == null) {
            results = new CopyOnWriteArrayList<>();
        } else {
            results.clear();
        }
        List<ProductMetricsDailyEntity> page = queryFactory
                .selectFrom(METRIC)
                .where(buildWhere())
                .orderBy(METRIC.id.asc())
                .limit(getPageSize())
                .fetch();
        entityManager.clear();
        results.addAll(page);
        if (!page.isEmpty()) {
            lastId = page.get(page.size() - 1).getId();
        }
    }

    private BooleanExpression buildWhere() {
        BooleanExpression cond = METRIC.id.gt(lastId);
        if (fromDate != null && toDate != null) {
            cond = cond.and(METRIC.metricDate.between(fromDate, toDate));
        }
        return cond;
    }
}
