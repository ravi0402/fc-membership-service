package com.firstclub.membership.tier.activity;

/**
 * Port for reading user activity. The membership service does not own order data; in
 * production this is implemented by an adapter over the Order service / data warehouse.
 * The rule engine depends only on this interface.
 */
public interface UserActivityProvider {

    UserActivity getActivity(String userId);
}
