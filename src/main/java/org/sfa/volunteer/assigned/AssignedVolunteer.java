package org.sfa.volunteer.assigned;

/** Fields required by the Review Request flow. Optional values may be null. */
public record AssignedVolunteer(
        String user_id,
        String full_name,
        String primary_email_address,
        String primary_phone_number,
        String user_status
) {}
