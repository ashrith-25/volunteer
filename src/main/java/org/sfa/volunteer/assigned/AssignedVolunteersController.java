package org.sfa.volunteer.assigned;

import java.util.Map;
import java.util.NoSuchElementException;

import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Follows the existing Volunteer Microservice 0.0.1 route and response envelope. */
@RestController
@RequestMapping("/0.0.1/volunteers")
public final class AssignedVolunteersController {
    private final AssignedVolunteersService service;
    private final ResponseBuilder responseBuilder;

    public AssignedVolunteersController(AssignedVolunteersService service, ResponseBuilder responseBuilder) {
        this.service = service;
        this.responseBuilder = responseBuilder;
    }

    @PostMapping("/assigned")
    public SaayamResponse<AssignedVolunteersResponse> assigned(
            @RequestBody(required = false) Map<String, Object> body) {
        Object reqId = body == null ? null : body.get("req_id");
        if (reqId != null && !(reqId instanceof String)) {
            throw new IllegalArgumentException("req_id must be a string");
        }
        AssignedVolunteersResponse response = service.findAssigned((String) reqId);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<SaayamResponse<Void>> invalid(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(
                responseBuilder.buildErrorResponse(400, SaayamStatusCode.BAD_REQUEST, e.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<SaayamResponse<Void>> notFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                responseBuilder.buildErrorResponse(404, SaayamStatusCode.INVALID_PARAMETER, e.getMessage()));
    }

    @ExceptionHandler(AssignmentRulesUnconfirmedException.class)
    public ResponseEntity<SaayamResponse<Void>> unconfigured(AssignmentRulesUnconfirmedException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                responseBuilder.buildErrorResponse(503, SaayamStatusCode.INTERNAL_SERVER_ERROR,
                        "Assigned-volunteer lookup is not configured"));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<SaayamResponse<Void>> databaseFailure(DataAccessException e) {
        // Deliberately do not expose SQL statements or database connection details.
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                responseBuilder.buildErrorResponse(500, SaayamStatusCode.DATABASE_ERROR,
                        "Database request failed"));
    }
}
