package com.firstclub.membership.subscription.repository;

import com.firstclub.membership.subscription.domain.SubscriptionEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionEventRepository extends JpaRepository<SubscriptionEvent, Long> {

    List<SubscriptionEvent> findByUserIdOrderByOccurredAtDescIdDesc(String userId);
}
