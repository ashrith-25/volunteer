package org.sfa.volunteer.assigned;

import java.util.Map;
import java.util.Set;

/** One bulk fetch. Never fetch each user's profile in a separate database query. */
@FunctionalInterface
public interface VolunteerProfilesLookup {
    Map<String, AssignedVolunteer> findByUserIds(Set<String> userIds);
}
