package com.flowops.repository;

import com.flowops.domain.UserAccount;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserAccount, UUID> {

    /**
     * Case-insensitive lookup, matching the {@code UNIQUE INDEX ON users
     * (lower(email))} that the database enforces.
     */
    Optional<UserAccount> findByEmailIgnoreCase(String email);
}
