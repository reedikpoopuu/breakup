package com.example.demo.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findBySmartIdIdentity(String smartIdIdentity);

    boolean existsByRole(Role role);
}
