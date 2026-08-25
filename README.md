# Student CRUD - Spring Boot Application

A Spring Boot REST API to **Store / Update / Retrieve / Delete** student records
(`id`, `name`, `department`) backed by **MySQL**, documented with **Swagger (springdoc-openapi)**,
and covered by **JUnit 5 / Mockito** unit tests + a full MockMvc integration test.

---

## Tech Stack
| Layer            | Technology                                  |
|-------------------|----------------------------------------------|
| Language          | Java 17                                      |
| Framework         | Spring Boot 3.3.4 (Web, Data JPA, Validation)|
| Build tool        | Maven                                        |
| Database          | MySQL 8 (H2 in-memory for tests)             |
| API docs          | springdoc-openapi / Swagger UI               |
| Testing           | JUnit 5, Mockito, MockMvc, AssertJ           |
| Boilerplate       | Lombok                                       |

---

## Project Structure
```
student-crud/
├── pom.xml
├── schema.sql                       # optional manual DDL + sample data
├── README.md
└── src
    ├── main
    │   ├── java/com/example/studentcrud
    │   │   ├── StudentCrudApplication.java
    │   │   ├── config/OpenApiConfig.java
    │   │   ├── controller/StudentController.java
    │   │   ├── dto/StudentDTO.java
    │   │   ├── entity/Student.java
    │   │   ├── exception/ (ResourceNotFoundException, GlobalExceptionHandler, ErrorResponse)
    │   │   ├── repository/StudentRepository.java
    │   │   └── service/ (StudentService, impl/StudentServiceImpl)
    │   └── resources/application.properties
    └── test
        ├── java/com/example/studentcrud
        │   ├── StudentCrudApplicationTests.java     # full integration test (H2)
        │   ├── controller/StudentControllerTest.java # @WebMvcTest
        │   └── service/StudentServiceImplTest.java   # Mockito unit test
        └── resources/application-test.properties     # H2 config for tests
```

---

## 1. JDK Setup

1. Install **JDK 17** (project targets Java 17):
   - Windows/Mac/Linux: download from [Eclipse Temurin](https://adoptium.net/) or use a version manager (`sdkman`, `asdf`).
   - Via SDKMAN: `sdk install java 17.0.12-tem`
2. Verify installation:
   ```bash
   java -version
   javac -version
   ```
3. Set `JAVA_HOME` to point to the JDK 17 install directory (usually done automatically by the installer / SDKMAN).

---

## 2. Maven Setup

The project uses standard Maven (no wrapper is bundled, but you can generate one).

1. Install Maven 3.9+:
   - Mac: `brew install maven`
   - Windows: download from [maven.apache.org](https://maven.apache.org/download.cgi) and add `bin` to `PATH`
   - Linux: `sudo apt install maven` (or use SDKMAN: `sdk install maven`)
2. Verify:
   ```bash
   mvn -version
   ```
3. (Optional) Generate the Maven wrapper so teammates don't need Maven installed globally:
   ```bash
   mvn -N io.takari:maven:wrapper
   ```
4. Build the project (downloads all dependencies from Maven Central):
   ```bash
   mvn clean install
   ```

---

## 3. IDE Setup

Any IDE works; steps below cover the two most common.

### IntelliJ IDEA
1. `File → Open` → select the `student-crud` folder (the one containing `pom.xml`).
2. IntelliJ auto-detects it as a Maven project and downloads dependencies.
3. Enable annotation processing for Lombok:
   `Settings → Build, Execution, Deployment → Compiler → Annotation Processors → Enable annotation processing`.
4. Install the **Lombok plugin** (`Settings → Plugins → search "Lombok" → Install`) if not bundled.
5. Set Project SDK to **17**: `File → Project Structure → Project → SDK`.
6. Run `StudentCrudApplication.java` (right-click → Run), or use the Maven side panel.

### Eclipse / Spring Tool Suite (STS)
1. `File → Import → Maven → Existing Maven Projects` → select `student-crud`.
2. Install the **Lombok** jar (`java -jar lombok.jar` → point to your Eclipse install) and restart.
3. Set the project's Java Build Path to JDK 17.
4. Right-click `StudentCrudApplication.java → Run As → Spring Boot App`.

### VS Code
1. Install extensions: *Extension Pack for Java*, *Spring Boot Extension Pack*, *Lombok Annotations Support*.
2. Open the `student-crud` folder.
3. Use the Spring Boot Dashboard or `Run → Start Debugging` on `StudentCrudApplication`.

---

## 4. Database Setup (MySQL)

1. Install MySQL 8 locally, or run it via Docker:
   ```bash
   docker run --name mysql-student -e MYSQL_ROOT_PASSWORD=root -p 3306:3306 -d mysql:8.0
   ```
2. The app is configured (see `src/main/resources/application.properties`) to:
   - connect to `jdbc:mysql://localhost:3306/studentdb`
   - auto-create the database (`createDatabaseIfNotExist=true`)
   - auto-create/update the `student` table (`spring.jpa.hibernate.ddl-auto=update`)

   So **no manual schema step is required** — just make sure MySQL is running and the
   credentials in `application.properties` match your instance:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/studentdb?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
   spring.datasource.username=root
   spring.datasource.password=root
   ```
3. **Optional**: if you'd rather create the schema yourself, run `schema.sql` manually:
   ```bash
   mysql -u root -p < schema.sql
   ```
   `schema.sql` also has two sample rows for a quick demo.

4. Unit/integration tests do **not** need MySQL — they run against an in-memory
   **H2** database via the `test` Spring profile (`src/test/resources/application-test.properties`),
   so `mvn test` works out of the box on any machine.

---

## 5. Unit Test Cases

Tests included:

| Test class                       | Type                      | Covers |
|-----------------------------------|----------------------------|--------|
| `StudentServiceImplTest`          | Unit (Mockito)             | create, get-by-id (found/not-found), get-all, update (found/not-found), delete (found/not-found) |
| `StudentControllerTest`           | Web slice (`@WebMvcTest` + MockMvc) | all 5 endpoints, success + validation (400) + not-found (404) paths |
| `StudentCrudApplicationTests`     | Integration (`@SpringBootTest` + H2) | Spring context loads; full create → read → update → delete lifecycle through real HTTP calls |

Run all tests:
```bash
mvn test
```

Run a single test class:
```bash
mvn -Dtest=StudentServiceImplTest test
```

Generate a surefire report at `target/surefire-reports/`.

---

## 6. Git Repo Creation & Code Check-in

From inside the `student-crud` folder:

```bash
git init
git add .
git commit -m "Initial commit: Student CRUD Spring Boot app with Swagger and unit tests"

# Create an empty repo on GitHub/GitLab/Bitbucket first, then:
git remote add origin <YOUR_REMOTE_REPO_URL>
git branch -M main
git push -u origin main
```

`.gitignore` is already included so `target/`, IDE metadata, and logs won't be committed.

Suggested branching workflow for future changes:
```bash
git checkout -b feature/<change-name>
# make changes
git add .
git commit -m "Description of change"
git push -u origin feature/<change-name>
# open a Pull Request into main
```

---

## 7. Demo

### Run the application
```bash
mvn spring-boot:run
```
The app starts on **http://localhost:8080**.

### Swagger UI (API documentation & try-it-out)
Open in a browser:
```
http://localhost:8080/swagger-ui.html
```
Raw OpenAPI JSON spec:
```
http://localhost:8080/v3/api-docs
```

### API Endpoints

| Method | Endpoint                     | Description              | Sample Body |
|--------|-------------------------------|---------------------------|-------------|
| POST   | `/api/v1/students`             | Create a student           | `{"name":"John Doe","department":"Computer Science"}` |
| GET    | `/api/v1/students/{id}`        | Retrieve one student       | – |
| GET    | `/api/v1/students`              | Retrieve all students      | – |
| PUT    | `/api/v1/students/{id}`        | Update a student            | `{"name":"John Updated","department":"Electrical"}` |
| DELETE | `/api/v1/students/{id}`        | Delete a student             | – |

### Example curl walkthrough
```bash
# Create
curl -X POST http://localhost:8080/api/v1/students \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","department":"Computer Science"}'

# Retrieve by id (replace 1 with the returned id)
curl http://localhost:8080/api/v1/students/1

# Retrieve all
curl http://localhost:8080/api/v1/students

# Update
curl -X PUT http://localhost:8080/api/v1/students/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"John Updated","department":"Electrical Engineering"}'

# Delete
curl -X DELETE http://localhost:8080/api/v1/students/1
```

### Error handling demo
- `GET /api/v1/students/999` (non-existent id) → `404 Not Found` with a JSON error body
  (`timestamp`, `status`, `error`, `message`, `path`).
- `POST /api/v1/students` with `{"name":"","department":"CS"}` → `400 Bad Request` with
  field-level `validationErrors`.

---

## Dependency Security Fixes

A dependency scan (Mend.io / WhiteSource) flagged transitive vulnerabilities pulled in by the
original Spring Boot 3.3.4 baseline. These have been resolved as follows:

| Vulnerability | Vulnerable dependency | Fix applied |
|---|---|---|
| CVE-2024-38819 (info disclosure), CVE-2025-41242 (path traversal), CVE-2026-22737 / CVE-2026-22741 / CVE-2026-22735 | `spring-webmvc:6.1.13` (via Spring Boot 3.3.4, Framework 6.1 — EOL since June 2025) | Bumped the parent to **`spring-boot-starter-parent:3.5.16`** (final 3.x release, built on Spring Framework 6.2, which received fixes for all of these before its June 2026 EOL) |
| WS-2026-0003 | `jackson-core:2.17.2` (bundled with Boot 3.3.4) | Resolved automatically by the same parent bump — 3.5.16 manages a much newer Jackson line |
| CVE-2025-48924 (uncontrolled recursion / DoS in `ClassUtils.getClass`) | `commons-lang3:3.14.0` (transitive via springdoc/swagger-core) | Pinned explicitly via the `<commons-lang3.version>3.18.0</commons-lang3.version>` property, since Spring Boot's own BOM doesn't dictate this on its own |

Two related changes were needed to keep everything compatible with Framework 6.2:
- **`springdoc-openapi-starter-webmvc-ui`** bumped from `2.6.0` → `2.8.17` (latest release on the 2.8.x line that targets Spring Boot 3.5 / Framework 6.2 — springdoc's 3.x line is for Spring Boot 4/Framework 7 only).
- **`spring.jpa.database-platform`** changed from the deprecated `MySQL8Dialect` to `MySQLDialect`, since Hibernate 6.6 (bundled with Boot 3.5) deprecated the versioned dialect classes.

**Important:** Spring Boot 3.3, 3.4, and now 3.5 have all reached open-source end-of-life
(3.5's final patch, 3.5.16, shipped June 25, 2026). This project is pinned to 3.5.16 as the
most compatible, least-breaking fix available on the 3.x line — but 3.5.16 itself will not
receive further CVE patches. For a codebase that needs ongoing security coverage, the
recommended next step is migrating to **Spring Boot 4.1.x** (the actively-supported release,
built on Spring Framework 7). That migration is more involved (Jackson 2→3, some relocated
framework classes, Undertow support removed, etc.), so it's worth doing as a deliberate,
tested follow-up rather than folding it into this fix.

I couldn't run `mvn` in the sandbox that generated this project (no network access), so please
run `mvn clean install` after pulling these changes to confirm everything still compiles and
all tests still pass on your machine.

## Notes / Production Considerations
- `spring.jpa.hibernate.ddl-auto=update` is convenient for a demo; in production prefer
  `validate` and manage schema changes with Flyway/Liquibase.
- Move DB credentials to environment variables / a secrets manager instead of
  `application.properties` before deploying anywhere real.
- Add pagination to `GET /api/v1/students` if the dataset can grow large.
