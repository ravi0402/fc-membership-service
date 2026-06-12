package com.firstclub.membership.subscription;

import com.firstclub.membership.catalog.domain.BillingCadence;
import com.firstclub.membership.subscription.dto.SubscribeRequest;
import com.firstclub.membership.subscription.repository.MembershipSubscriptionRepository;
import com.firstclub.membership.subscription.service.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the "at most one active subscription per user" guarantee holds under a race:
 * many threads subscribe the same user simultaneously; exactly one must win.
 */
@SpringBootTest
class SubscriptionConcurrencyIntegrationTest {

    @Autowired
    private SubscriptionService subscriptionService;
    @Autowired
    private MembershipSubscriptionRepository subscriptionRepository;

    @Test
    void concurrentSubscribe_sameUser_onlyOneSucceeds() throws Exception {
        String user = "race-user";
        int threads = 8;
        var pool = Executors.newFixedThreadPool(threads);
        var startGate = new CountDownLatch(1);
        var request = new SubscribeRequest(BillingCadence.MONTHLY, "SILVER", true);

        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            tasks.add(() -> {
                startGate.await();
                try {
                    subscriptionService.subscribe(user, request, null);
                    return true; // won the race
                } catch (RuntimeException e) {
                    return false; // lost: duplicate-active or constraint conflict
                }
            });
        }

        List<Future<Boolean>> futures = new ArrayList<>();
        for (Callable<Boolean> task : tasks) {
            futures.add(pool.submit(task));
        }
        startGate.countDown(); // release all threads at once

        long winners = 0;
        for (Future<Boolean> f : futures) {
            if (f.get()) {
                winners++;
            }
        }
        pool.shutdown();

        assertThat(winners).isEqualTo(1);
        assertThat(subscriptionRepository.findActiveByUserId(user)).isPresent();
    }
}
