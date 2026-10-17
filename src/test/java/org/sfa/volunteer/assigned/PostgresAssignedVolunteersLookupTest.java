package org.sfa.volunteer.assigned;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/** Adapter contract checks; live database integration still requires dev environment. */
class PostgresAssignedVolunteersLookupTest {

    @Test
    void rejectsUnsafeColumnNames() {
        var jdbc = mock(NamedParameterJdbcTemplate.class);
        assertThrows(IllegalArgumentException.class,
                () -> new PostgresAssignedVolunteersLookup(jdbc, "req_id; DROP TABLE users", true));
    }

    @Test
    void cannotClaimCurrentAssignmentsWithoutConfirmation() {
        var jdbc = mock(NamedParameterJdbcTemplate.class);
        var repo = new PostgresAssignedVolunteersLookup(jdbc, "req_id", false);
        assertThrows(AssignmentRulesUnconfirmedException.class,
                () -> repo.currentVolunteerUserIds("REQ-1"));
        verifyNoInteractions(jdbc);
    }

    @Test
    void configuredLookupBindsRequestId() {
        var jdbc = mock(NamedParameterJdbcTemplate.class);
        var repo = new PostgresAssignedVolunteersLookup(jdbc, "req_id", true);
        when(jdbc.query(anyString(), anyMap(), org.mockito.ArgumentMatchers.<RowMapper<String>>any()))
                .thenReturn(List.of("VOL-1", "VOL-2"));
        assertEquals(List.of("VOL-1", "VOL-2"), repo.currentVolunteerUserIds("REQ-1"));
        verify(jdbc).query(contains("req_id = :reqId"), eq(Map.of("reqId", "REQ-1")),
                org.mockito.ArgumentMatchers.<RowMapper<String>>any());
    }

    @Test
    void emptyUserIdsAvoidQuery() {
        var jdbc = mock(NamedParameterJdbcTemplate.class);
        var repo = new PostgresAssignedVolunteersLookup(jdbc, "req_id", true);
        assertTrue(repo.findByUserIds(Set.of()).isEmpty());
        verifyNoInteractions(jdbc);
    }
}
