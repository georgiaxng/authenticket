package com.authenticket.authenticket.repository;

import com.authenticket.authenticket.model.Admin;
import io.micrometer.observation.ObservationFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public interface AdminRepository extends JpaRepository<Admin, Integer> {
    Optional<Admin> findByEmail(String email);

    @Transactional
    @Modifying
    @Query("UPDATE EventOrganiser a " +
            "SET a.enabled = TRUE, a.admin = ?2 WHERE a.email = ?1")
    void enableEventOrganisation(String email, String adminId);

//    ObservationFilter findById(Integer adminId);
}
