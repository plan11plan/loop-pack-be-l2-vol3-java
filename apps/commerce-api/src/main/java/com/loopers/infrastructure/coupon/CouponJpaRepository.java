package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.CouponModel;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface CouponJpaRepository extends JpaRepository<CouponModel, Long> {

    Optional<CouponModel> findByIdAndDeletedAtIsNull(Long id);

    Page<CouponModel> findAllByDeletedAtIsNull(Pageable pageable);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE CouponModel c"
            + " SET c.issuedQuantity = c.issuedQuantity + 1"
            + " WHERE c.id = :id AND c.issuedQuantity < c.totalQuantity")
    int incrementIssuedQuantity(@Param("id") Long id);
}
