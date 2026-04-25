package org.example.model;

import java.util.List;

/**
 * Stores the policy check result for one incident.
 */
public record PolicyResult(
        String eventId,
        boolean humanReviewRequired,
        boolean autoDismissAllowed,
        List<String> appliedPolicies
) {
}
