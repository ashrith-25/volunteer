package org.sfa.volunteer.assigned;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/** Domain logic independent of the final database adapter. */
public final class AssignedVolunteersService {
    private final RequestExistsLookup requests;
    private final CurrentAssignmentsLookup assignments;
    private final VolunteerProfilesLookup profiles;

    public AssignedVolunteersService(RequestExistsLookup requests,
                                     CurrentAssignmentsLookup assignments,
                                     VolunteerProfilesLookup profiles) {
        this.requests = Objects.requireNonNull(requests);
        this.assignments = Objects.requireNonNull(assignments);
        this.profiles = Objects.requireNonNull(profiles);
    }

    public AssignedVolunteersResponse findAssigned(String reqId) {
        // Step 1: Fail fast on missing or invalid payload data.
        if (reqId == null || reqId.isBlank()) {
            throw new IllegalArgumentException("req_id is required");
        }
        String normalizedId = reqId.trim();

        // Step 2: Distinguish missing requests from valid requests with zero assignments.
        if (!requests.exists(normalizedId)) {
            throw new NoSuchElementException("request not found: " + normalizedId);
        }

        // Step 3: Fetch *current* assignment IDs, as determined by the canonical adapter.
        List<String> userIds = assignments.currentVolunteerUserIds(normalizedId);
        if (userIds == null || userIds.isEmpty()) {
            return new AssignedVolunteersResponse(normalizedId, List.of());
        }
        Set<String> uniqueIds = new LinkedHashSet<>();
        for (String userId : userIds) {
            if (userId != null && !userId.isBlank()) uniqueIds.add(userId);
        }
        if (uniqueIds.isEmpty()) return new AssignedVolunteersResponse(normalizedId, List.of());

        // Step 4: Single bulk profile lookup; missing profile fields are allowed to be null.
        Map<String, AssignedVolunteer> found = profiles.findByUserIds(uniqueIds);
        if (found == null) found = Map.of();
        List<AssignedVolunteer> result = new ArrayList<>(uniqueIds.size());
        for (String userId : uniqueIds) {
            AssignedVolunteer profile = found.get(userId);
            result.add(profile == null
                    ? new AssignedVolunteer(userId, null, null, null, null)
                    : new AssignedVolunteer(userId, profile.full_name(), profile.primary_email_address(),
                                            profile.primary_phone_number(), profile.user_status()));
        }
        return new AssignedVolunteersResponse(normalizedId, List.copyOf(result));
    }
}
