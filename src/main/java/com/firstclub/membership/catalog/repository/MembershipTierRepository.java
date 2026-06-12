package com.firstclub.membership.catalog.repository;

import com.firstclub.membership.catalog.domain.MembershipTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembershipTierRepository extends JpaRepository<MembershipTier, Long> {

    Optional<MembershipTier> findByCode(String code);

    List<MembershipTier> findByActiveTrueOrderByLevelAsc();
}
