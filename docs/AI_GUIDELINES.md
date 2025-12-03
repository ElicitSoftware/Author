# AI Assistant Guidelines for Elicit Survey Platform

This document provides comprehensive guidance for AI coding assistants (GitHub Copilot, Cursor, ChatGPT, Claude, etc.) when working with the Elicit Survey Platform codebase.

## Quick Reference

### MCP Servers for Enhanced Context

Use these Model Context Protocol servers when working on specific areas:

#### Database & Data Layer
- **`@postgres`** - PostgreSQL-specific queries, functions, indexing, performance optimization
- **`@database`** - General database design patterns, schema migrations, data modeling
- **`@hibernate`** - Hibernate ORM, JPA annotations, query optimization, caching strategies

#### Backend Framework & APIs
- **`@quarkus`** - Quarkus framework, extensions, configuration, build optimization
- **`@jakarta-ee`** - Jakarta EE specifications (CDI, Bean Validation, JAX-RS, JPA)
- **`@jax-rs`** - REST API design, HTTP methods, response codes, API versioning

#### Frontend UI
- **`@vaadin`** - Vaadin Flow components, layouts, data binding, routing, theming

#### Testing & Quality
- **`@junit`** - JUnit 5 testing patterns, parameterized tests, test lifecycle
- **`@mockito`** - Mocking strategies, stubbing, verification, argument captors
- **`@testcontainers`** - Container-based integration testing, database testing

#### Build & Deployment
- **`@maven`** - Maven configuration, dependency management, plugins, profiles
- **`@docker`** - Containerization, Dockerfile optimization, multi-stage builds

#### Development Tools
- **`@git`** - Version control, branching strategies, commit conventions
- **`@github-actions`** - CI/CD pipelines, workflow automation

## Core Development Philosophy

### Test-Driven Development (Non-Negotiable)

Every single code contribution must follow TDD:

1. **Write the test FIRST** - No exceptions
2. **Watch it fail** - Verify the test actually tests something
3. **Write minimal code** - Just enough to pass the test
4. **Refactor** - Improve while keeping tests green
5. **Repeat** - For each new behavior or feature

### Coverage Requirements

| Code Type | Minimum Coverage |
|-----------|------------------|
| Overall Project | 85% |
| Business Logic | 95% |
| Service Layer | 90% |
| Repository Layer | 85% |
| Utility Functions | 100% |

## AI Code Generation Workflow

### Step 1: Analyze Requirements
Before generating any code, understand:
- What is the business requirement?
- What are the acceptance criteria?
- What edge cases exist?
- What are the dependencies?

### Step 2: Generate Test Suite FIRST

```java
// ALWAYS START HERE - Generate the test class
@QuarkusTest
class SurveyServiceTest {

    @Inject
    SurveyService surveyService;

    @Test
    void shouldCreateSurveyWithValidData() {
        // Given
        CreateSurveyRequest request = new CreateSurveyRequest(
            "Family Health History",
            "Comprehensive family cancer history survey"
        );

        // When
        Survey survey = surveyService.createSurvey(request);

        // Then
        assertThat(survey).isNotNull();
        assertThat(survey.getId()).isGreaterThan(0L);
        assertThat(survey.getName()).isEqualTo("Family Health History");
    }

    @Test
    void shouldThrowExceptionWhenNameIsNull() {
        // Given
        CreateSurveyRequest request = new CreateSurveyRequest(null, "Description");

        // When & Then
        assertThatThrownBy(() -> surveyService.createSurvey(request))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Survey name is required");
    }

    @Test
    void shouldThrowExceptionWhenNameIsBlank() {
        // Given
        CreateSurveyRequest request = new CreateSurveyRequest("   ", "Description");

        // When & Then
        assertThatThrownBy(() -> surveyService.createSurvey(request))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Survey name is required");
    }

    @Test
    void shouldHandleLongDescriptions() {
        // Given
        String longDescription = "A".repeat(1000);
        CreateSurveyRequest request = new CreateSurveyRequest("Survey", longDescription);

        // When
        Survey survey = surveyService.createSurvey(request);

        // Then
        assertThat(survey.getDescription()).hasSize(1000);
    }
}
```

### Step 3: Generate Interface/Contract

```java
// SECOND - Define the API contract
public interface SurveyService {

    /**
     * Creates a new survey with the provided details.
     *
     * @param request the survey creation request containing name and description
     * @return the created survey with generated ID
     * @throws ValidationException if the request data is invalid
     */
    Survey createSurvey(CreateSurveyRequest request);

    /**
     * Finds a survey by its unique identifier.
     *
     * @param id the survey ID
     * @return Optional containing the survey if found, empty otherwise
     */
    Optional<Survey> findById(Long id);

    /**
     * Lists all surveys accessible to the current user.
     *
     * @return list of surveys, never null
     */
    List<Survey> findAll();
}
```

### Step 4: Generate Implementation

```java
// THIRD - Implement to satisfy tests
@ApplicationScoped
public class SurveyServiceImpl implements SurveyService {

    private final SurveyRepository repository;
    private final ValidationService validationService;

    @Inject
    public SurveyServiceImpl(
        SurveyRepository repository,
        ValidationService validationService
    ) {
        this.repository = repository;
        this.validationService = validationService;
    }

    @Override
    @Transactional
    public Survey createSurvey(CreateSurveyRequest request) {
        validateRequest(request);

        Survey survey = buildSurvey(request);
        repository.persist(survey);

        return survey;
    }

    private void validateRequest(CreateSurveyRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new ValidationException("Survey name is required");
        }

        validationService.validateSurveyName(request.name());
    }

    private Survey buildSurvey(CreateSurveyRequest request) {
        Survey survey = new Survey();
        survey.setName(request.name().trim());
        survey.setDescription(request.description());
        survey.setCreatedDate(LocalDateTime.now());
        return survey;
    }

    @Override
    public Optional<Survey> findById(Long id) {
        return repository.findByIdOptional(id);
    }

    @Override
    public List<Survey> findAll() {
        return repository.listAll();
    }
}
```

### Step 5: Generate Integration Tests

```java
// FOURTH - Test with real dependencies
@QuarkusTest
@TestProfile(DatabaseTestProfile.class)
class SurveyServiceIntegrationTest {

    @Inject
    SurveyService surveyService;

    @BeforeEach
    @Transactional
    void setUp() {
        Survey.deleteAll();
    }

    @Test
    @Transactional
    void shouldPersistSurveyToDatabase() {
        // Given
        CreateSurveyRequest request = new CreateSurveyRequest(
            "Integration Test Survey",
            "Testing database persistence"
        );

        // When
        Survey created = surveyService.createSurvey(request);
        Survey found = Survey.findById(created.getId());

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("Integration Test Survey");
        assertThat(found.getCreatedDate()).isNotNull();
    }

    @Test
    @Transactional
    void shouldPreventDuplicateSurveyNames() {
        // Given
        CreateSurveyRequest request1 = new CreateSurveyRequest("Unique Survey", "Desc");
        CreateSurveyRequest request2 = new CreateSurveyRequest("Unique Survey", "Desc");
        surveyService.createSurvey(request1);

        // When & Then
        assertThatThrownBy(() -> surveyService.createSurvey(request2))
            .isInstanceOf(DuplicateNameException.class);
    }
}
```

## Domain-Specific Patterns

### Working with Survey Entities

```java
// Entity with validation
@Entity
@Table(name = "surveys", uniqueConstraints = {
    @UniqueConstraint(columnNames = "name")
})
public class Survey extends PanacheEntity {

    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Size(max = 1000)
    @Column(name = "description")
    private String description;

    @NotNull
    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @OneToMany(mappedBy = "survey", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Question> questions = new ArrayList<>();

    // Builder pattern for easy test data creation
    public static class Builder {
        private String name;
        private String description;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Survey build() {
            Survey survey = new Survey();
            survey.setName(name);
            survey.setDescription(description);
            survey.setCreatedDate(LocalDateTime.now());
            return survey;
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
```

### Implementing Token Replacement

```java
// Test-first token replacement
@Test
void shouldReplaceS1TokenWithPersonName() {
    // Given
    String template = "What is {S1|your}'s age?";
    Map<String, String> tokens = Map.of("S1", "John");

    // When
    String result = tokenService.replaceTokens(template, tokens);

    // Then
    assertThat(result).isEqualTo("What is John's age?");
}

@Test
void shouldUseDefaultValueWhenTokenNotProvided() {
    // Given
    String template = "What is {S1|your}'s age?";
    Map<String, String> tokens = Map.of();

    // When
    String result = tokenService.replaceTokens(template, tokens);

    // Then
    assertThat(result).isEqualTo("What is your's age?");
}

@Test
void shouldHandlePossessiveGrammarCorrectly() {
    // Given
    String template = "What is {S1|you}'s age?";
    Map<String, String> tokens = Map.of("S1", "Mary");

    // When
    String result = tokenService.replaceTokens(template, tokens);

    // Then
    assertThat(result).isEqualTo("What is Mary's age?");
}

// Implementation
@ApplicationScoped
public class TokenReplacementService {

    private static final Pattern TOKEN_PATTERN =
        Pattern.compile("\\{([A-Z0-9]+)\\|([^}]+)\\}");

    public String replaceTokens(String template, Map<String, String> tokens) {
        if (template == null) {
            return null;
        }

        Matcher matcher = TOKEN_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String tokenName = matcher.group(1);
            String defaultValue = matcher.group(2);
            String replacement = tokens.getOrDefault(tokenName, defaultValue);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        return result.toString();
    }
}
```

### Implementing Conditional Logic

```java
// Test relationship evaluation
@ExtendWith(MockitoExtension.class)
class RelationshipEvaluatorTest {

    @Mock
    private AnswerRepository answerRepository;

    @InjectMocks
    private RelationshipEvaluator evaluator;

    @Test
    void shouldShowQuestionWhenConditionIsTrue() {
        // Given
        Relationship relationship = createShowRelationship();
        relationship.setOperator(Operator.EQUAL);
        relationship.setReferenceValue("Yes");

        when(answerRepository.findAnswer(any(), any()))
            .thenReturn(Optional.of(createAnswer("Yes")));

        // When
        boolean result = evaluator.evaluate(relationship, "respondent-123");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void shouldHideQuestionWhenConditionIsFalse() {
        // Given
        Relationship relationship = createShowRelationship();
        relationship.setOperator(Operator.EQUAL);
        relationship.setReferenceValue("Yes");

        when(answerRepository.findAnswer(any(), any()))
            .thenReturn(Optional.of(createAnswer("No")));

        // When
        boolean result = evaluator.evaluate(relationship, "respondent-123");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void shouldHandleGreaterThanOperator() {
        // Given
        Relationship relationship = createShowRelationship();
        relationship.setOperator(Operator.GREATER_THAN);
        relationship.setReferenceValue("18");

        when(answerRepository.findAnswer(any(), any()))
            .thenReturn(Optional.of(createAnswer("25")));

        // When
        boolean result = evaluator.evaluate(relationship, "respondent-123");

        // Then
        assertThat(result).isTrue();
    }
}
```

### Vaadin UI Components

```java
// Test Vaadin view
class SurveyListViewTest {

    @Mock
    private SurveyService surveyService;

    private SurveyListView view;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        view = new SurveyListView(surveyService);
    }

    @Test
    void shouldDisplaySurveysInGrid() {
        // Given
        List<Survey> surveys = List.of(
            createSurvey("Survey 1"),
            createSurvey("Survey 2")
        );
        when(surveyService.findAll()).thenReturn(surveys);

        // When
        view.refreshGrid();

        // Then
        assertThat(view.getGrid().getListDataView().getItemCount()).isEqualTo(2);
    }

    @Test
    void shouldOpenEditorWhenCreateButtonClicked() {
        // When
        view.getCreateButton().click();

        // Then
        assertThat(view.getEditor().isOpened()).isTrue();
    }
}

// Implementation
@Route(value = "surveys", layout = MainLayout.class)
@PageTitle("Surveys | Elicit")
public class SurveyListView extends VerticalLayout {

    private final SurveyService surveyService;
    private final Grid<Survey> grid = new Grid<>(Survey.class, false);
    private final Button createButton = new Button("Create Survey");
    private final SurveyEditor editor;

    @Inject
    public SurveyListView(SurveyService surveyService) {
        this.surveyService = surveyService;
        this.editor = new SurveyEditor(surveyService);

        configureGrid();
        configureLayout();
        refreshGrid();
    }

    private void configureGrid() {
        grid.addColumn(Survey::getName).setHeader("Name").setSortable(true);
        grid.addColumn(Survey::getDescription).setHeader("Description");
        grid.addColumn(this::formatDate).setHeader("Created");
        grid.addComponentColumn(this::createActionsColumn).setHeader("Actions");

        grid.asSingleSelect().addValueChangeListener(e -> editSurvey(e.getValue()));
    }

    private void configureLayout() {
        HorizontalLayout toolbar = new HorizontalLayout(createButton);
        createButton.addClickListener(e -> createSurvey());

        add(toolbar, grid, editor);
        setSizeFull();
    }

    public void refreshGrid() {
        grid.setItems(surveyService.findAll());
    }

    private void createSurvey() {
        grid.asSingleSelect().clear();
        editor.editSurvey(new Survey());
    }

    private void editSurvey(Survey survey) {
        if (survey != null) {
            editor.editSurvey(survey);
        }
    }

    // Getters for testing
    public Grid<Survey> getGrid() { return grid; }
    public Button getCreateButton() { return createButton; }
    public SurveyEditor getEditor() { return editor; }
}
```

## Anti-Patterns to Avoid

### ❌ Don't Generate Code Without Tests

```java
// WRONG - Implementation without tests
@ApplicationScoped
public class SurveyService {
    public Survey createSurvey(String name) {
        // TODO: Implement
        return null;
    }
}
```

### ❌ Don't Use Placeholders

```java
// WRONG - Placeholder comments
public class TokenService {
    public String replaceTokens(String text) {
        // TODO: Parse tokens
        // TODO: Replace with values
        // TODO: Handle edge cases
        return text;
    }
}
```

### ❌ Don't Skip Edge Cases

```java
// WRONG - Only happy path tested
@Test
void shouldCreateSurvey() {
    Survey survey = service.createSurvey(new CreateRequest("Name", "Desc"));
    assertThat(survey).isNotNull();
}

// RIGHT - Test edge cases
@Test
void shouldCreateSurvey() { /* happy path */ }

@Test
void shouldRejectNullName() { /* null test */ }

@Test
void shouldRejectBlankName() { /* blank test */ }

@Test
void shouldRejectTooLongName() { /* length test */ }

@Test
void shouldHandleSpecialCharactersInName() { /* special chars */ }
```

### ❌ Don't Ignore Integration Tests

```java
// WRONG - Only unit tests
// Need both unit AND integration tests

// RIGHT - Both types
@QuarkusTest
class ServiceTest { /* unit tests with mocks */ }

@QuarkusTest
@TestProfile(DatabaseTestProfile.class)
class ServiceIntegrationTest { /* integration tests */ }
```

## Performance Considerations

### Database Query Optimization

```java
// Test query performance
@Test
void shouldFetchSurveysWithQuestionsEfficiently() {
    // Given
    createSurveyWithQuestions(100); // 100 questions

    // When
    long startTime = System.currentTimeMillis();
    List<Survey> surveys = repository.findAllWithQuestions();
    long duration = System.currentTimeMillis() - startTime;

    // Then
    assertThat(surveys).hasSize(1);
    assertThat(surveys.get(0).getQuestions()).hasSize(100);
    assertThat(duration).isLessThan(100); // Should be fast (no N+1)
}

// Optimized query
@ApplicationScoped
public class SurveyRepository implements PanacheRepository<Survey> {

    public List<Survey> findAllWithQuestions() {
        return find(
            "SELECT DISTINCT s FROM Survey s LEFT JOIN FETCH s.questions"
        ).list();
    }
}
```

### Caching Strategies

```java
// Test caching
@Test
void shouldCacheFrequentlyAccessedSurveys() {
    // Given
    Survey survey = createSurvey("Cached Survey");

    // When - First call hits database
    Survey first = service.findById(survey.getId());
    Survey second = service.findById(survey.getId());

    // Then - Second call from cache
    verify(repository, times(1)).findById(survey.getId());
    assertThat(first).isSameAs(second);
}

// Cached service method
@ApplicationScoped
public class SurveyService {

    @CacheResult(cacheName = "surveys")
    public Survey findById(Long id) {
        return repository.findByIdOptional(id)
            .orElseThrow(() -> new SurveyNotFoundException(id));
    }

    @CacheInvalidate(cacheName = "surveys")
    public Survey updateSurvey(Long id, UpdateRequest request) {
        // Update logic
    }
}
```

## Error Handling Patterns

```java
// Custom exceptions
public class SurveyNotFoundException extends RuntimeException {
    public SurveyNotFoundException(Long id) {
        super("Survey not found with ID: " + id);
    }
}

public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}

// Global exception handler
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        if (exception instanceof ValidationException) {
            return Response
                .status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(exception.getMessage()))
                .build();
        }

        if (exception instanceof SurveyNotFoundException) {
            return Response
                .status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse(exception.getMessage()))
                .build();
        }

        // Log unexpected errors
        Log.error("Unexpected error", exception);

        return Response
            .status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(new ErrorResponse("An unexpected error occurred"))
            .build();
    }
}
```

## Summary Checklist

When generating code, verify:

- ✅ Tests written FIRST
- ✅ All tests pass (green)
- ✅ 85%+ code coverage
- ✅ Edge cases tested
- ✅ Error scenarios tested
- ✅ Integration tests for database operations
- ✅ No placeholder comments
- ✅ Complete implementations
- ✅ Proper error handling
- ✅ Documentation/Javadoc added
- ✅ Follows naming conventions
- ✅ Uses appropriate design patterns
- ✅ Performance considered
- ✅ Security validated

---

**Remember: If it's not tested, it's not done. Write tests first, always.**
