package com.example.studentcrud.service.impl;

import com.example.studentcrud.dto.StudentDTO;
import com.example.studentcrud.entity.Student;
import com.example.studentcrud.exception.ResourceNotFoundException;
import com.example.studentcrud.repository.StudentRepository;
import com.example.studentcrud.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;

    @Override
    public StudentDTO createStudent(StudentDTO studentDTO) {
        Student student = mapToEntity(studentDTO);
        Student saved = studentRepository.save(student);
        return mapToDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentDTO getStudentById(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));
        return mapToDTO(student);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentDTO> getAllStudents() {
        return studentRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public StudentDTO updateStudent(Long id, StudentDTO studentDTO) {
        Student existing = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));

        existing.setName(studentDTO.getName());
        existing.setDepartment(studentDTO.getDepartment());

        Student updated = studentRepository.save(existing);
        return mapToDTO(updated);
    }

    @Override
    public void deleteStudent(Long id) {
        if (!studentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Student not found with id: " + id);
        }
        studentRepository.deleteById(id);
    }

    private Student mapToEntity(StudentDTO dto) {
        return Student.builder()
                .id(dto.getId())
                .name(dto.getName())
                .department(dto.getDepartment())
                .build();
    }

    private StudentDTO mapToDTO(Student student) {
        return StudentDTO.builder()
                .id(student.getId())
                .name(student.getName())
                .department(student.getDepartment())
                .build();
    }
}
