package org.sfa.volunteer.assigned;

/** Adapter must consult the canonical request store (not a copied table). */
@FunctionalInterface
public interface RequestExistsLookup {
    boolean exists(String reqId);
}
