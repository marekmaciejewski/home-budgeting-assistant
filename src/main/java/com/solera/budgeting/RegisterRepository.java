package com.solera.budgeting;

import com.solera.budgeting.entities.Register;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegisterRepository extends JpaRepository<Register, Integer> {
}
