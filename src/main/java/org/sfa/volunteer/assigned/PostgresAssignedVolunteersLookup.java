package org.sfa.volunteer.assigned;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Reads the existing request, volunteers_assigned, users, volunteer_details and
 * user_status tables. This class does NOT make up an 'active' column that the
 * published May 2026 schema does not contain.
 *
 * IMPORTANT: enabling currentRowsOnly is a data-owner confirmation that
 * volunteers_assigned stores only CURRENT rows (history has been removed).
 * If the table stores historical rows, this adapter must be replaced with an
 * approved history-aware query; do NOT enable the flag as a workaround.
 */
@Repository
public final class PostgresAssignedVolunteersLookup implements
        RequestExistsLookup, CurrentAssignmentsLookup, VolunteerProfilesLookup {

    private final NamedParameterJdbcTemplate jdbc;
    private final String assignmentRequestColumn;
    private final boolean currentRowsOnly;

    public PostgresAssignedVolunteersLookup(
            NamedParameterJdbcTemplate jdbc,
            @Value("${saayam.assigned-volunteers.request-column:req_id}") String assignmentRequestColumn,
            @Value("${saayam.assigned-volunteers.current-rows-only:false}") boolean currentRowsOnly) {
        this.jdbc = jdbc;
        // Column names cannot be bound as values. Validate against two published
        // schema variants before adding the identifier to any SQL statement.
        if (!Set.of("req_id", "request_id").contains(assignmentRequestColumn)) {
            throw new IllegalArgumentException("Unsupported assignment request column");
        }
        this.assignmentRequestColumn = assignmentRequestColumn;
        this.currentRowsOnly = currentRowsOnly;
    }

    @Override
    public boolean exists(String reqId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM request WHERE req_id = :reqId",
                Map.of("reqId", reqId), Integer.class);
        return count != null && count > 0;
    }

    @Override
    public List<String> currentVolunteerUserIds(String reqId) {
        if (!currentRowsOnly) {
            throw new AssignmentRulesUnconfirmedException();
        }
        // Both older Virginia and newer Ireland SQL describe this assignment
        // table, but with different request FK column names. No status field.
        // This lookup is safe only if the source table is CURRENT rows only.
        String sql = "SELECT DISTINCT volunteer_id FROM volunteers_assigned "
                + "WHERE " + assignmentRequestColumn + " = :reqId ORDER BY volunteer_id";
        return jdbc.query(sql, Map.of("reqId", reqId),
                (rs, rowNum) -> rs.getString("volunteer_id"));
    }

    @Override
    public Map<String, AssignedVolunteer> findByUserIds(Set<String> userIds) {
        if (userIds.isEmpty()) return Map.of();

        // One bulk query. LEFT JOINs preserve users without volunteer_details
        // or user_status and never filter out unavailable/inactive volunteers.
        String sql = """
                SELECT u.user_id, u.full_name, u.primary_email_address,
                       u.primary_phone_number, us.user_status
                FROM users u
                LEFT JOIN volunteer_details vd ON vd.user_id = u.user_id
                LEFT JOIN user_status us ON us.user_status_id = u.user_status_id
                WHERE u.user_id IN (:userIds)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource(
                "userIds", new ArrayList<>(new LinkedHashSet<>(userIds)));
        List<AssignedVolunteer> rows = jdbc.query(sql, params, (rs, rowNum) ->
                new AssignedVolunteer(
                        rs.getString("user_id"),
                        rs.getString("full_name"),
                        rs.getString("primary_email_address"),
                        rs.getString("primary_phone_number"),
                        rs.getString("user_status")));
        Map<String, AssignedVolunteer> profiles = new HashMap<>();
        for (AssignedVolunteer volunteer : rows) {
            profiles.put(volunteer.user_id(), volunteer);
        }
        return profiles;
    }
}
