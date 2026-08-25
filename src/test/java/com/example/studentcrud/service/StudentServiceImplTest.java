package com.example.studentcrud.service;

import com.example.studentcrud.dto.StudentDTO;
import com.example.studentcrud.entity.Student;
import com.example.studentcrud.exception.ResourceNotFoundException;
import com.example.studentcrud.repository.StudentRepository;
import com.example.studentcrud.service.impl.StudentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StudentServiceImpl unit tests")
class StudentServiceImplTest {

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private StudentServiceImpl studentService;

    private Student student;
    private StudentDTO studentDTO;

    @BeforeEach
    void setUp() {
        student = Student.builder()
                .id(1L)
                .name("John Doe")
                .department("Computer Science")
                .build();

        studentDTO = StudentDTO.builder()
                .id(1L)
                .name("John Doe")
                .department("Computer Science")
                .build();
    }

    @Test
    @DisplayName("createStudent() should save and return the created student")
    void createStudent_shouldReturnSavedStudent() {
        when(studentRepository.save(any(Student.class))).thenReturn(student);

        StudentDTO result = studentService.createStudent(studentDTO);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("John Doe");
        assertThat(result.getDepartment()).isEqualTo("Computer Science");
        verify(studentRepository, times(1)).save(any(Student.class));
    }

    @Test
    @DisplayName("getStudentById() should return student when found")
    void getStudentById_whenFound_shouldReturnStudent() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));

        StudentDTO result = studentService.getStudentById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(studentRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getStudentById() should throw ResourceNotFoundException when not found")
    void getStudentById_whenNotFound_shouldThrowException() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.getStudentById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(studentRepository, times(1)).findById(99L);
    }

    @Test
    @DisplayName("getAllStudents() should return list of all students")
    void getAllStudents_shouldReturnAllStudents() {
        Student student2 = Student.builder().id(2L).name("Jane Roe").department("Mathematics").build();
        when(studentRepository.findAll()).thenReturn(Arrays.asList(student, student2));

        List<StudentDTO> result = studentService.getAllStudents();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("John Doe");
        assertThat(result.get(1).getName()).isEqualTo("Jane Roe");
        verify(studentRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("updateStudent() should update and return updated student when found")
    void updateStudent_whenFound_shouldReturnUpdatedStudent() {
        StudentDTO updateRequest = StudentDTO.builder()
                .name("John Updated")
                .department("Electrical Engineering")
                .build();

        Student updatedEntity = Student.builder()
                .id(1L)
                .name("John Updated")
                .department("Electrical Engineering")
                .build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(studentRepository.save(any(Student.class))).thenReturn(updatedEntity);

        StudentDTO result = studentService.updateStudent(1L, updateRequest);

        assertThat(result.getName()).isEqualTo("John Updated");
        assertThat(result.getDepartment()).isEqualTo("Electrical Engineering");
        verify(studentRepository, times(1)).findById(1L);
        verify(studentRepository, times(1)).save(any(Student.class));
    }

    @Test
    @DisplayName("updateStudent() should throw ResourceNotFoundException when student not found")
    void updateStudent_whenNotFound_shouldThrowException() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.updateStudent(99L, studentDTO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(studentRepository, never()).save(any(Student.class));
    }

    @Test
    @DisplayName("deleteStudent() should delete when student exists")
    void deleteStudent_whenExists_shouldDelete() {
        when(studentRepository.existsById(1L)).thenReturn(true);
        doNothing().when(studentRepository).deleteById(1L);

        studentService.deleteStudent(1L);

        verify(studentRepository, times(1)).existsById(1L);
        verify(studentRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("deleteStudent() should throw ResourceNotFoundException when student does not exist")
    void deleteStudent_whenNotExists_shouldThrowException() {
        when(studentRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> studentService.deleteStudent(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(studentRepository, never()).deleteById(anyLong());
    }
}
