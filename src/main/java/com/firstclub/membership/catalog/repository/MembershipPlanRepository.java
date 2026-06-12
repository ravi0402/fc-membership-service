package com.firstclub.membership.catalog.repository;

import com.firstclub.membership.catalog.domain.BillingCadence;
import com.firstclub.membership.catalog.domain.MembershipPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembershipPlanRepository extends JpaRepository<MembershipPlan, Long> {

    Optional<MembershipPlan> findByCadence(BillingCadence cadence);

    List<MembershipPlan> findByActiveTrueOrderByIdAsc();
}
