def backendServices = [
    'discovery-service',
    'gateway-service',
    'user-service',
    'product-service',
    'media-service'
]
def deployAttempted = false
def deploySucceeded = false
def healthCheckPassed = false
def rollbackTriggered = false
def currentStageName = 'Not started'
def isMainBranchBuild = false
def isPullRequestBuild = false

pipeline {
    agent any

    parameters {
        booleanParam(
            name: 'ENABLE_DEPLOY',
            defaultValue: true,
            description: 'Run deployment and post-deploy health checks after a successful build'
        )
        string(
            name: 'EMAIL_RECIPIENTS',
            defaultValue: '',
            description: 'Comma-separated email recipients (example: dev1@company.com,dev2@company.com)'
        )
    }

    options {
        timestamps()
        disableConcurrentBuilds()
        skipDefaultCheckout(true)
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    triggers {
        githubPush()
    }

    tools {
        nodejs 'nodejs'
    }

    environment {
        CHROME_BIN = '/usr/bin/chromium'
        DEPLOY_DIR = '/home/opc/ecom-platform-deploy'
        LAST_SUCCESSFUL_DEPLOY_FILE = '/home/opc/ecom-platform-deploy/.jenkins-last-successful-deploy'
        SONAR_PROJECT_KEY = 'ecom-platform'
        SONAR_HOST_URL = 'http://84.8.216.100:9002'
        SONAR_TOKEN = credentials('sonar-token-ecom')
    }

    stages {
        stage('Checkout Code') {
            steps {
                checkout scm
                script {
                    currentStageName = 'Checkout Code'
                    env.CURRENT_BRANCH = env.BRANCH_NAME ?: sh(
                        script: 'git rev-parse --abbrev-ref HEAD',
                        returnStdout: true
                    ).trim()
                    env.CURRENT_COMMIT = sh(
                        script: 'git rev-parse HEAD',
                        returnStdout: true
                    ).trim()
                    isPullRequestBuild = !!env.CHANGE_ID?.trim()
                    isMainBranchBuild = env.CURRENT_BRANCH == 'main'

                    echo "Detected branch: ${env.CURRENT_BRANCH}"
                    echo "Detected commit: ${env.CURRENT_COMMIT}"
                    echo "Pull request build: ${isPullRequestBuild}"
                }
            }
        }

        stage('Verify Backend') {
            steps {
                script {
                    currentStageName = 'Verify Backend'
                    backendServices.each { service ->
                        echo "Building ${service}..."
                        dir("backend/${service}") {
                            sh './mvnw -B -ntp clean verify'
                            sh './mvnw -B -ntp dependency:copy-dependencies -DincludeScope=compile -DoutputDirectory=target/dependency'
                        }
                    }
                }
            }
        }

        stage('Install Frontend Dependencies') {
            steps {
                script {
                    currentStageName = 'Install Frontend Dependencies'
                }
                dir('frontend') {
                    sh 'npm ci'
                }
            }
        }

        stage('Test Frontend') {
            steps {
                script {
                    currentStageName = 'Test Frontend'
                }
                dir('frontend') {
                    echo 'Running Angular unit tests...'
                    sh 'npm run test:ci'
                }
            }
        }

        stage('Build Frontend') {
            steps {
                script {
                    currentStageName = 'Build Frontend'
                }
                dir('frontend') {
                    echo 'Building Angular app...'
                    sh 'npm run build'
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                script {
                    if (!env.SONAR_HOST_URL?.trim() || !env.SONAR_TOKEN?.trim()) {
                        error('SONAR_HOST_URL and SONAR_TOKEN must be configured in Jenkins environment/credentials.')
                    }
                }

                sh '''#!/bin/bash
set -euo pipefail

test -f sonarQube/sonar-project.properties || {
  echo "ERROR: sonarQube/sonar-project.properties not found in workspace: $PWD"
  ls -la
  exit 1
}

docker run --rm \
  --add-host=host.docker.internal:host-gateway \
  --volumes-from "$(hostname)" \
  -e SONAR_HOST_URL="${SONAR_HOST_URL}" \
  -e SONAR_TOKEN="${SONAR_TOKEN}" \
  -w "$PWD" \
  sonarsource/sonar-scanner-cli:latest \
  -Dproject.settings="$PWD/sonarQube/sonar-project.properties" \
  -Dsonar.projectKey="${SONAR_PROJECT_KEY}" \
  -Dsonar.qualitygate.wait=true \
  -Dsonar.qualitygate.timeout=300
'''
            }
        }

        stage('Deploy') {
            steps {
                script {
                    currentStageName = 'Deploy'
                    if (!params.ENABLE_DEPLOY) {
                        echo 'Skipping deploy because ENABLE_DEPLOY is false.'
                        return
                    }
                    if (!isMainBranchBuild) {
                        echo "Skipping deploy because branch ${env.CURRENT_BRANCH} is not main."
                        return
                    }
                    if (isPullRequestBuild) {
                        echo 'Skipping deploy because this is a pull request build.'
                        return
                    }

                    echo 'Deploying application with Docker Compose...'
                    deployAttempted = true
                    sh """
                        set -e

                        mkdir -p "${DEPLOY_DIR}"

                        rsync -a --delete \
                          --exclude '.git/' \
                          --exclude '.jenkins-last-successful-deploy' \
                          --exclude 'backend/docker.env' \
                          --exclude 'backend/certs/' \
                          --exclude 'backend/keys/' \
                          --exclude 'frontend/node_modules/' \
                          --exclude 'frontend/coverage/' \
                          --exclude 'frontend/reports/' \
                          ./ "${DEPLOY_DIR}/"

                        cd "${DEPLOY_DIR}"
                        docker compose --env-file backend/docker.env -f docker-compose.yml up --build -d
                    """
                    deploySucceeded = true
                }
            }
        }

        stage('Health Check') {
            steps {
                script {
                    currentStageName = 'Health Check'
                    if (!params.ENABLE_DEPLOY) {
                        echo 'Skipping health check because ENABLE_DEPLOY is false.'
                        return
                    }
                    if (!isMainBranchBuild) {
                        echo "Skipping health check because branch ${env.CURRENT_BRANCH} is not main."
                        return
                    }
                    if (isPullRequestBuild) {
                        echo 'Skipping health check because this is a pull request build.'
                        return
                    }

                    echo 'Waiting for gateway health endpoint to become ready...'
                    sh '''
                        set -e

                        for attempt in $(seq 1 6); do
                          if curl -kfsS https://host.docker.internal:8443/actuator/health | grep -q '"status":"UP"'; then
                            echo "Gateway is healthy on attempt ${attempt}."
                            exit 0
                          fi

                          echo "Gateway not ready yet (attempt ${attempt}/6). Waiting 5 seconds..."
                          sleep 5
                        done

                        echo 'Gateway health check did not succeed in time.'
                        exit 1
                    '''

                    echo 'Waiting for frontend to become reachable...'
                    sh '''
                        set -e

                        for attempt in $(seq 1 6); do
                          if curl -kfsS https://host.docker.internal:4200 > /dev/null; then
                            echo "Frontend is reachable on attempt ${attempt}."
                            exit 0
                          fi

                          echo "Frontend not ready yet (attempt ${attempt}/6). Waiting 5 seconds..."
                          sleep 5
                        done

                        echo 'Frontend health check did not succeed in time.'
                        exit 1
                    '''

                    echo "Saving ${env.CURRENT_COMMIT} as the last successful deployed commit..."
                    sh """printf '%s\\n' '${env.CURRENT_COMMIT}' > '${env.LAST_SUCCESSFUL_DEPLOY_FILE}'"""
                    healthCheckPassed = true
                }
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: 'backend/**/target/surefire-reports/*.xml,frontend/reports/junit/*.xml'
            archiveArtifacts allowEmptyArchive: true, artifacts: 'frontend/coverage/**'
            echo 'Pipeline execution complete.'
        }
        success {
            echo 'Build and tests succeeded.'
            script {
                if (!params.EMAIL_RECIPIENTS?.trim()) {
                    echo 'Skipping success email: EMAIL_RECIPIENTS parameter is empty.'
                    return
                }

                mail(
                    to: params.EMAIL_RECIPIENTS.trim(),
                    subject: "[Jenkins] SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                    body: """Pipeline succeeded.

Job: ${env.JOB_NAME}
Build: #${env.BUILD_NUMBER}
Branch: ${env.CURRENT_BRANCH ?: 'N/A'}
Pull request build: ${isPullRequestBuild}
Commit: ${env.CURRENT_COMMIT ?: 'N/A'}
Deploy enabled: ${params.ENABLE_DEPLOY}
Deploy attempted: ${deployAttempted}
Deploy succeeded: ${deploySucceeded}
Health check passed: ${healthCheckPassed}
Rollback triggered: ${rollbackTriggered}
Build URL: ${env.BUILD_URL ?: 'N/A'}
"""
                )
            }
        }
        failure {
            echo 'Pipeline failed. Check stage logs and published reports.'
            script {
                if (deployAttempted) {
                    def rollbackFileExists = sh(
                        script: "[ -f '${env.LAST_SUCCESSFUL_DEPLOY_FILE}' ]",
                        returnStatus: true
                    ) == 0

                    if (rollbackFileExists) {
                        def previousCommit = sh(
                            script: "cat '${env.LAST_SUCCESSFUL_DEPLOY_FILE}'",
                            returnStdout: true
                        ).trim()

                        if (previousCommit && previousCommit != env.CURRENT_COMMIT) {
                            echo "Rolling back to previous successful commit ${previousCommit}..."
                            rollbackTriggered = true
                            sh "git checkout ${previousCommit}"
                            sh """
                                set -e

                                mkdir -p "${DEPLOY_DIR}"

                                rsync -a --delete \
                                  --exclude '.git/' \
                                  --exclude '.jenkins-last-successful-deploy' \
                                  --exclude 'backend/docker.env' \
                                  --exclude 'backend/certs/' \
                                  --exclude 'backend/keys/' \
                                  --exclude 'frontend/node_modules/' \
                                  --exclude 'frontend/coverage/' \
                                  --exclude 'frontend/reports/' \
                                  ./ "${DEPLOY_DIR}/"

                                cd "${DEPLOY_DIR}"
                                docker compose --env-file backend/docker.env -f docker-compose.yml up --build -d
                            """
                        } else {
                            echo 'Skipping rollback because there is no different previously successful deployed commit.'
                        }
                    } else {
                        echo "Skipping rollback because ${env.LAST_SUCCESSFUL_DEPLOY_FILE} does not exist."
                    }
                } else {
                    echo 'Skipping rollback because deployment was not attempted.'
                }

                if (!params.EMAIL_RECIPIENTS?.trim()) {
                    echo 'Skipping failure email: EMAIL_RECIPIENTS parameter is empty.'
                    return
                }

                mail(
                    to: params.EMAIL_RECIPIENTS.trim(),
                    subject: "[Jenkins] FAILURE: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                    body: """Pipeline failed.

Job: ${env.JOB_NAME}
Build: #${env.BUILD_NUMBER}
Branch: ${env.CURRENT_BRANCH ?: 'N/A'}
Pull request build: ${isPullRequestBuild}
Commit: ${env.CURRENT_COMMIT ?: 'N/A'}
Failed stage: ${currentStageName}
Deploy enabled: ${params.ENABLE_DEPLOY}
Deploy attempted: ${deployAttempted}
Deploy succeeded: ${deploySucceeded}
Health check passed: ${healthCheckPassed}
Rollback triggered: ${rollbackTriggered}
Build URL: ${env.BUILD_URL ?: 'N/A'}
"""
                )
            }
        }
    }
}
