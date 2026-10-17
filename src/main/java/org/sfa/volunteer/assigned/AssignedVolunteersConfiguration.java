package org.sfa.volunteer.assigned;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the pure domain service to existing PostgreSQL data sources. */
@Configuration
public class AssignedVolunteersConfiguration {
    @Bean
    public AssignedVolunteersService assignedVolunteersService(
            PostgresAssignedVolunteersLookup repository) {
        return new AssignedVolunteersService(repository, repository, repository);
    }
}
