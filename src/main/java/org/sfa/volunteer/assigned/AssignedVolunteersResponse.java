package org.sfa.volunteer.assigned;

import java.util.List;

/** A valid request without current assignments has an empty list, not an error. */
public record AssignedVolunteersResponse(String req_id, List<AssignedVolunteer> assignedVolunteers) {}
