package com.verbamind.organization.repository;

import com.verbamind.organization.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    boolean existsBySlug(String slug);
    Optional<Organization> findBySlug(String slug);
}