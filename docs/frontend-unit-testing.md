# Frontend Unit Testing Guide (Jasmine + Karma)

## 1) What Are Jasmine and Karma?

### Jasmine

Jasmine is the test framework.

It gives you:

- test structure (`describe`, `it`)
- assertions (`expect(...).toBe(...)`, etc.)
- spies/mocks for dependencies

### Karma

Karma is the test runner.

It:

- launches a browser
- executes Jasmine test files
- reports pass/fail results

In this project:

- Jasmine defines tests
- Karma runs them
- Angular test builder compiles and serves test bundles

## 2) Why Frontend Unit Tests Matter

Frontend unit tests help you validate:

- component behavior
- service logic
- guards/interceptors behavior
- integration of Angular pieces at unit/small integration level

They catch regressions early before manual QA.

## 3) Core Concepts You Should Know

- `describe(...)`: group of related tests
- `it(...)`: one specific test case
- `expect(...)`: assertion
- `beforeEach(...)`: setup before each test
- `TestBed`: Angular testing module setup
- `spyOn(...)`: mock/observe function calls
- `fakeAsync` and `tick`: control async timing in tests

## 4) What This Project Tests

You already have frontend tests for:

- guards
- interceptors
- core services
- auth/shop/seller feature components
- shared components

Examples in this repo:

- [header.spec.ts](../frontend/src/app/shared/components/header/header.spec.ts)
- [product-details.spec.ts](../frontend/src/app/features/shop/product-details/product-details.spec.ts)
- [new-product.spec.ts](../frontend/src/app/features/seller/new-product/new-product.spec.ts)
- [dashboard.spec.ts](../frontend/src/app/features/seller/dashboard/dashboard.spec.ts)

## 4.1 Library Definitions (Simple)

- `jasmine-core`:
  - test framework API (`describe`, `it`, `expect`, spies).
- `karma`:
  - test runner that launches a browser and executes test bundles.
- `karma-chrome-launcher`:
  - Chrome/ChromeHeadless browser launcher for Karma.
- `karma-coverage`:
  - coverage instrumentation and report generation.
- Angular testing utilities (`@angular/core/testing`):
  - tools like `TestBed`, fixtures, and async helpers for Angular tests.

## 5) How Frontend Tests Are Configured

Main files:

- [frontend/package.json](../frontend/package.json)
- [frontend/angular.json](../frontend/angular.json)

Test script:

- `npm run test`

Angular test target uses:

- Karma builder (`@angular/build:karma`)
- test TypeScript config (`tsconfig.spec.json`)
- test polyfills (`zone.js/testing`)

## 6) How To Run Frontend Tests Locally

From repo root:

```bash
cd frontend
npm ci
npm run test
```

Useful CI-style local run:

```bash
npm run test -- --watch=false --browsers=ChromeHeadless --code-coverage
```

## 7) How Jenkins Runs Frontend Tests

In [Jenkinsfile](/home/sbeytour/Documents/ecom-platform/Jenkinsfile):

1. `Install Frontend Dependencies`
   - `npm ci`
2. `Test Frontend`
   - `npm run test -- --watch=false --browsers=ChromeHeadlessNoSandbox --code-coverage`
3. `Build Frontend`
   - `npm run build`

Why this order:

- fail fast on tests
- avoid building if tests fail

## 8) Why CI Uses `ChromeHeadlessNoSandbox`

In this repo, Jenkins container is configured to run as root.

In root containers, default Chrome sandbox can block startup.

So CI uses:

- `ChromeHeadlessNoSandbox`

This is a CI compatibility setting for this environment.

## 9) Frontend Test Artifacts in Jenkins

Pipeline archives coverage output:

- `frontend/coverage/**`

So after a run you can inspect coverage artifacts in the Jenkins build page.

## 10) Common Frontend Test Failures

### A) `Running as root without --no-sandbox is not supported`

Cause:

- Chrome launched in root container without no-sandbox mode

Fix:

- run with `--browsers=ChromeHeadlessNoSandbox` in CI

### B) `describe is not defined`

Cause:

- Jasmine framework setup not loaded correctly (often due to incorrect custom Karma config)

Fix:

- keep Angular default Karma setup unless you fully configure frameworks/plugins

### C) Browser launch timeout/failure

Cause:

- missing Chrome/Chromium in CI image

Fix:

- install Chromium in Jenkins image
- ensure `CHROME_BIN` path is valid in pipeline environment

## 11) Good Testing Practices

1. Keep one behavior per `it(...)`.
2. Use clear test names (`shouldXWhenY`).
3. Prefer deterministic tests (no random/time dependence).
4. Mock external dependencies instead of real HTTP/backend.
5. Test both success and error paths.

## 11.1 Common Methods and What They Mean

### Jasmine structure methods

- `describe('...', () => { ... })`:
  - group related tests for one feature/class.
- `it('...', () => { ... })`:
  - one specific expected behavior.
- `beforeEach(() => { ... })`:
  - setup code that runs before each test case.
- `afterEach(() => { ... })`:
  - cleanup code after each test case.

### Jasmine assertion methods

- `expect(value).toBe(expected)`:
  - strict equality.
- `expect(value).toEqual(expected)`:
  - deep equality for objects/arrays.
- `expect(value).toBeTruthy()` / `toBeFalsy()`:
  - truthy/falsy check.
- `expect(fn).toThrow()`:
  - verifies error throw behavior.
- `expect(spy).toHaveBeenCalled()` / `toHaveBeenCalledWith(...)`:
  - verifies calls and arguments.

### Jasmine spy methods

- `spyOn(obj, 'method')`:
  - replace a real method with a spy.
- `spy.and.returnValue(value)`:
  - make the spy return a fixed value.
- `spy.and.callFake(fn)`:
  - provide custom fake implementation.
- `jasmine.createSpyObj('name', ['m1', 'm2'])`:
  - create a mocked object with spy methods.

### Angular testing methods

- `TestBed.configureTestingModule({...})`:
  - define testing module dependencies.
- `TestBed.createComponent(Component)`:
  - create component fixture + instance.
- `fixture.detectChanges()`:
  - apply bindings and update template.
- `TestBed.inject(Service)`:
  - get service instance from test injector.
- `waitForAsync(...)`:
  - async setup helper.
- `fakeAsync(...)` + `tick(...)`:
  - control async/timer behavior deterministically.
---
