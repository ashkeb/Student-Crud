package com.example.studentcrud.controller;

import com.example.studentcrud.dto.StudentDTO;
import com.example.studentcrud.exception.ResourceNotFoundException;
import com.example.studentcrud.service.StudentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StudentController.class)
@DisplayName("StudentController web layer tests")
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StudentService studentService;

    @Test
    @DisplayName("POST /api/v1/students should create a student and return 201")
    void createStudent_shouldReturn201() throws Exception {
        StudentDTO request = StudentDTO.builder().name("John Doe").department("Computer Science").build();
        StudentDTO response = StudentDTO.builder().id(1L).name("John Doe").department("Computer Science").build();

        when(studentService.createStudent(any(StudentDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.department").value("Computer Science"));

        verify(studentService, times(1)).createStudent(any(StudentDTO.class));
    }

    @Test
    @DisplayName("POST /api/v1/students with blank name should return 400")
    void createStudent_withInvalidPayload_shouldReturn400() throws Exception {
        StudentDTO invalidRequest = StudentDTO.builder().name("").department("Computer Science").build();

        mockMvc.perform(post("/api/v1/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(studentService, never()).createStudent(any(StudentDTO.class));
    }

    @Test
    @DisplayName("GET /api/v1/students/{id} should return student when found")
    void getStudentById_whenFound_shouldReturn200() throws Exception {
        StudentDTO response = StudentDTO.builder().id(1L).name("John Doe").department("Computer Science").build();
        when(studentService.getStudentById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/students/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("John Doe"));
    }

    @Test
    @DisplayName("GET /api/v1/students/{id} should return 404 when not found")
    void getStudentById_whenNotFound_shouldReturn404() throws Exception {
        when(studentService.getStudentById(99L))
                .thenThrow(new ResourceNotFoundException("Student not found with id: 99"));

        mockMvc.perform(get("/api/v1/students/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Student not found with id: 99"));
    }

    @Test
    @DisplayName("GET /api/v1/students should return list of students")
    void getAllStudents_shouldReturn200() throws Exception {
        List<StudentDTO> students = Arrays.asList(
                StudentDTO.builder().id(1L).name("John Doe").department("Computer Science").build(),
                StudentDTO.builder().id(2L).name("Jane Roe").department("Mathematics").build()
        );
        when(studentService.getAllStudents()).thenReturn(students);

        mockMvc.perform(get("/api/v1/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("John Doe"))
                .andExpect(jsonPath("$[1].name").value("Jane Roe"));
    }

    @Test
    @DisplayName("PUT /api/v1/students/{id} should update and return 200")
    void updateStudent_whenFound_shouldReturn200() throws Exception {
        StudentDTO request = StudentDTO.builder().name("John Updated").department("Electrical Engineering").build();
        StudentDTO response = StudentDTO.builder().id(1L).name("John Updated").department("Electrical Engineering").build();

        when(studentService.updateStudent(eq(1L), any(StudentDTO.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/students/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Updated"))
                .andExpect(jsonPath("$.department").value("Electrical Engineering"));
    }

    @Test
    @DisplayName("PUT /api/v1/students/{id} should return 404 when student not found")
    void updateStudent_whenNotFound_shouldReturn404() throws Exception {
        StudentDTO request = StudentDTO.builder().name("John Updated").department("Electrical Engineering").build();

        when(studentService.updateStudent(eq(99L), any(StudentDTO.class)))
                .thenThrow(new ResourceNotFoundException("Student not found with id: 99"));

        mockMvc.perform(put("/api/v1/students/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/students/{id} should return 204 when deleted")
    void deleteStudent_whenFound_shouldReturn204() throws Exception {
        doNothing().when(studentService).deleteStudent(1L);

        mockMvc.perform(delete("/api/v1/students/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(studentService, times(1)).deleteStudent(1L);
    }

    @Test
    @DisplayName("DELETE /api/v1/students/{id} should return 404 when not found")
    void deleteStudent_whenNotFound_shouldReturn404() throws Exception {
        doThrow(new ResourceNotFoundException("Student not found with id: 99"))
                .when(studentService).deleteStudent(99L);

        mockMvc.perform(delete("/api/v1/students/{id}", 99L))
                .andExpect(status().isNotFound());
    }

    // static import helper for eq(); kept local to avoid pulling in unrelated matchers
    private static <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
