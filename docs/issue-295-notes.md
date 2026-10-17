# Issue #295 - Assigned volunteers endpoint (integration draft)

**PR readiness:** blocked by confirmed current-vs-historical assignment semantics (fork connected; draft PR opened for review). **Do not merge or deploy** in this state.

## Verified repository findings

- Issue: https://github.com/saayam-for-all/data/issues/295
- Target microservice: https://github.com/saayam-for-all/volunteer
- Request records: `request` table, key `req_id`.
- Assignment records: `volunteers_assigned` table. Older Virginia DDL and `db_info.json` use `request_id`, newer Ireland (May 2026) DDL uses `req_id`. Configure `saayam.assigned-volunteers.request-column` according to actual deployment.
- Assignment columns: volunteer identifier and volunteer type; no assignment status or active/current flag in either published DDL. The sample CSV contains repeated roles for some requests (possibly history, possibly parallel volunteers). These cannot be disambiguated safely from the published model.
- Volunteer contact/profile: `users`, optional `volunteer_details`, status lookup: `user_status`. No need to join skills.
- Existing Volunteer Controller uses `/0.0.1/volunteers` path prefix and the `SaayamResponse` wrapper.

## Included implementation

`POST /0.0.1/volunteers/assigned`

Input: `{"req_id":"REQ-00-000-000-001"}`

Successful response is returned inside `SaayamResponse.data`:

```json
{
  "req_id": "REQ-00-000-000-001",
  "assignedVolunteers": [
    {
      "user_id": "VOL-001",
      "full_name": "Example Volunteer",
      "primary_email_address": "volunteer@example.org",
      "primary_phone_number": null,
      "user_status": "Active"
    }
  ]
}
```

- Missing/blank `req_id` -> HTTP 400.
- Nonexistent `req_id` -> HTTP 404.
- Valid request with no assignments -> 200 with empty array **after source-of-truth assignment policy is configured**.
- DB failures -> sanitized HTTP 500 with Saayam error envelope.
- Until history semantics are confirmed -> HTTP 503 (fail closed, not a false success).
- Multiple assigned IDs -> bulk profile lookup (one query), no filtering by user availability/status, nullable optional fields.
- Named SQL bind parameters for request/user IDs. Column name is explicitly restricted to two published variants (`req_id` or `request_id`), never caller supplied.

## REQUIRED before PR is review-ready

1. Ask the data/Request service owners: Does `volunteers_assigned` contain **only current assignments**, or does it also hold historical assignments? If history, which additional source, event, status, or business rule defines current volunteers? Without the answer, acceptance criteria on replaced/cancelled/rejected volunteers are unverifiable.
2. Check production/development **effective schema/search_path** and whether assignment FK is `req_id` or `request_id`. Run the read-only inspection in `docs/issue-295-schema-check.sql` against approved development DB.
3. If table contains current rows only, configure in approved deployment environment (not a committed secret file):
   `SAAYAM_ASSIGNED_VOLUNTEERS_CURRENT_ROWS_ONLY=true`
   and, for older Virginia naming, `SAAYAM_ASSIGNED_VOLUNTEERS_REQUEST_COLUMN=request_id`.
   **DO NOT enable currentRowsOnly** if the table holds history.
4. If the table holds history, implement a *verified* history-aware query and remove the fail-closed restriction only after tests cover reassignment, rejection and cancellation. There is no sound way to infer history from just role/timestamp.
5. Verify request-level authorization (request owner, assigned volunteer, or trusted service/admin). Email and phone are personally identifiable. Existing gateway configuration must authorize callers before the endpoint returns contact details; don't trust unverified caller headers.
6. Review response envelope expectations: SaayamResponse wrapper vs raw payload, and request-not-found error code convention.
7. Add live dev integration tests with verified history fixtures and request status handling.
8. Run Maven tests in an actual project checkout and smoke test with development Postgres. Not executed in this environment, which has no Maven and no network checkout.

## Evidence / test status

- Independent Java 17 core test harness: **PASS 13 assertions**.
- JUnit/Mockito source included; cannot execute without Maven dependencies. Status: NOT RUN.
- Spring controller, JDBC adapter and SQL integration: NOT COMPILED in project or tested against live DB.
- No schema mutations, no new assignment tables, no credentials.

## Proposed PR

Title: `feat(volunteer): lookup volunteers assigned to a request (data#295)`

Opening as **draft** is appropriate once the repository is forked and code is pushed, to get assignment semantics confirmed. Only mark ready after remaining tests pass.
