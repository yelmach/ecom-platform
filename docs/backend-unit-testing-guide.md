# Backend Unit Testing Guide (JUnit)

## 1) What Is JUnit?

JUnit is a Java testing framework.

In simple words:

- you write small methods that verify behavior
- each test checks one expected result
- the test runner executes all tests and reports pass/fail

This project uses **JUnit 5** (Jupiter API).

## 2) Why Unit Tests Matter

Unit tests help you:

- catch regressions early
- verify business rules without manual clicking/testing
- refactor safely
- ship changes with more confidence

In CI, unit tests are a quality gate: if tests fail, pipeline fails.

## 3) Key Concepts You Should Know

- `Test case`: one test method (`@Test`)
- `Test class`: group of related tests
- `Assertion`: check expected result (`assertEquals`, `assertThrows`, etc.)
- `Mock`: fake dependency used to isolate the unit under test
- `Unit under test`: the class/method whose logic you validate
- `Arrange / Act / Assert`:
  - Arrange: setup inputs/mocks
  - Act: call method
  - Assert: verify output/interaction

## 4) Stack Used in This Project

The backend tests use:

- **JUnit 5** for test execution and assertions
- **Mockito** for mocks and behavior verification
- **Spring test support** (MockMvc/web test slices in controller tests)

### 4.1 Library Definitions (Simple)

- `org.junit.jupiter`:
  - JUnit 5 API used to declare and run tests.
- `org.mockito`:
  - Mocking library used to fake dependencies and verify interactions.
- `org.springframework.test` and `spring-boot-starter-webmvc-test`:
  - Spring testing helpers for controller-layer tests (`MockMvc`, request/response assertions).

You can see test files under:

- `backend/*/src/test/java/...`

Examples in this repo:

- gateway tests:
  - `JwtUtilTest`
  - `AuthenticationFilterTest`
- user/product/media service tests:
  - service-layer tests (business logic)
  - controller tests (HTTP layer behavior)

## 5) Typical Test Features Used

### 5.1 `@Test`

Marks a method as a test method.

Also common JUnit annotations:

- `@BeforeEach`: runs before each test method to initialize shared setup.
- `@ExtendWith(MockitoExtension.class)`: enables Mockito support in JUnit 5 tests.

### 5.2 Assertions

Common assertions you will see:

- `assertEquals(expected, actual)`
- `assertTrue(...)`, `assertFalse(...)`
- `assertNotNull(...)`
- `assertThrows(Exception.class, () -> ...)`

What they mean:

- `assertEquals`: verify actual value equals expected value.
- `assertThrows`: verify an exception is raised for invalid behavior.

### 5.3 Mockito (`@Mock`, stubbing, verification)

Used when a class depends on repositories/services/clients.

Typical pattern:

1. mock dependencies
2. define stub behavior (`when(...).thenReturn(...)`)
3. call method under test
4. verify output and interactions (`verify(...)`)

Common Mockito methods in practice:

- `when(mock.method(...)).thenReturn(value)`: define fake return value.
- `when(...).thenThrow(...)`: define fake exception flow.
- `verify(mock).method(...)`: confirm dependency method was called.
- `verify(mock, never()).method(...)`: confirm method was not called.
- argument matchers such as `any()`, `eq(...)`: flexible argument matching.

### 5.4 Spring MVC/controller tests

Controller tests validate:

- status codes
- request/response payload behavior
- security/controller wiring behavior

They are faster than full end-to-end tests and still validate API layer logic.

Common Spring test methods used:

- `mockMvc.perform(...)`: execute HTTP request in test context.
- request builders: `get(...)`, `post(...)`, `put(...)`, `patch(...)`, `delete(...)`.
- response checks: `andExpect(status().isOk())`, `andExpect(status().isBadRequest())`, etc.
- payload checks: `andExpect(jsonPath(\"...\").value(...))`.

## 6) Test Structure Recommendations

Keep each test:

- focused on one behavior
- deterministic (same result every run)
- independent from other tests
- readable (clear name and intent)

Naming style recommendation:

- `shouldReturnXWhenY`
- `shouldThrowXWhenInvalidY`

## 7) How To Run Backend Tests

### Run tests for one service

```bash
cd backend/user-service
./mvnw test
```

### Run tests for all backend services (manual)

```bash
for s in discovery-service gateway-service user-service product-service media-service; do
  (cd "backend/$s" && ./mvnw -B -ntp test) || exit 1
done
```

## 8) How Jenkins Runs Backend Tests

In this project pipeline ([Jenkinsfile](/home/sbeytour/Documents/ecom-platform/Jenkinsfile)):

1. `Build Backend` stage:
   - `./mvnw -B -ntp clean package -DskipTests`
2. `Test Backend` stage:
   - `./mvnw -B -ntp test`

So build and tests are separate:

- fast packaging first
- real verification in dedicated test stage

If any service test fails, pipeline stops and marks build as failure.

## 9) Test Reports in Jenkins

Jenkins publishes backend JUnit XML results from:

- `backend/**/target/surefire-reports/*.xml`

Where they come from:

- Maven Surefire plugin outputs standard JUnit report files.

In Jenkins build page you can inspect:

- `Test Result` summary
- failed test class/method
- stack traces

## 10) Common Warnings You May See

### Mockito dynamic agent warning (Java 21+)

You may see warnings about dynamic agent loading with Mockito.

Important:

- This is currently a warning, not a test failure.
- Tests can still pass.
