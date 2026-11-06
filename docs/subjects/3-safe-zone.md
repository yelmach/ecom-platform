## SafeZone

### Overview

In this project, you will improve the **code quality** and **security** of your e-commerce microservices platform (continue on buy-01 and mr-jenk) by integrating **SonarQube** into your development workflow.  
The goal is to automate code analysis, detect potential vulnerabilities, and ensure continuous monitoring of your codebase to maintain clean, secure, and maintainable code.

### Role Play

You are a **DevOps and quality assurance engineer** responsible for implementing continuous code quality checks across all microservices.  
Your mission is to configure **SonarQube**, connect it to your GitHub repository, and integrate it with your CI/CD pipeline to automatically detect and prevent poor-quality or insecure code from being merged.

### Learning Objectives

- Set up **SonarQube** using Docker
- Configure SonarQube for **multi-service projects**
- Integrate **SonarQube with GitHub** and CI/CD pipelines
- Automate **static code analysis** during builds
- Enforce **quality gates** to prevent merging insecure or low-quality code
- Establish a **review and approval process** for code improvements
- Promote continuous **monitoring and feedback** of code quality metrics

### Instructions

#### 1. SonarQube Setup with Docker

- Pull the official **SonarQube Docker image** from Docker Hub and run it locally.
- Ensure the service is accessible through your browser on the configured port.
- 💡 _Hint_: Use the official SonarQube documentation for Docker setup.

#### 2. SonarQube Configuration

- Access the **SonarQube dashboard** from your local environment.
- Configure a new project for your **e-commerce microservices** codebase.
- Set up appropriate project keys, tokens, and quality profiles.

#### 3. GitHub Integration

- Integrate **SonarQube** with your GitHub repository.
- Configure **webhooks or GitHub Actions** to trigger code analysis on every push.
- Ensure all branches and pull requests are scanned for quality and security issues.

#### 4. Code Analysis

- Automate **static code analysis** through your **CI/CD pipeline**.
- Configure your pipeline to **fail automatically** if SonarQube detects:
  - Major vulnerabilities
  - Code smells
  - Failing quality gates or coverage thresholds

#### 5. Continuous Monitoring

- Schedule **regular SonarQube scans** to continuously monitor code health.
- Review SonarQube dashboards for key metrics such as coverage, duplication, maintainability, and security hotspots.

#### 6. Review and Approval Process

- Establish a **mandatory code review** process before merging pull requests.
- Require all identified issues to be resolved or justified before approval.
- Track quality improvements over time through SonarQube reports.

### Constraints

- Use **SonarQube via Docker** for setup and execution
- Integrate SonarQube with **GitHub** for automatic scanning
- Link SonarQube with your **CI/CD pipeline** for build-time analysis
- The pipeline must **fail** if quality gates are not met
- Maintain a visible dashboard of key quality metrics

---
---

## Audit page (Evaluation)

#### Functional

##### Access the SonarQube web interface running on your local environment.

###### Is the SonarQube web interface accessible, and has it been configured to work with your project's codebase?

##### Integrate SonarQube with your GitHub repository.

###### Is SonarQube integrated with GitHub, and does it trigger code analysis on every push to the repository?

##### Set up and configure SonarQube for code analysis using Docker.

###### Is SonarQube configured correctly, and does it analyze code during the CI/CD pipeline?

##### Automate code analysis using SonarQube during the CI/CD pipeline.

###### Does the CI/CD pipeline correctly analyze code, and does it fail when code quality or security issues are detected?

##### Implement a code review and approval process.

###### Is there a code review and approval process in place to ensure code quality improvements are reviewed and approved?

#### Comprehension

##### SonarQube Setup and Integration

###### Can the student explain the steps required to set up SonarQube within the project environment?

###### Can the student describe the process of integrating SonarQube with the project's CI/CD pipeline and GitHub repository?

##### SonarQube Functionality

###### Can the student explain how SonarQube functions within the project, including its role in code analysis and how it contributes to code quality improvement?

#### Security

##### Review the permissions and access controls in SonarQube.

###### Are permissions set appropriately to prevent unauthorized access to code analysis results?

#### Code Quality and Standards

##### Examine the SonarQube rules and code analysis reports.

###### Are SonarQube rules configured correctly, and are code quality and security issues accurately identified?

##### Review any code quality improvements made based on SonarQube feedback.

###### Are code quality issues addressed and committed to the GitHub repository?
