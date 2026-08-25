# Student CRUD — Spring Boot Project: Complete Walkthrough

This document explains the entire project from scratch — what was built, why each
piece exists, every layer of the code, the database decision (H2 instead of MySQL),
the security fixes applied, and the real debugging journey that got this from zero
to a working, tested application. It's written so you can walk your mentor through
it end-to-end.

---

## 1. The Requirement

Build a Spring Boot application that performs CRUD (Create, Read, Update, Delete)
operations on student records via a REST API, with:

- **Table**: `student` — fields `id`, `name`, `department`
- **Database**: MySQL (originally specified)
- **Documentation**: Swagger/OpenAPI, so the API is self-describing and testable
  from a browser
- **Testing**: automated unit tests
- Plus the full environment story: IDE setup, Maven setup, DB setup, JDK setup,
  Git check-in, and a live demo

---

## 2. Technology Stack

| Concern | Technology | Version | Why |
|---|---|---|---|
| Language | Java | 17 | LTS version, required baseline for Spring Boot 3.x |
| Framework | Spring Boot | 3.5.16 | Handles web server, dependency injection, auto-configuration |
| Data access | Spring Data JPA + Hibernate | 6.6.53 | Turns Java objects into database rows without hand-written SQL |
| Database (current) | H2 (file-based) | 2.3.232 | See Section 5 — swapped in because MySQL couldn't be installed |
| Database (designed for) | MySQL | 8.0 / connector 9.7.0 | Original requirement; config kept in place, commented out |
| API docs | springdoc-openapi (Swagger UI) | 2.8.17 | Auto-generates interactive API documentation from the code |
| Testing | JUnit 5, Mockito, MockMvc, AssertJ | via spring-boot-starter-test | Three layers of automated verification (see Section 8) |
| Build tool | Maven | 3.9.16 | Dependency management and build lifecycle |
| Boilerplate reduction | Lombok | 1.18.46 | Auto-generates getters/setters/constructors from annotations |

---

## 3. Architecture — Why the Code Is Organized This Way

The project follows a standard **layered architecture**, which is the conventional
way to structure a Spring Boot REST API. Each layer has exactly one job, which
makes the code easier to test, change, and reason about:

```
HTTP Request
     ↓
┌─────────────────────┐
│   Controller layer   │  ← receives HTTP requests, returns HTTP responses
│  StudentController    │     (knows nothing about the database)
└─────────────────────┘
     ↓
┌─────────────────────┐
│    Service layer      │  ← business logic (validation rules, orchestration)
│  StudentService /      │     (knows nothing about HTTP)
│  StudentServiceImpl    │
└─────────────────────┘
     ↓
┌─────────────────────┐
│  Repository layer     │  ← talks to the database
│  StudentRepository     │     (just an interface — Spring generates the implementation)
└─────────────────────┘
     ↓
┌─────────────────────┐
│    Entity layer        │  ← Java class that maps 1:1 to the `student` table
│      Student            │
└─────────────────────┘
     ↓
   Database (H2 / MySQL)
```

Two more layers cut across all of these:

- **DTO (Data Transfer Object)** — `StudentDTO` is the shape of data that crosses
  the API boundary (what the client sends/receives). It's kept separate from the
  `Student` entity so that internal database structure and external API contract
  can evolve independently, and so validation rules live at the API boundary.
- **Exception handling** — `GlobalExceptionHandler` catches errors thrown anywhere
  in the app and converts them into consistent, structured JSON error responses
  instead of raw stack traces.

---

## 4. Every File, Explained

### 4.1 `Student.java` — the Entity (maps Java ↔ database table)

```java
@Entity
@Table(name = "student")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "department", nullable = false, length = 100)
    private String department;
}
```

- `@Entity` tells Hibernate "this class represents a database table."
- `@Table(name = "student")` pins it to the exact table name required.
- `@Id` + `@GeneratedValue(strategy = GenerationType.IDENTITY)` means the database
  auto-increments the `id` column — we never set it manually.
- `@Column(nullable = false, length = 100)` enforces `NOT NULL` and `VARCHAR(100)`
  at the database level, matching the required fields.
- `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder` are **Lombok**
  annotations — they generate getters, setters, `equals()`/`hashCode()`,
  `toString()`, constructors, and a builder pattern automatically, so we don't
  hand-write ~80 lines of boilerplate.

### 4.2 `StudentDTO.java` — the API-facing shape

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentDTO {

    private Long id;

    @NotBlank(message = "Name must not be blank")
    private String name;

    @NotBlank(message = "Department must not be blank")
    private String department;
}
```

`@NotBlank` is a **Bean Validation** annotation. When a request comes in, Spring
automatically checks these rules before the controller method even runs — if
`name` or `department` is empty, the request is rejected with a `400 Bad Request`
before touching the database at all.

**Why a separate DTO instead of just using `Student` directly?** It's a common
best practice: it means the internal database schema (entity) and the external
API contract (DTO) can change independently, and it's the natural place to attach
validation rules that only apply to incoming API requests, not to every database
operation.

### 4.3 `StudentRepository.java` — the data access layer

```java
@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    // No method bodies needed!
}
```

This is the most "magic" part of Spring Data JPA. By extending `JpaRepository<Student, Long>`
(entity type, id type), we get a full set of working database operations —
`save()`, `findById()`, `findAll()`, `deleteById()`, `existsById()`, and more —
**without writing any SQL or implementation code**. Spring generates the
implementation at startup by inspecting the interface.

### 4.4 `StudentService.java` + `StudentServiceImpl.java` — business logic

```java
public interface StudentService {
    StudentDTO createStudent(StudentDTO studentDTO);
    StudentDTO getStudentById(Long id);
    List<StudentDTO> getAllStudents();
    StudentDTO updateStudent(Long id, StudentDTO studentDTO);
    void deleteStudent(Long id);
}
```

The implementation (`StudentServiceImpl`) does three things the repository alone
can't:
1. **Converts between DTO and Entity** (`mapToEntity` / `mapToDTO`), keeping that
   translation logic in one place.
2. **Applies business rules** — e.g. `getStudentById` throws a
   `ResourceNotFoundException` if the id doesn't exist, rather than silently
   returning `null`.
3. **Manages transactions** — the class is annotated `@Transactional`, so if
   anything fails mid-operation, the database change is rolled back rather than
   left half-applied.

Example — the update method shows the "find, then modify, then save" pattern:

```java
@Override
public StudentDTO updateStudent(Long id, StudentDTO studentDTO) {
    Student existing = studentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));

    existing.setName(studentDTO.getName());
    existing.setDepartment(studentDTO.getDepartment());

    Student updated = studentRepository.save(existing);
    return mapToDTO(updated);
}
```

### 4.5 `StudentController.java` — the REST API surface

```java
@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
@Tag(name = "Student", description = "CRUD APIs for managing student details")
public class StudentController {

    private final StudentService studentService;

    @PostMapping
    public ResponseEntity<StudentDTO> createStudent(@Valid @RequestBody StudentDTO studentDTO) {
        StudentDTO created = studentService.createStudent(studentDTO);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentDTO> getStudentById(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.getStudentById(id));
    }

    @GetMapping
    public ResponseEntity<List<StudentDTO>> getAllStudents() {
        return ResponseEntity.ok(studentService.getAllStudents());
    }

    @PutMapping("/{id}")
    public ResponseEntity<StudentDTO> updateStudent(@PathVariable Long id, @Valid @RequestBody StudentDTO studentDTO) {
        return ResponseEntity.ok(studentService.updateStudent(id, studentDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }
}
```

This maps directly to REST conventions:

| HTTP Method | URL | Action | Success code |
|---|---|---|---|
| POST | `/api/v1/students` | Create | `201 Created` |
| GET | `/api/v1/students/{id}` | Read one | `200 OK` |
| GET | `/api/v1/students` | Read all | `200 OK` |
| PUT | `/api/v1/students/{id}` | Update | `200 OK` |
| DELETE | `/api/v1/students/{id}` | Delete | `204 No Content` |

`@Valid` triggers the `@NotBlank` checks on `StudentDTO`. The `@Operation` /
`@ApiResponses` annotations (from Swagger, trimmed from the snippet above for
brevity — see the actual file) are what make the Swagger UI documentation
detailed and interactive rather than just listing bare endpoints.

### 4.6 Exception handling — turning crashes into clean JSON

`ResourceNotFoundException.java` is a small custom exception thrown when a
student id doesn't exist. `GlobalExceptionHandler.java` uses `@RestControllerAdvice`
to catch it (and validation errors, and anything else) **anywhere in the
application** and convert it into a consistent JSON shape:

```java
@ExceptionHandler(ResourceNotFoundException.class)
public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
    ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error(HttpStatus.NOT_FOUND.getReasonPhrase())
            .message(ex.getMessage())
            .path(request.getRequestURI())
            .build();
    return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
}
```

So instead of a raw 500 error and a Java stack trace leaking to the client, a
request for a missing student gets back:
```json
{
  "timestamp": "2026-08-18 10:40:00",
  "status": 404,
  "error": "Not Found",
  "message": "Student not found with id: 99",
  "path": "/api/v1/students/99"
}
```

### 4.7 `OpenApiConfig.java` — Swagger metadata

This small config class just sets the title, description, and version shown at
the top of the Swagger UI page. The actual endpoint documentation is generated
automatically by springdoc-openapi from the `@Operation`/`@ApiResponses`/`@Schema`
annotations scattered through the controller and DTO — nothing here is hand-written
HTML or JSON.

---

## 5. Why H2 Instead of MySQL — and How the App Doesn't Care

### The original plan
The task specified MySQL, and the project's `pom.xml` and `application.properties`
were built for it from the start — `mysql-connector-j` as the JDBC driver, and a
connection string pointing at `localhost:3306`.

### What actually happened
The development machine is a corporate laptop with no admin/install rights,
which meant:
- The MySQL Installer couldn't run (needs elevation)
- Docker Desktop wasn't available either

So there was no way to stand up a real MySQL server on that machine.

### The fix: H2, a database that needs no installation
**H2** is a full relational database written entirely in Java. Instead of being a
separate program you install and run as a service (like MySQL), it's just a
`.jar` file — a normal Maven dependency, already declared in `pom.xml`:

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
</dependency>
```

When the Spring Boot app starts, H2 runs *inside the same Java process* — no
separate server, no service to install, no admin rights needed. Configured in
**file mode**, it writes its data to a file on disk (`./data/studentdb`) so
records survive application restarts, exactly like a real database would.

### Why the rest of the application didn't need to change
This is the actual engineering point worth explaining to a mentor: **the
`Student` entity, `StudentRepository`, `StudentService`, and `StudentController`
have zero MySQL-specific or H2-specific code in them.** They're written entirely
against Spring Data JPA and Hibernate's abstractions. The only place the database
choice lives is three lines in `application.properties`:

```properties
spring.datasource.url=jdbc:h2:file:./data/studentdb;DB_CLOSE_DELAY=-1;MODE=MySQL
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
```

The MySQL configuration is kept in the same file, commented out, ready to
re-enable:
```properties
#spring.datasource.url=jdbc:mysql://localhost:3306/studentdb?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
#spring.datasource.username=root
#spring.datasource.password=root
#spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

Switching back to MySQL later (once there's access to a real instance — a
provisioned dev server, a teammate's machine, a cloud free tier, etc.) is a
**four-line config swap, not a code change.** This is exactly the kind of
database-independence JPA/Hibernate is designed to provide, and it's a good
example to show a mentor of *why* the repository pattern and ORM abstraction
matter in practice, not just in theory.

### The H2 web console
Because H2 ships with a lightweight browser-based database viewer, the data can
be inspected directly without any external tool:
```
http://localhost:8080/h2-console
JDBC URL: jdbc:h2:file:./data/studentdb
User: sa   Password: (blank)
```

### The `MODE=MySQL` detail
Notice `MODE=MySQL` in the H2 connection URL — H2 supports "compatibility modes"
that make it interpret SQL closer to how MySQL would, reducing the chance of
subtle SQL-dialect differences causing surprises when the real MySQL migration
happens later.

---

## 6. Security: Dependency Vulnerabilities Found and Fixed

A dependency scan (Mend.io) flagged three transitive vulnerabilities in the
original dependency set. Each was investigated and fixed individually rather
than blindly bumping versions:

| Vulnerability | Vulnerable dependency | Root cause | Fix |
|---|---|---|---|
| CVE-2024-38819, CVE-2025-41242, CVE-2026-22737, CVE-2026-22741, CVE-2026-22735 | `spring-webmvc:6.1.13` | Spring Boot 3.3.4 pulled in Spring Framework 6.1, a line that reached end-of-life in mid-2025 and stopped receiving patches | Bumped Spring Boot parent to **3.5.16** (Spring Framework 6.2, patched before its own June 2026 end-of-life) |
| WS-2026-0003 | `jackson-core:2.17.2` | Old Jackson version bundled with Spring Boot 3.3.4 | Resolved automatically by the same version bump — 3.5.16 manages a newer Jackson line |
| CVE-2025-48924 (uncontrolled recursion / denial-of-service risk in `ClassUtils.getClass`) | `commons-lang3:3.14.0` | Pulled in transitively by springdoc/swagger-core, not directly controlled by the Spring Boot BOM | Pinned explicitly via `<commons-lang3.version>3.18.0</commons-lang3.version>` in `pom.xml` |

Two supporting changes were needed to keep the version bump internally
consistent:
- **springdoc-openapi** bumped `2.6.0 → 2.8.17` (the 2.8.x line targets Spring
  Framework 6.2; springdoc's newer `3.x` line targets Spring Boot 4/Framework 7,
  which is a bigger jump not taken here).
- `spring.jpa.database-platform` changed from the deprecated `MySQL8Dialect` to
  `MySQLDialect`, since Hibernate 6.6 (bundled with Boot 3.5) deprecated the
  version-specific dialect classes.

**Honest caveat worth mentioning to a mentor:** Spring Boot 3.5.16 is itself now
past its own end-of-life window. It was the most compatible fix available on the
3.x line without a riskier jump to Spring Boot 4.x (which involves a Jackson 2→3
migration and other breaking changes). For a codebase that needs continuous
security coverage going forward, migrating to Spring Boot 4.1.x is the
recommended next step — treated as a deliberate, separately-tested follow-up
rather than bundled into this fix.

---

## 7. Environment Setup

### JDK
JDK 17 (OpenLogic OpenJDK build), confirmed via:
```
java -version
```

### Maven
Apache Maven 3.9.16, confirmed via:
```
mvn -version
```
Maven reads `pom.xml`, resolves all dependencies from Maven Central into the
local `~/.m2` repository, compiles the code, runs the tests, and packages the
final `.jar`.

### IDE
IntelliJ IDEA, with the Lombok plugin enabled (required — without it, IntelliJ
can't see the getters/setters/constructors Lombok generates at compile time,
and the code appears broken even though it compiles fine via Maven).

### Database
H2, file-based, requiring no installation — see Section 5.

---

## 8. Testing — Three Layers, Different Purposes

19 automated tests run via `mvn clean install` or `mvn test`. They're organized
in three tiers, each testing a different layer of the application:

### 8.1 Unit tests — `StudentServiceImplTest.java` (8 tests)
Tests the service layer **in isolation**, using Mockito to fake out the
repository (`@Mock private StudentRepository studentRepository`). This verifies
the business logic — e.g. "does `getStudentById` throw `ResourceNotFoundException`
when the repository returns empty?" — without touching any real database at all.

```java
@Test
void getStudentById_whenNotFound_shouldThrowException() {
    when(studentRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> studentService.getStudentById(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("99");
}
```

### 8.2 Web-layer tests — `StudentControllerTest.java` (9 tests)
Tests the controller **in isolation** using `@WebMvcTest` and `MockMvc`, which
spins up just the web layer (no real database, no full Spring context) and
simulates real HTTP requests. This verifies routing, status codes, JSON
serialization, and validation behavior:

```java
@Test
void createStudent_withInvalidPayload_shouldReturn400() throws Exception {
    StudentDTO invalidRequest = StudentDTO.builder().name("").department("Computer Science").build();

    mockMvc.perform(post("/api/v1/students")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
}
```

### 8.3 Integration test — `StudentCrudApplicationTests.java` (2 tests)
Boots the **entire real Spring application** — full context, real JPA/Hibernate,
a real (in-memory) H2 database via the `test` Spring profile — and drives the
complete lifecycle through actual HTTP calls: create → read → update → delete,
verifying the whole stack works together end-to-end, not just each piece alone.

```java
@Test
void fullCrudLifecycle_shouldWorkEndToEnd() throws Exception {
    // POST to create, GET to verify, PUT to update, DELETE to remove,
    // then GET again to confirm it's really gone (expects 404)
}
```

### Why three layers instead of just one big test?
- Unit tests are fast and pinpoint exactly which piece of logic broke.
- Web-layer tests catch HTTP/JSON/routing mistakes without the overhead of a
  full app + database.
- The integration test is the only one that proves all the pieces actually
  connect correctly — but it's slower, so there's just enough of it to cover
  the critical path, not every edge case (those are covered faster at the unit
  level instead).

This is a standard "testing pyramid" approach — many fast, focused tests at the
bottom, fewer expensive end-to-end tests at the top.

### The test output for reference
```
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0  -- StudentController web layer tests
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0  -- StudentServiceImpl unit tests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0  -- StudentCrudApplicationTests
[INFO] Tests run: 19, Failures: 0, Errors: 0, Skipped: 0 -- TOTAL
[INFO] BUILD SUCCESS
```

---

## 9. The Debugging Journey (worth mentioning to a mentor — this is real troubleshooting)

Getting from "code written" to "app running" on this particular machine surfaced
several real-world problems, each diagnosed and solved in turn:

1. **`Connection refused` to MySQL** — diagnosed as MySQL simply not being
   installed/running yet, not a code bug.
2. **No admin rights to install MySQL or Docker** — corporate laptop restriction
   discovered through direct questioning, ruling out the two most common local
   database options.
3. **Zip extraction silently dropped files** — after switching to an H2-based
   local plan, the project zip extracted only empty folders (no actual `.java`
   or `.properties` files), twice, on this machine — most likely something in
   the corporate environment (AV/DLP scanning, or a sync tool) interfering with
   zip extraction of source code.
4. **Workaround: a self-contained PowerShell generator script** — rather than
   keep fighting zip extraction, all 21 project files were embedded directly as
   text inside a single `.ps1` script, which wrote them to disk directly with no
   archive format involved. This worked and got a fully compiling, fully
   tested project onto the machine.
5. **A second properties file silently failed to get created** — even after the
   script succeeded overall, one specific file (`application-local.properties`)
   didn't materialize on disk for unclear reasons. Rather than keep chasing that,
   the fix was simplified: merge the H2 configuration directly into the one
   config file (`application.properties`) already proven to reliably exist,
   eliminating the dependency on a second file entirely.
6. **`Cannot load driver class: org.h2.Driver`** — after the H2 config was in
   place, the H2 dependency itself turned out to be scoped as `test`-only in
   `pom.xml` (only available during `mvn test`, not during `mvn spring-boot:run`).
   Removing the test scope made H2 available at runtime, which finally let the
   application start successfully.

Each of these was diagnosed from the actual error message and stack trace rather
than guessed at — for example, `Connection refused` in a Java stack trace always
means "nothing is listening on that port," which immediately rules out
credentials/config issues and points at "is the service even running."

---

## 10. How to Run and Demo It

```powershell
cd C:\Users\ShanbhaA\student-crud
mvn clean install      # compiles, runs all 19 tests, packages the jar
mvn spring-boot:run    # starts the app on http://localhost:8080
```

Then:
- **Swagger UI** (interactive API docs): `http://localhost:8080/swagger-ui.html`
- **H2 console** (view the raw data): `http://localhost:8080/h2-console`
  (JDBC URL `jdbc:h2:file:./data/studentdb`, user `sa`, blank password)
- **Raw OpenAPI spec**: `http://localhost:8080/v3/api-docs`

A full demo walkthrough: create a student via Swagger's POST endpoint, confirm
it appears in GET all, update it via PUT, confirm the change, delete it via
DELETE, then confirm GET by that id now returns `404 Not Found` with the
structured error JSON from Section 4.6.

---

## 11. What's Left

- **Git repository creation and check-in** — not yet done. Next step: `git init`,
  commit, push to a remote (GitHub/GitLab/etc.).
- **MySQL migration** — the config is ready and commented out in
  `application.properties`; swapping back is a config change, not a code change,
  once a real MySQL instance is accessible.
- **Spring Boot 4.x migration** — recommended for long-term security patch
  coverage, but deliberately deferred as a separate, tested effort rather than
  bundled into this work (see Section 6).
