package com.firstclub.membership.catalog.repository;

import com.firstclub.membership.catalog.domain.MembershipTier;
import com.firstclub.membership.catalog.domain.TierCriterion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TierCriterionRepository extends JpaRepository<TierCriterion, Long> {

    List<TierCriterion> findByTier(MembershipTier tier);
}
