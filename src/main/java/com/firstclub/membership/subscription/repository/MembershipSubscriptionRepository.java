package com.firstclub.membership.subscription.repository;

import com.firstclub.membership.subscription.domain.MembershipSubscription;
import com.firstclub.membership.subscription.domain.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MembershipSubscriptionRepository extends JpaRepository<MembershipSubscription, Long> {

    /** Current active membership for a user; JOIN FETCH plan+tier to avoid lazy issues in DTO mapping. */
    @Query("""
            SELECT s FROM MembershipSubscription s
            JOIN FETCH s.plan JOIN FETCH s.tier
            WHERE s.userId = :userId AND s.status = :status
            """)
    Optional<MembershipSubscription> findActive(@Param("userId") String userId,
                                                @Param("status") SubscriptionStatus status);

    Optional<MembershipSubscription> findByIdempotencyKey(String idempotencyKey);

    default Optional<MembershipSubscription> findActiveByUserId(String userId) {
        return findActive(userId, SubscriptionStatus.ACTIVE);
    }
}
