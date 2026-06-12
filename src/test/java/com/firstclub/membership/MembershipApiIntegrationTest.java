package com.firstclub.membership;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end API tests over the real wiring (seeded catalogue + H2). */
@SpringBootTest
@AutoConfigureMockMvc
class MembershipApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    private static String subscribeBody(String cadence, String tier) {
        return "{\"planCadence\":\"%s\",\"tierCode\":\"%s\",\"autoRenew\":true}".formatted(cadence, tier);
    }

    @Test
    void catalogue_isSeededAndExposed() throws Exception {
        mvc.perform(get("/api/v1/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(3)));
        mvc.perform(get("/api/v1/tiers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)));
        mvc.perform(get("/api/v1/benefits"))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(5))));
    }

    @Test
    void fullLifecycle_subscribeUpgradeBenefitsDowngradeCancel() throws Exception {
        String user = "lifecycle-user";
        // seed activity so the user can earn GOLD
        mvc.perform(put("/api/v1/users/{u}/activity", user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderCount\":8,\"monthlyOrderValue\":7000,\"cohorts\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendedTierCode").value("GOLD"));

        mvc.perform(post("/api/v1/users/{u}/subscription", user)
                        .contentType(MediaType.APPLICATION_JSON).content(subscribeBody("MONTHLY", "SILVER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.tierCode").value("SILVER"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mvc.perform(get("/api/v1/users/{u}/subscription", user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planCadence").value("MONTHLY"));

        mvc.perform(post("/api/v1/users/{u}/subscription/upgrade", user)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"targetTierCode\":\"GOLD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tierCode").value("GOLD"));

        // Gold unlocks a 10% discount (override) + exclusive deals
        mvc.perform(get("/api/v1/users/{u}/benefits", user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.code=='EXTRA_DISCOUNT')].value", hasItem("10")));

        mvc.perform(post("/api/v1/users/{u}/subscription/downgrade", user)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"targetTierCode\":\"SILVER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tierCode").value("SILVER"));

        mvc.perform(post("/api/v1/users/{u}/subscription/cancel", user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        // history records every transition
        mvc.perform(get("/api/v1/users/{u}/subscription/history", user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(4)));
    }

    @Test
    void subscribe_twiceWhileActive_conflicts() throws Exception {
        String user = "dup-user";
        mvc.perform(post("/api/v1/users/{u}/subscription", user)
                        .contentType(MediaType.APPLICATION_JSON).content(subscribeBody("MONTHLY", "SILVER")))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/users/{u}/subscription", user)
                        .contentType(MediaType.APPLICATION_JSON).content(subscribeBody("YEARLY", "SILVER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void subscribe_toGatedTier_withoutActivity_conflicts() throws Exception {
        mvc.perform(post("/api/v1/users/{u}/subscription", "fresh-user")
                        .contentType(MediaType.APPLICATION_JSON).content(subscribeBody("MONTHLY", "GOLD")))
                .andExpect(status().isConflict());
    }

    @Test
    void subscribe_withInvalidBody_returnsBadRequest() throws Exception {
        mvc.perform(post("/api/v1/users/{u}/subscription", "bad-user")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"tierCode\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void currentSubscription_whenNone_returnsNotFound() throws Exception {
        mvc.perform(get("/api/v1/users/{u}/subscription", "ghost-user"))
                .andExpect(status().isNotFound());
    }

    @Test
    void idempotentSubscribe_sameKey_returnsSameMembership() throws Exception {
        String user = "idem-user";
        mvc.perform(post("/api/v1/users/{u}/subscription", user)
                        .header("Idempotency-Key", "abc-123")
                        .contentType(MediaType.APPLICATION_JSON).content(subscribeBody("MONTHLY", "SILVER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.tierCode").value("SILVER"));
        // replay with the same key must not create a second membership nor conflict
        mvc.perform(post("/api/v1/users/{u}/subscription", user)
                        .header("Idempotency-Key", "abc-123")
                        .contentType(MediaType.APPLICATION_JSON).content(subscribeBody("MONTHLY", "SILVER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.tierCode").value("SILVER"));
    }
}
