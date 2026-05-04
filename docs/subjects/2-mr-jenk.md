## MR-Jenk

### Overview

In this project, you will set up a **Continuous Integration (CI)** and **Continuous Deployment (CD)** pipeline using **Jenkins** for your **e-commerce microservices platform**.  
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
  - 💡 _Hint_: Use **JUnit** for backend testing and **Jasmine/Karma** for Angular frontend testing.
- Ensure the pipeline fails when a test fails.

#### 4. Deployment

- Automatically deploy your application to a server or platform of your choice after successful builds.
  - Examples: AWS, Heroku, or a local server.
- Implement a **rollback strategy** in case a deployment fails.

#### 5. Notifications

- Set up **email** or **Slack notifications** to inform team members of build status (success or failure).

### Constraints

- The pipeline must automate the entire process from **build → test → deploy**.
- Failures in any stage should stop the pipeline.
- Notifications must clearly indicate build and deployment results.
- The system should support rollback in case of deployment errors.

### Evaluation

Your CI/CD setup will be assessed on:

- ⚙️ **Automation**: Proper and automatic code fetching and build triggering
- 🧪 **Testing Integration**: Correct handling of automated tests and failures
- 🚀 **Deployment**: Reliable and repeatable deployment process
- 🔔 **Notifications**: Accurate and timely build and deployment updates
- 🧩 **Pipeline Quality**: Clear structure, proper stage configuration, and maintainability
- (⭐ Bonus) Effective use of parameterized and distributed builds

### Bonus Features (Optional but Recommended)

- **Parameterized Builds**: Allow customization of parameters (e.g., environment selection) for each build run
- **Distributed Builds**: Use multiple build agents for parallel or multi-environment builds
