package com.funwallet.backend.repository;

import com.funwallet.backend.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByName(String name);
    Optional<AppUser> findByEmail(String email);
}
