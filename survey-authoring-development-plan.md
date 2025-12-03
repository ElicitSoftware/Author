# Survey Authoring Application Development Plan

## Project Overview
Development of a comprehensive survey authoring application for the Elicit platform. This application will enable researchers, educators, business analysts, and survey creators across all domains to build sophisticated surveys through visual interfaces, utilizing Elicit's powerful survey infrastructure and advanced capabilities including complex logic, dynamic content, and dimensional analytics.

## Database Analysis - COMPLETED ✅

### Current Database Schema Overview
The Elicit survey platform uses a PostgreSQL database with a comprehensive schema that supports surveys across all domains and industries:

#### Step 2: Plan Dimensional Framework
- **Understand automatic dimension creation**
  - Dimensions are automatically created based on ontology tags
  - `dim_age` tables created from 'Age' ontology tags
  - `dim_category` tables created from categorical data
  - `dim_rating` tables created from rating scale data

- **Plan ontology structure**
  - Any categorical data (departments, products, locations)
  - Research variables (conditions, treatments, outcomes)
  - Demographic data (age groups, education levels, income brackets)
  - Each ontology tag becomes a dimension table automatically

- **Design metadata strategy**
  - Plan which questions need dimensional tracking
  - Design ontology tag naming conventions
  - Understand star schema will be auto-generated

#### Survey Instance Management & Repetition
The system uses a sophisticated **display key system** to manage repeatable elements:

**Display Key Format**: `survey.step.step_instance.section.section_instance.question.question_instance`

**Example**: `1.2.1.3.2.15.1` = Survey 1, Step 2 (1st instance), Section 3 (2nd instance), Question 15 (1st instance)

**Repetition Capabilities**:
- **Step Repetition**: Multiple instances of the same step (e.g., multiple data subjects)
- **Section Repetition**: Multiple instances of sections within a step (e.g., multiple data categories)
- **Sub-Section Support**: Hierarchical section organization for complex workflows
- **Question Repetition**: Multiple instances of questions within sections (e.g., multiple medications)

**Use Cases for Repetition**:
- Data collection surveys (repeat steps for each data subject)
- Data categories (repeat sections for each category)
- Medication tracking (repeat questions for each medication)
- Longitudinal studies (repeat entire surveys over time)

#### Question Management
**Supported Question Types:**
- TEXT, TEXTAREA - Text input fields
- INTEGER, DOUBLE - Numeric inputs
- CHECKBOX, RADIO - Single selection
- CHECKBOX_GROUP, MULTI_SELECT - Multiple selection
- COMBOBOX - Dropdown selection
- DATE_PICKER, DATETIME - Date/time inputs
- EMAIL, PASSWORD - Specialized text inputs
- HTML - Informational content

**Question Features:**
- Required field validation
- Min/max value constraints
- Input masks and placeholders
- Tooltips and validation messages
- Default values
- Custom variants

#### Selection Groups & Items
- **Select Groups** - Collections of selectable options
- **Select Items** - Individual options with coded values and display text
- Support for data types and ordering

#### Survey Flow & Logic
- **Relationships** - Question dependencies and conditional logic
- **Operators** - BOOLEAN, GREATER_THAN, EQUAL, NOT_EQUAL, FIELD_EXIST, CONTAINS
- **Actions** - SHOW, REPEAT, TEXT replacement
- **Dependencies** - Tracks answer relationships for dynamic behavior

#### Response Management
- **Respondents** - Tracks survey participants with tokens
- **Answers** - Stores survey responses with rich metadata
- **Display Keys** - Hierarchical addressing system for survey elements

#### Administrative Features
- **Users** - Admin users with role-based access
- **Departments** - Organizational units for data segregation
- **Reports** - Configurable survey reports
- **Post-Survey Actions** - Automated actions after completion

### Current Survey Creation Workflow
Based on analysis of existing code:

1. **Survey Definition** - Currently done via direct database insertion or code
2. **Question Creation** - Questions created programmatically with type assignments
3. **Section Assignment** - Questions assigned to sections via sections_questions table
4. **Logic Configuration** - Relationships defined for conditional behavior
5. **Respondent Management** - Admin app provides REST API for subject registration

**Gaps Identified:**
- ❌ No visual survey authoring interface
- ❌ No drag-and-drop question builder
- ❌ No visual logic/flow designer
- ❌ No template or reusable component system
- ❌ Limited survey preview capabilities

## Proposed Authoring Application Architecture

### Technology Stack
- **Backend**: Java with Quarkus (consistent with existing apps)
- **Frontend**: Vaadin Flow (consistent with existing apps)
- **Database**: Existing PostgreSQL schema (no changes needed)
- **Authentication**: OIDC integration (same as Admin app)

### Testing Technology Stack
- **Unit Testing**: JUnit 5 (Jupiter) with Mockito
- **Assertions**: AssertJ for fluent, readable assertions
- **Database Testing**: Testcontainers with PostgreSQL
- **UI Testing**: Vaadin TestBench for component tests
- **API Testing**: REST Assured for endpoint testing
- **Mocking**: Mockito and WireMock for external dependencies
- **Coverage**: JaCoCo for code coverage reporting
- **Performance Testing**: JMeter or Gatling for load tests
- **Test Data**: Datafaker for realistic test data generation
- **CI/CD**: GitHub Actions with automated test execution

### Core Features for Version 1.0

#### 1. Survey Management Interface
- Create/edit/delete surveys
- Survey metadata management (name, title, description)
- Survey templates and cloning
- Version control and drafts

#### 2. Question Builder
- Visual question designer
- All supported question types
- Drag-and-drop interface
- **Token-enabled text editing** with real-time preview
- **Token palette** with available tokens (S1, G1, C1, CS2, R1)
- **Token syntax helper** for {TOKEN|default} patterns
- Question validation and preview
- **Live token replacement preview**
- Reusable question library

#### 3. Section & Step Organization
- Visual section designer
- Drag-and-drop section ordering
- Step configuration interface
- **Token-aware section naming** with preview
- Section templates

#### 4. Logic & Flow Designer
- Visual relationship builder
- Conditional logic designer
- **TOKEN replacement relationship designer**
- **Token mapping interface** for TEXT actions
- **Real-time token preview** in relationship editor
- Question dependency mapping
- Flow validation and testing

#### 5. Preview & Testing
- Live survey preview
- Test respondent simulation
- Logic flow testing
- Response validation

#### 6. Integration Features
- Import/export survey definitions
- Integration with existing Admin app
- User permission management
- Department-based access control

### Development Phases - Test Driven Development (TDD) Approach

**TDD Principles Applied Throughout:**
- Write tests FIRST before implementation code
- Red-Green-Refactor cycle for all features
- Unit tests for business logic (80%+ coverage)
- Integration tests for database operations
- UI component tests for Vaadin views
- End-to-end tests for critical user workflows

#### Phase 1: Foundation (Weeks 1-2) - TDD Focus: Infrastructure Tests
**Test-First Approach:**
1. Write tests for entity relationships and validation
2. Write tests for repository query methods
3. Write tests for authentication/authorization
4. Implement code to pass tests
5. Refactor for clean architecture

**Deliverables:**
- Set up project structure with test frameworks (JUnit 5, Mockito, Testcontainers)
- Database integration with test database configuration
- Basic CRUD operations for surveys (all methods test-covered)
- User authentication integration (security tests included)
- **Test Coverage Target: 85%+**

#### Phase 2: Core Authoring (Weeks 3-4) - TDD Focus: Question Builder Logic
**Test-First Approach:**
1. Write tests for question type handling
2. Write tests for section/question relationships
3. Write UI component interaction tests
4. Implement features to satisfy tests

**Deliverables:**
- Question builder interface (all question types tested)
- Section management (relationship tests)
- Basic survey structure creation (integration tests)
- **Test Coverage Target: 80%+**

#### Phase 3: Advanced Features (Weeks 5-6) - TDD Focus: Logic Engine
**Test-First Approach:**
1. Write tests for conditional logic evaluation
2. Write tests for template system
3. Write tests for preview rendering
4. Implement features to pass all tests

**Deliverables:**
- Logic designer (operator tests for all combinations)
- Preview functionality (preview generation tests)
- Template system (template validation tests)
- **Test Coverage Target: 85%+**

#### Phase 4: Integration & Polish (Weeks 7-8) - TDD Focus: E2E Workflows
**Test-First Approach:**
1. Write end-to-end workflow tests
2. Write integration tests for admin app
3. Write permission/authorization tests
4. Fix bugs revealed by comprehensive test suite

**Deliverables:**
- Admin app integration (API integration tests)
- User permissions (authorization tests)
- **Comprehensive end-to-end test suite**
- Bug fixes validated by regression tests
- **Test Coverage Target: Overall 85%+ with all critical paths at 95%+**

### Database Integration Strategy
**Advantage**: No schema changes required! The existing database structure is robust and supports all needed authoring features.

**Key Integration Points:**
- Reuse existing entity models
- Leverage existing sequence generators
- Maintain data consistency with Survey/Admin apps
- Use existing dimension and ontology systems

## Metadata & Star Schema Management - CRITICAL FEATURE ⭐

### Current Metadata System
The existing system has a sophisticated **metadata and dimensional modeling system** that automatically builds star schema structures for reporting:

#### Metadata Components
- **Dimensions Table** - Defines dimensional categories (e.g., "department", "category_type")
- **Ontology Table** - Maps tags to dimensions with survey-specific context
- **Metadata Table** - Links survey elements (questions, sections, steps) to ontology entries
- **Dynamic Dimension Tables** - Auto-generated `dim_*` tables (e.g., `dim_department`)
- **Fact Tables** - `fact_sections` and `fact_respondents` with dimensional foreign keys

#### Automatic ETL Process
The system includes a sophisticated ETL service that:
- **Automatically creates dimension tables** based on ontology tags (no manual dimension creation needed)
- **Auto-generates table names** from ontology tags: `'Age'` → `dim_age`, `'Breast Cancer'` → `dim_breast_cancer`
- Populates dimensions from survey responses via metadata assignments
- Builds fact tables with proper dimensional relationships
- Creates reporting views with star schema structure
- **Dimensions are created on-demand** based on survey metadata, not predefined

#### Current Gaps in Token Management
- ❌ **No token authoring interface** - Tokens currently hardcoded in database
- ❌ **No token preview system** - Cannot see token replacement results
- ❌ **No token documentation** - Token types and syntax not explained
- ❌ **No TEXT action UI** - Token replacement relationships created manually
- ❌ **No token validation** - No checking for token syntax or grammar errors
- ❌ **No token libraries** - No reusable token patterns for common surveys

#### Current Gaps in Metadata Management
- ❌ **No visual metadata editor** - Metadata currently managed via direct database manipulation
- ❌ **No ontology management interface** - Tags and dimensions created programmatically
- ❌ **No dimensional modeling UI** - Star schema structure not visually manageable
- ❌ **No metadata validation** - No interface to verify metadata consistency
- ❌ **No reporting preview** - Cannot preview dimensional structure before deployment

### Proposed Metadata Management Features

#### 7. Token Management System
- **Token Type Library**
  - Predefined token types (S1, G1, C1, CS2, R1) with documentation
  - Custom token creation for survey-specific needs
  - Token validation and naming conventions

- **Token-Enabled Text Editor**
  - Rich text editor with token insertion buttons
  - Auto-complete for token syntax: `{TOKEN|default_text}`
  - Syntax highlighting for token expressions
  - Grammar validation for possessive forms (S1's, G1's)

- **Real-Time Token Preview**
  - Live preview of text with token replacements
  - Sample data for preview contexts
  - Multi-scenario preview (different data structures)
  - Grammar checking for pronoun consistency

- **Token Relationship Manager**
  - Visual interface for TEXT action relationships
  - Drag-and-drop token assignment
  - Upstream value mapping to tokens
  - Token inheritance and propagation rules

#### 8. Question Text Editor with Token Support
- **Rich Text Editor Features**
  - WYSIWYG editor with token insertion
  - Token button palette for quick insertion
  - Auto-complete for `{TOKEN|default}` syntax
  - Syntax highlighting for tokens and defaults

- **Token Preview Panel**
  - Side-by-side preview of tokenized vs. rendered text
  - Sample survey data for realistic previews
  - Multiple preview scenarios (different data sizes)
  - Grammar and pronoun validation warnings

- **Token Library Integration**
  - Pre-built token patterns for common survey types
  - Data structure token templates
  - General survey token patterns
  - Demographic survey token libraries

#### 9. TEXT Action Relationship Builder
- **Visual Token Mapping**
  - Drag-and-drop interface for token assignments
  - Visual representation of token→value relationships
  - Upstream question to token binding
  - Default value and override value configuration

- **Token Flow Visualization**
  - Show how tokens propagate through survey
  - Dependency mapping for token values
  - Token usage tracking across questions
  - Token conflict detection and resolution

#### 10. Metadata & Ontology Editor
- **Visual Ontology Manager**
  - Create/edit/delete ontology entries
  - Map tags to dimensional categories
  - Survey-specific vs. global ontology management
  - Tag hierarchy and relationship management

- **Dimension Configuration**
  - Create new dimensional categories
  - Configure dimension properties and data types
  - Preview dimension table structures
  - Manage dimension value mappings

- **Metadata Assignment Interface**
  - Drag-and-drop metadata assignment to survey elements
  - Visual metadata relationship mapping
  - Bulk metadata operations
  - Metadata inheritance and templates

#### 8. Star Schema Designer
- **Visual Dimensional Modeling**
  - Interactive star schema diagram
  - Fact table configuration
  - Dimension relationship visualization
  - ETL process configuration

- **Reporting Structure Preview**
  - Preview generated dimension tables
  - Fact table structure visualization
  - Sample data preview with dimensional context
  - Reporting query builder

- **ETL Management**
  - Manual ETL process triggers
  - ETL status monitoring
  - Dimensional data validation
  - Performance optimization tools

### Updated Development Phases

#### Phase 1: Foundation (Weeks 1-2)
- Set up project structure
- Database integration
- Basic CRUD operations for surveys
- User authentication integration

#### Phase 2: Core Authoring (Weeks 3-4)
- **Token-enabled question builder interface**
- **Token palette and syntax helper**
- **Real-time token preview system**
- Section management
- Basic survey structure creation
- **Metadata entity integration**

#### Phase 3: Advanced Logic & Tokens (Weeks 5-6)
- **TEXT action relationship builder**
- **Token mapping and assignment interfaces**
- **Multi-context token preview**
- Logic designer
- Preview functionality
- **Token validation and testing**

#### Phase 4: Metadata & Dimensional Modeling (Weeks 7-8)
- **Ontology management interface**
- **Metadata assignment tools**
- **Automatic dimension visualization** (shows auto-created dimension tables)
- **Star schema preview** (displays ETL-generated structure)
- **Token-to-metadata integration**

#### Phase 5: Advanced Features (Weeks 9-10)
- Logic designer
- Preview functionality
- Template system
- **ETL management interface**

#### Phase 5: Advanced Features (Weeks 9-10)
- **Advanced token pattern libraries**
- **Token template system for common patterns**
- Template system
- **ETL management interface**

#### Phase 6: Integration & Polish (Weeks 11-12)
- Admin app integration
- User permissions
- **Reporting preview tools**
- Testing and bug fixes

### Database Integration for Metadata
**Key Advantage**: The metadata system is already robust and supports sophisticated dimensional modeling without schema changes.

**Integration Points**:
- Leverage existing `Dimensions`, `Ontology`, and `Metadata` entities
- Utilize existing ETL service infrastructure
- Extend current star schema automation
- Integrate with existing reporting architecture

### Next Steps for Implementation - TDD Approach

#### Step 1: Test Infrastructure Setup
1. Create new Maven project structure with test dependencies
   - JUnit 5 (Jupiter)
   - Mockito for mocking
   - AssertJ for fluent assertions
   - Testcontainers for database integration tests
   - Vaadin TestBench for UI tests
   - REST Assured for API tests

#### Step 2: Entity Tests First
2. **Write entity validation tests** before creating entities
   - Constraint validation tests (required fields, ranges)
   - Relationship integrity tests
   - Cascade operation tests
3. Set up Quarkus with Vaadin
4. **Implement entities** to pass validation tests
5. Copy and adapt entity models from Survey app with test coverage

#### Step 3: Repository Tests First
6. **Write repository query tests** before implementing repositories
   - CRUD operation tests
   - Custom query method tests
   - Transaction isolation tests
   - Pagination and sorting tests
7. **Implement Panache repositories** to satisfy tests

#### Step 4: Service Layer Tests First
8. **Analyze existing ETL service** for metadata management patterns
9. **Write service method tests** with mocked dependencies
   - Survey creation/update/delete tests
   - Question management tests
   - Token parsing and replacement tests
   - Relationship evaluation tests
10. **Implement service layer** to pass all tests

#### Step 5: UI Component Tests First
11. Design initial UI mockups **including metadata management interfaces**
12. **Write Vaadin component interaction tests**
    - Form submission tests
    - Validation feedback tests
    - Navigation tests
13. **Implement basic survey CRUD UI** with test-driven development

#### Step 6: Advanced Feature Tests First
14. **Write metadata and ontology operation tests**
    - Ontology creation and validation tests
    - Metadata assignment rule tests
    - Dimension generation tests
15. **Build metadata and ontology management interfaces** to specification

#### Step 7: Integration & E2E Tests
16. **Write end-to-end workflow tests** for complete features
17. **Run full test suite** and achieve 85%+ coverage
18. **Performance testing** with realistic data volumes

### TDD Success Metrics
- **Unit Test Coverage**: 85%+ overall, 95%+ for critical business logic
- **Integration Test Coverage**: All database operations and external integrations
- **UI Test Coverage**: All user workflows and form validations
- **Test Execution Time**: Full suite under 5 minutes
- **Continuous Integration**: All tests pass before merge
- **Bug Detection**: 90%+ of bugs caught by automated tests before manual QA

---

## FHHS Analysis: Validation Test Case

I've analyzed the FHHS (Family Health History Survey) application as a complex test case to validate that the proposed authoring tool can handle sophisticated survey requirements. The FHHS implementation demonstrates that our development plan is comprehensive and aligned with real-world requirements for complex surveys across any domain.

### FHHS Survey Complexity Analysis

The FHHS survey serves as an excellent test case demonstrating advanced features that validate our development plan:

**Survey Structure:**
- 15 steps with hierarchical navigation
- 48+ questions with complex conditional logic
- Dynamic question text using token replacement (S1, G1, C1, CS2, R1)
- Multi-dimensional metadata tracking

**Question Types Used:**
- Information Display (type 4) - Welcome screens, instructions
- Numeric Input (type 5) - Ages, counts
- Single Select (type 7) - Yes/No, gender, status selections
- Text Input (type 8) - Names, descriptions
- Checkboxes (type 1) - Multiple selections
- Multi-Select (type 10) - Complex choice selections

**Advanced Features:**
- **67+ ontology mappings** linking questions to dimensional data
- **82+ conditional relationships** controlling survey flow
- **Dynamic section generation** based on variable counts
- **Complex metadata assignments** for star schema population
- **Token-based text replacement** for personalized questions
- **16 select groups** with 40+ coded values
- **Repetition handling** for variable structures

### Development Plan Validation

✅ **Database Schema Support**: FHHS uses the exact same survey schema our plan targets - no additional tables needed

✅ **Question Type Coverage**: Our planned question type library covers all types used in FHHS

✅ **Conditional Logic System**: FHHS uses 82+ relationships - our visual conditional editor design is essential

✅ **Metadata Management**: FHHS has 67+ metadata mappings - our metadata interface planning is validated

✅ **Dynamic Repetition**: FHHS creates sections dynamically - our repetition system design is confirmed

✅ **Token Replacement System**: FHHS extensively uses tokens (S1, G1, C1, etc.) - our token management features are critically needed

✅ **Select Group Management**: FHHS uses 16 select groups - our dropdown/choice management features are needed

### Key Insights for Implementation

1. **Token Management is Critical**: FHHS uses complex token patterns requiring sophisticated authoring tools
2. **Visual Relationship Editor is Critical**: With 82+ conditional relationships, a drag-and-drop relationship builder is essential
3. **Metadata Interface Priority**: 67+ metadata mappings require sophisticated dimensional data management
4. **Token System Complexity**: Token replacement is more complex than initially estimated - requires dedicated token management UI with preview
5. **Repetition Patterns**: FHHS patterns show we need templates for common survey structures (hierarchical data, repetitive sections, etc.)

### Updated Feature Requirements

Based on FHHS analysis, our development plan should emphasize:

**Phase 2 Enhancements:**
- **Comprehensive token management system** with visual editor and preview
- **Token pattern libraries** for hierarchical data and complex surveys
- Advanced token management with preview capabilities
- Template library for common survey patterns (hierarchical data, demographic surveys)
- Bulk operations for creating repetitive question sequences

**Phase 3 Additions:**
- **TEXT action relationship builder** with drag-and-drop token assignment
- **Multi-context token preview** for complex hierarchical structures
- Relationship pattern libraries (common conditional logic templates)
- Advanced metadata bulk assignment tools
- Survey complexity analysis and optimization suggestions

**Phase 4 Additions:**
- **Token-to-metadata integration** for dimensional modeling
- **Advanced token validation** and grammar checking
- **Token template system** for reusable patterns

### Plan Validation Summary

The FHHS analysis confirms that our 10-week development plan is comprehensive and properly scoped. The proposed visual authoring tool will be capable of creating sophisticated surveys with:
- Complex hierarchical navigation
- Advanced conditional logic (82+ relationships)
- Sophisticated metadata management (67+ mappings)
- Dynamic content generation with token replacement
- Multi-dimensional data modeling integration

**Final Assessment**: ✅ **Development plan is validated and capable of creating FHHS-level complexity surveys**

---
*This comprehensive analysis, validated against the successful FHHS implementation, provides the foundation for building a powerful survey authoring tool with sophisticated metadata and dimensional modeling capabilities that seamlessly integrates with the existing Elicit platform.*