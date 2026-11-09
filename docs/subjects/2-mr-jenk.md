## MR-Jenk

### Overview

In this project, you will set up a **Continuous Integration (CI)** and **Continuous Deployment (CD)** pipeline using **Jenkins** for your **e-commerce microservices platform (buy-01)** .  
This pipeline will automatically **build**, **test**, and **deploy** your application, ensuring consistent and reliable delivery.

### Role Play

You are a **DevOps engineer** responsible for automating the development workflow of your e-commerce platform.  
Your mission is to build a robust Jenkins pipeline that fetches the latest code, runs automated tests, deploys the project, and notifies the team of build statuses.

### Learning Objectives

- Set up and configure **Jenkins**
- Implement **CI/CD pipelines** using Jenkins Jobs
- Integrate **automated testing** for backend and frontend
- Automate **build, test, and deployment** stages
- Implement **notifications** and basic **rollback strategies**
- Apply **best practices** in automation and deployment

### Instructions

#### 1. Setting Up Jenkins

- Download, install, and configure **Jenkins**.
  - 💡 _Hint_: Use Jenkins official documentation or Docker to set up Jenkins.
- Set up build agents if necessary.

#### 2. Create a CI/CD Pipeline for Your E-commerce Platform

- Create a Jenkins job that fetches the source code from your Git repository (e.g., GitHub).
- Set up build triggers to initiate a build automatically whenever there’s a new commit.

#### 3. Automated Testing

- Integrate automated testing into your pipeline.
- Ensure the pipeline fails when a test fails.

#### 4. Deployment

- Automatically deploy your application to a server or platform of your choice after successful builds.
  - Examples: AWS, Oracle, Heroku, or a local server.
- Implement a **rollback strategy** in case a deployment fails.

#### 5. Notifications

- Set up **email** or **Slack notifications** to inform team members of build status (success or failure).

### Constraints

- The pipeline must automate the entire process from **build → test → deploy**.
- Failures in any stage should stop the pipeline.
- Notifications must clearly indicate build and deployment results.
- The system should support rollback in case of deployment errors.

---
---

## Audit page (Evaluation)

#### Functional

##### Download the project and trigger a Jenkins build. Observe if the pipeline runs as expected.

###### Does the pipeline initiate and run successfully from start to finish?

##### Trigger some intentional build errors and observe Jenkins' response.

###### Does Jenkins respond appropriately to build errors?

##### Examine the automated testing step.

###### Are tests run automatically during the pipeline execution? Does the pipeline halt on test failure?

##### Make a minor change in the source code, commit, and push. Observe if the pipeline is triggered automatically.

###### Does a new commit and push automatically trigger the Jenkins pipeline?

##### Check the deployment process.

###### Is the application deployed automatically after a successful build? Is there a rollback strategy in place?

#### Security

##### Examine the permissions on the Jenkins dashboard.

###### Are permissions set appropriately to prevent unauthorized access or changes?

##### Review how sensitive data (like API keys, passwords) is managed in Jenkins.

###### Is sensitive data secured using Jenkins secrets or environment variables?

#### Code Quality and Standards

##### Examine the Jenkinsfile or the build configuration.

###### Is the code/script well-organized and understandable? Are there any best practices being ignored?

##### Look into the test report formats and outputs.

###### Are test reports clear, comprehensive, and stored for future reference?

##### Check for notifications setup.

###### Are notifications triggered on build and deployment events? Are they informative?
