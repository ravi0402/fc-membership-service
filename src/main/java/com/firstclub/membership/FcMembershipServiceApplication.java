package com.firstclub.membership;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the FirstClub Membership Service.
 *
 * <p>Powers subscription-based memberships: billing plans (cadence), benefit tiers,
 * configurable perks, and an activity-driven tier-eligibility engine.
 */
@SpringBootApplication
public class FcMembershipServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FcMembershipServiceApplication.class, args);
    }
}
