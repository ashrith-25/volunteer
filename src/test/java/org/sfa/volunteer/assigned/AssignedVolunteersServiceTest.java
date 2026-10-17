package org.sfa.volunteer.assigned;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/** Service tests intentionally run without any live database. */
class AssignedVolunteersServiceTest {

    @Test
    void requiresRequestId() {
        var service = new AssignedVolunteersService(id -> true, id -> List.of(), ids -> Map.of());
        assertThrows(IllegalArgumentException.class, () -> service.findAssigned(null));
        assertThrows(IllegalArgumentException.class, () -> service.findAssigned(""));
        assertThrows(IllegalArgumentException.class, () -> service.findAssigned("  "));
    }

    @Test
    void distinguishesMissingRequestFromUnassignedRequest() {
        var missing = new AssignedVolunteersService(id -> false, id -> List.of(), ids -> Map.of());
        assertThrows(NoSuchElementException.class, () -> missing.findAssigned("REQ-UNKNOWN"));
        var empty = new AssignedVolunteersService(id -> true, id -> List.of(), ids -> Map.of());
        assertEquals(List.of(), empty.findAssigned("REQ-EXISTS").assignedVolunteers());
    }

    @Test
    void supportsMultipleVolunteersAndBatchProfilesAndPreservesStatus() {
        var queries = new AtomicInteger();
        var first = new AssignedVolunteer("VOL-1", "Alex", "a@example.org", null, "Inactive");
        var service = new AssignedVolunteersService(id -> true,
                id -> List.of("VOL-1", "VOL-2", "VOL-1"), ids -> {
                    queries.incrementAndGet();
                    assertEquals(2, ids.size());
                    return Map.of("VOL-1", first);
                });
        var volunteers = service.findAssigned("REQ-1").assignedVolunteers();
        assertEquals(2, volunteers.size());
        assertEquals("Inactive", volunteers.get(0).user_status());
        assertEquals("VOL-2", volunteers.get(1).user_id());
        assertNull(volunteers.get(1).primary_email_address());
        assertEquals(1, queries.get());
    }

    @Test
    void propagatesDatabaseFailures() {
        var service = new AssignedVolunteersService(
                id -> { throw new IllegalStateException("database unavailable"); },
                id -> List.of(), ids -> Map.of());
        assertThrows(IllegalStateException.class, () -> service.findAssigned("REQ-1"));
    }
}
