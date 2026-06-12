package com.firstclub.membership.catalog.repository;

import com.firstclub.membership.catalog.domain.MembershipTier;
import com.firstclub.membership.catalog.domain.TierBenefit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TierBenefitRepository extends JpaRepository<TierBenefit, Long> {

    /** JOIN FETCH the benefit to avoid N+1 when resolving a tier's effective perks. */
    @Query("SELECT tb FROM TierBenefit tb JOIN FETCH tb.benefit WHERE tb.tier = :tier")
    List<TierBenefit> findByTierWithBenefit(@Param("tier") MembershipTier tier);

    @Query("SELECT tb FROM TierBenefit tb JOIN FETCH tb.benefit JOIN FETCH tb.tier")
    List<TierBenefit> findAllWithBenefitAndTier();
}
