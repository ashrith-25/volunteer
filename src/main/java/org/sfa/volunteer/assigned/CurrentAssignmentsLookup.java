package org.sfa.volunteer.assigned;

import java.util.List;

/** Adapter must filter cancelled, rejected, completed and replaced history using verified rules. */
@FunctionalInterface
public interface CurrentAssignmentsLookup {
    List<String> currentVolunteerUserIds(String reqId);
}
