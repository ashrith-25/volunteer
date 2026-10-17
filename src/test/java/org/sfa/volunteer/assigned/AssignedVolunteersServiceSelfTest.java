package org.sfa.volunteer.assigned;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/** Run with JDK 17+; self-contained logic tests, not DB integration tests. */
public final class AssignedVolunteersServiceSelfTest {
    private static int tests;

    private static void check(boolean ok, String message) {
        tests++;
        if (!ok) throw new AssertionError(message);
    }

    private static void rejectsMissingId() {
        AssignedVolunteersService service = new AssignedVolunteersService(id -> true, id -> List.of(), ids -> Map.of());
        for (String reqId : new String[]{null, "", "  "}) {
            try {
                service.findAssigned(reqId);
                throw new AssertionError("Expected 400-equivalent validation failure");
            } catch (IllegalArgumentException expected) {
                check(expected.getMessage().contains("req_id"), "invalid missing-ID message");
            }
        }
    }

    private static void nonexistentRequest() {
        AssignedVolunteersService service = new AssignedVolunteersService(id -> false, id -> List.of(), ids -> Map.of());
        try {
            service.findAssigned("REQ-MISSING");
            throw new AssertionError("Expected 404-equivalent failure");
        } catch (NoSuchElementException expected) {
            check(expected.getMessage().contains("REQ-MISSING"), "missing request error");
        }
    }

    private static void oneOrMultipleOrNoAssignments() {
        AssignedVolunteer a = new AssignedVolunteer("U1", "Alex", "alex@test", null, "Inactive");
        AssignedVolunteer b = new AssignedVolunteer("U2", "Bo", null, null, null);
        AtomicInteger queries = new AtomicInteger();
        AssignedVolunteersService service = new AssignedVolunteersService(id -> true,
                id -> switch (id) {
                    case "ONE" -> List.of("U1");
                    case "MANY" -> List.of("U1", "U2", "U1");
                    default -> List.of();
                }, ids -> {
                    queries.incrementAndGet();
                    return Map.of("U1", a, "U2", b);
                });
        check(service.findAssigned("NONE").assignedVolunteers().isEmpty(), "empty assignment list");
        check(service.findAssigned("ONE").assignedVolunteers().size() == 1, "one assignment");
        check(service.findAssigned("MANY").assignedVolunteers().size() == 2, "deduplicated multiple assignments");
        check(queries.get() == 2, "one bulk profile query per non-empty request");
        check(service.findAssigned("ONE").assignedVolunteers().get(0).user_status().equals("Inactive"),
                "do not filter volunteers by user status");
        check(service.findAssigned("MANY").assignedVolunteers().get(1).primary_email_address() == null,
                "preserve null optional contact fields");
    }

    private static void missingVolunteerProfile() {
        AssignedVolunteersService service = new AssignedVolunteersService(
                id -> true, id -> List.of("U404"), ids -> Map.of());
        var result = service.findAssigned("REQ1");
        check(result.assignedVolunteers().get(0).user_id().equals("U404"), "preserve assigned ID");
        check(result.assignedVolunteers().get(0).full_name() == null, "missing optional profile");
    }

    private static void databaseFailureNotHidden() {
        AssignedVolunteersService service = new AssignedVolunteersService(
                id -> { throw new IllegalStateException("database connection failed"); },
                id -> List.of(), ids -> Map.of());
        try {
            service.findAssigned("REQ1");
            throw new AssertionError("Expected database exception");
        } catch (IllegalStateException expected) {
            check(expected.getMessage().contains("database connection"), "database failures propagate");
        }
    }

    public static void main(String[] args) {
        rejectsMissingId();
        nonexistentRequest();
        oneOrMultipleOrNoAssignments();
        missingVolunteerProfile();
        databaseFailureNotHidden();
        System.out.println("PASS: " + tests + " core assertions");
    }
}
