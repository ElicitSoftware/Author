# Contributing to Elicit Survey Platform

Thank you for your interest in contributing to the Elicit Survey Platform! This document provides guidelines and standards for contributing to the project.

> **Note on versioned entities:** several examples and the Domain Knowledge
> section below predate the shared schema's Kimball Type 2 versioning of
> structural tables. See `docs/scd-contract.md` and `docs/vision.md`'s
> Constraints for the current contract before writing code or docs that
> touch `questions`, `sections`, `steps`, `select_groups`, `select_items`,
> `relationships`, or their join tables.

## Table of Contents
- [Development Methodology](#development-methodology)
- [Technology Stack](#technology-stack)
- [Getting Started](#getting-started)
- [Coding Standards](#coding-standards)
- [Testing Requirements](#testing-requirements)
- [Pull Request Process](#pull-request-process)
- [Domain Knowledge](#domain-knowledge)

## Development Methodology

### Test-Driven Development (TDD) - MANDATORY

This project follows **strict Test-Driven Development** practices. All contributions must adhere to TDD principles:

#### The TDD Cycle
1. **Red** - Write a failing test first
2. **Green** - Write minimal code to make the test pass
3. **Refactor** - Improve code while keeping tests green

#### Coverage Requirements
- **Overall project**: 85%+ code coverage
- **Critical business logic**: 95%+ code coverage
- **All public methods**: Must have corresponding tests
- **Edge cases**: Must be tested

#### Test Types Required
- **Unit Tests**: All service methods, business logic, utilities
- **Integration Tests**: Database operations, external APIs
- **UI Component Tests**: Vaadin views and components
- **End-to-End Tests**: Critical user workflows

**❌ Pull requests without tests will be rejected.**

## Technology Stack

### Backend Technologies
- **Java**: 17 or later (LTS)
- **Framework**: Quarkus 3.x
- **ORM**: Hibernate with Panache
- **Database**: PostgreSQL 14+
- **Dependency Injection**: CDI (Jakarta EE)
- **REST APIs**: JAX-RS (RESTEasy)
- **Authentication**: OIDC/OAuth2

### Frontend Technologies
- **Framework**: Vaadin Flow 24.x
- **Language**: Java (server-side rendering)
- **Components**: Vaadin component library

### Testing Technologies
- **Unit Testing**: JUnit 5 (Jupiter)
- **Mocking**: Mockito
- **Assertions**: AssertJ
- **Database Testing**: Testcontainers (PostgreSQL)
- **UI Testing**: Vaadin TestBench
- **API Testing**: REST Assured
- **Coverage**: JaCoCo
- **Test Data**: Datafaker
- **Performance**: JMeter or Gatling

### Build & Tools
- **Build**: Maven
- **CI/CD**: GitHub Actions
- **Containers**: Docker
- **Version Control**: Git

## Getting Started

### Prerequisites
- Java 17+ JDK
- Maven 3.8+
- Docker (for Testcontainers)
- PostgreSQL 14+ (or use Docker)
- IDE with Vaadin support (IntelliJ IDEA recommended)

### Setup Development Environment

1. **Clone the repository**
```bash
git clone https://github.com/ElicitSoftware/Elicit.git
cd Elicit/Author
```

2. **Set up PostgreSQL** (or use Docker)
```bash
docker run -d --name elicit-postgres \
  -e POSTGRES_DB=elicit_survey \
  -e POSTGRES_USER=elicit \
  -e POSTGRES_PASSWORD=elicit123 \
  -p 5432:5432 postgres:14
```

3. **Configure application properties**
```properties
# src/main/resources/application.properties
quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=elicit
quarkus.datasource.password=elicit123
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/elicit_survey
```

4. **Run tests to verify setup**
```bash
mvn clean test
```

5. **Run the application**
```bash
mvn quarkus:dev
```

### Recommended IDE Setup

**IntelliJ IDEA:**
- Install Quarkus plugin
- Install Vaadin plugin
- Enable annotation processing
- Configure JaCoCo for coverage visualization

**VS Code:**
- Install Extension Pack for Java
- Install Quarkus extension
- Install Test Runner for Java

## Coding Standards

### Java Code Style

#### Naming Conventions
- **Classes**: PascalCase (e.g., `SurveyService`, `QuestionRepository`)
- **Methods**: camelCase (e.g., `createSurvey()`, `findQuestionById()`)
- **Variables**: camelCase (e.g., `surveyId`, `questionText`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `MAX_QUESTIONS`, `DEFAULT_TIMEOUT`)
- **Packages**: lowercase (e.g., `com.elicit.survey.service`)

#### Code Structure
- **Maximum method length**: 20 lines (extract to helper methods if longer)
- **Maximum class length**: 300 lines (consider splitting if longer)
- **Maximum parameters**: 4 (use parameter objects if more needed)
- **Cyclomatic complexity**: Maximum 10 per method

#### Java Features
- Use **Java 17+ features**: records, switch expressions, text blocks, pattern matching
- Prefer **immutability**: Use `final` for variables when possible
- Use `Optional<T>` for nullable return values
- Use records for DTOs and value objects
- Prefer method references over lambdas when appropriate

#### Example: Good Code
```java
@ApplicationScoped
public class SurveyService {
    private final SurveyRepository repository;

    @Inject
    public SurveyService(SurveyRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Survey createSurvey(CreateSurveyRequest request) {
        validateRequest(request);

        Survey survey = Survey.builder()
            .name(request.name())
            .description(request.description())
            .build();

        return repository.persist(survey);
    }

    private void validateRequest(CreateSurveyRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new ValidationException("Survey name is required");
        }
    }
}
```

### Entity/Model Classes

```java
@Entity
@Table(name = "surveys")
public class Survey extends PanacheEntity {

    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "name", nullable = false)
    private String name;

    @Size(max = 1000)
    @Column(name = "description")
    private String description;

    @OneToMany(mappedBy = "survey", cascade = CascadeType.ALL)
    private List<Question> questions = new ArrayList<>();

    // Getters, setters, equals, hashCode
}
```

### Repository Pattern

Use Panache repositories:

```java
@ApplicationScoped
public class SurveyRepository implements PanacheRepository<Survey> {

    public List<Survey> findByNameContaining(String searchTerm) {
        return find("LOWER(name) LIKE LOWER(?1)", "%" + searchTerm + "%").list();
    }

    // Illustrative only — NOT the pattern for Kimball Type 2 tables, which
    // have no boolean "active"/"is_current" column. For those, "current"
    // means effective_to = '9999-12-31 23:59:59+00'. See docs/scd-contract.md.
    public Optional<Survey> findActiveById(Long id) {
        return find("id = ?1 AND active = true", id).firstResultOptional();
    }
}
```

### Service Layer

```java
@ApplicationScoped
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final TokenReplacementService tokenService;

    @Inject
    public QuestionService(
        QuestionRepository questionRepository,
        TokenReplacementService tokenService
    ) {
        this.questionRepository = questionRepository;
        this.tokenService = tokenService;
    }

    @Transactional
    public Question createQuestion(QuestionDTO dto) {
        // Business logic here
    }
}
```

### Vaadin UI Code

```java
@Route(value = "surveys", layout = MainLayout.class)
@PageTitle("Surveys")
public class SurveyListView extends VerticalLayout {

    private final SurveyService surveyService;
    private final Grid<Survey> grid = new Grid<>(Survey.class);

    @Inject
    public SurveyListView(SurveyService surveyService) {
        this.surveyService = surveyService;

        configureGrid();
        configureLayout();
        updateList();
    }

    private void configureGrid() {
        grid.setColumns("name", "description", "createdDate");
        grid.addComponentColumn(this::createActionsColumn);
    }

    private void updateList() {
        grid.setItems(surveyService.findAll());
    }
}
```

## Testing Requirements

### Test Structure

Follow the **Arrange-Act-Assert (AAA)** pattern:

```java
@Test
void shouldCreateSurveyWithValidData() {
    // Arrange (Given)
    CreateSurveyRequest request = new CreateSurveyRequest(
        "Family Health History",
        "Comprehensive family health survey"
    );

    // Act (When)
    Survey survey = surveyService.createSurvey(request);

    // Assert (Then)
    assertThat(survey).isNotNull();
    assertThat(survey.getName()).isEqualTo("Family Health History");
    assertThat(survey.getId()).isGreaterThan(0L);
}
```

### Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class SurveyServiceTest {

    @Mock
    private SurveyRepository repository;

    @InjectMocks
    private SurveyService service;

    @Test
    void shouldThrowExceptionWhenSurveyNameIsBlank() {
        // Given
        CreateSurveyRequest request = new CreateSurveyRequest("", "Description");

        // When & Then
        assertThatThrownBy(() -> service.createSurvey(request))
            .isInstanceOf(ValidationException.class)
            .hasMessage("Survey name is required");
    }
}
```

### Integration Tests

```java
@QuarkusTest
@TestProfile(DatabaseTestProfile.class)
class SurveyServiceIntegrationTest {

    @Inject
    SurveyService surveyService;

    @Test
    @Transactional
    void shouldPersistSurveyToDatabase() {
        // Given
        CreateSurveyRequest request = new CreateSurveyRequest(
            "Test Survey",
            "Description"
        );

        // When
        Survey survey = surveyService.createSurvey(request);

        // Then
        Survey found = Survey.findById(survey.getId());
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("Test Survey");
    }
}
```

### UI Component Tests

```java
@Test
void shouldRenderSurveyGridWithData() {
    // Given
    List<Survey> surveys = List.of(
        createSurvey("Survey 1"),
        createSurvey("Survey 2")
    );
    when(surveyService.findAll()).thenReturn(surveys);

    // When
    SurveyListView view = new SurveyListView(surveyService);

    // Then
    assertThat(view.getGrid().getColumns()).hasSize(4);
    assertThat(view.getGrid().getListDataView().getItems()).hasSize(2);
}
```

### Test Naming Convention

Use descriptive test names that explain what is being tested:

- `shouldCreateSurveyWithValidData()`
- `shouldThrowExceptionWhenNameIsBlank()`
- `shouldReturnEmptyOptionalWhenSurveyNotFound()`
- `shouldUpdateQuestionTextWithTokenReplacement()`
- `shouldCreateDraftQuestionVersionWithRevisedText()` *(for versioned entities — "update" is
  never in-place; see `docs/scd-contract.md`)*
- `shouldPublishDraftQuestionVersion()`

### Running Tests

```bash
# Run all tests
mvn test

# Run tests with coverage
mvn test jacoco:report

# Run specific test class
mvn test -Dtest=SurveyServiceTest

# Run tests in specific package
mvn test -Dtest=com.elicit.survey.service.*

# Run integration tests only
mvn verify -Pintegration-tests
```

## Pull Request Process

### Before Submitting

1. **Write tests first** - Follow TDD methodology
2. **Run all tests** - Ensure they all pass
3. **Check coverage** - Meet 85%+ requirement
4. **Run code analysis** - Fix any issues
5. **Update documentation** - If adding new features
6. **Follow commit conventions** - Use conventional commits

### Commit Message Format

Use [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `test`: Adding or updating tests
- `refactor`: Code refactoring
- `docs`: Documentation changes
- `style`: Code style changes (formatting)
- `perf`: Performance improvements
- `chore`: Build process or auxiliary tool changes

**Examples:**
```
feat(survey): add token replacement service

Implements token parsing and replacement logic for dynamic question text.
Supports all token types: S1, G1, C1, CS2, R1.

Closes #123

test(survey): add tests for conditional logic evaluation

refactor(question): extract validation logic to separate class
```

### Pull Request Template

When creating a PR, include:

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Tests written first (TDD)
- [ ] All tests pass
- [ ] Coverage meets 85%+ requirement
- [ ] Integration tests added

## Checklist
- [ ] Code follows project style guidelines
- [ ] Self-review completed
- [ ] Documentation updated
- [ ] No new warnings introduced
```

### Code Review Guidelines

Reviewers will check:
- ✅ Tests written before implementation
- ✅ All tests pass
- ✅ Code coverage meets requirements
- ✅ Code follows style guidelines
- ✅ Edge cases tested
- ✅ Error handling tested
- ✅ Documentation updated

## Domain Knowledge

### Survey Platform Concepts

#### Display Keys
Hierarchical addressing system for survey elements:
```
survey.step.step_instance.section.section_instance.question.question_instance
Example: 0001-0002-0001-0003-0002-0015-0001
```

> **Not the same as SCD "version".** The `_instance` segments above number
> *repeated* occurrences of a step/section/question within one published
> structural version (e.g. a repeating "list another diagnosis" section).
> They are unrelated to a structural element's Kimball Type 2 `version`
> (which numbers revisions of the element's definition over time). A single
> Display Key addresses one instance of one specific version of a step,
> section, or question. Do not use "instance" and "version" interchangeably
> in UI copy or code.

#### Token System
Dynamic text replacement for personalized questions. A placeholder is written
`{phrase containing a <TOKEN>|default text}` (UC-045 BR-003): the token is the
upper-case name in angle brackets, and the phrase and the default around it are
prose. A phrase is filled all-or-nothing — every reference in it must have a
value, or the placeholder falls back to its default. A rule's `token` column
stores the name alone, upper case (`S1`); the angle brackets belong to the text
that names it, and a token stored with them fills nothing.

The Family History Survey's tokens:
- `{<S1>|default}` - Subject/person identifier
- `{<G1>|default}` - General relationship term
- `{<C1>|default}` - Category/condition reference
- `{<CS2>|default}` - Category-subject combination
- `{<R1>|default}` - Relationship context

**Example:**
```
Question: "What is {<S1>'s|your} age?"
Renders as: "What is John's age?" (when S1 = "John")
           "What is your age?"    (when no rule fills S1)
```

The undelimited `{S1|default}` form is obsolete. `ExportValidation` reports a
placeholder that names a known token as a bare word as an **error** that blocks
export.

#### Conditional Logic
- **Operators**: BOOLEAN, GREATER_THAN, EQUAL, NOT_EQUAL, FIELD_EXIST, CONTAINS
- **Actions**: SHOW (display question), REPEAT (replicate section), TEXT (replace token)

#### Metadata & Dimensional Modeling
- **Ontology tags** → Automatically create `dim_*` dimension tables
- ETL service generates star schema from metadata assignments
- Example: `'Age'` ontology tag → `dim_age` table

### Database Schema Key Tables

**Core:**
- `surveys`, `steps`, `sections`, `questions`
- `question_types`, `select_groups`, `select_items`

**Logic:**
- `relationships`, `dependencies`, `operators`, `actions`

**Analytics:**
- `dimensions`, `ontology`, `metadata`
- `dim_*` (auto-generated), `fact_sections`, `fact_respondents`

**Responses:**
- `respondents`, `answers`

> These lists predate the Kimball Type 2 versioning columns now present on
> `questions`, `select_groups`, `select_items`, `sections`, `steps`,
> `sections_questions`, `steps_sections`, and `relationships` (durable key,
> element key `{entity}_key`, `version`, `effective_from`/`effective_to`,
> `published_by`, `published_comment`). See `docs/scd-contract.md` for the
> current contract and for why Author treats its rows as plain working copies.
> Note also: the "Logic" table list above says `dependencies`; confirm the
> actual table name (`dependents`, per the Survey schema) at entity-model
> time.

## Getting Help

- **Documentation**: `/docs` directory
- **Development Plan**: `survey-authoring-development-plan.md`
- **Survey Creation Guide**: `creating-fhhs-style-surveys.md`
- **Issues**: GitHub Issues
- **Discussions**: GitHub Discussions

## License

By contributing, you agree that your contributions will be licensed under the same license as the project.

---

**Thank you for contributing to the Elicit Survey Platform!** 🎉
