package com.firstclub.membership.catalog.repository;

import com.firstclub.membership.catalog.domain.Benefit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BenefitRepository extends JpaRepository<Benefit, Long> {

    Optional<Benefit> findByCode(String code);

    List<Benefit> findByActiveTrueOrderByIdAsc();
}
