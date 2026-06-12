package com.firstclub.membership.catalog.repository;

import com.firstclub.membership.catalog.domain.MembershipPlan;
import com.firstclub.membership.catalog.domain.MembershipPlanPrice;
import com.firstclub.membership.catalog.domain.MembershipTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MembershipPlanPriceRepository extends JpaRepository<MembershipPlanPrice, Long> {

    Optional<MembershipPlanPrice> findByPlanAndTier(MembershipPlan plan, MembershipTier tier);

    @Query("SELECT p FROM MembershipPlanPrice p JOIN FETCH p.plan JOIN FETCH p.tier")
    List<MembershipPlanPrice> findAllWithPlanAndTier();
}
