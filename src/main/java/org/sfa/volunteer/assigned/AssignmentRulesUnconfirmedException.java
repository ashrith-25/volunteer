package org.sfa.volunteer.assigned;

/** Prevents returning historical volunteers as if they were current. */
public final class AssignmentRulesUnconfirmedException extends IllegalStateException {
    public AssignmentRulesUnconfirmedException() {
        super("The current-assignment rule has not been confirmed for volunteers_assigned");
    }
}
