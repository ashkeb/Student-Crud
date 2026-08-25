package com.example.studentcrud.repository;

import com.example.studentcrud.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    // Basic CRUD methods (save, findById, findAll, deleteById, existsById) are
    // inherited from JpaRepository - no extra code needed for the required operations.
}
