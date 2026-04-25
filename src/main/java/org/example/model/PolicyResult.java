package org.example.model;

import java.util.List;

public record PolicyResult(
        String eventId,
        boolean humanReviewRequired,
        boolean autoDismissAllowed,
        List<String> appliedPolicies
) {
}
