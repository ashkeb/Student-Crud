package com.example.studentcrud.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for Student, used at the API boundary.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Student details payload")
public class StudentDTO {

    @Schema(description = "Auto-generated student id", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "Name must not be blank")
    @Schema(description = "Student's full name", example = "John Doe")
    private String name;

    @NotBlank(message = "Department must not be blank")
    @Schema(description = "Department the student belongs to", example = "Computer Science")
    private String department;
}
