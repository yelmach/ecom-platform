pipeline {
    agent any

    parameters {
        string(
            name: 'EMAIL_RECIPIENTS',
            defaultValue: 'beytour.safae@gmail.com,beyour.safae@gmail.com',
            description: 'Comma-separated email recipients (example: dev1@company.com,dev2@company.com)'
        )
    }

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    environment {
        CHROME_BIN = '/usr/bin/chromium'
        BACKEND_SERVICES = 'discovery-service gateway-service user-service product-service media-service'
        SONAR_PROJECT_KEY = 'ecom-platform'
        SONAR_HOST_URL = 'http://host.docker.internal:9000'
        SONAR_TOKEN = credentials('sonar-token-ecom')
    }

    stages {
        stage('Checkout Code') {
            steps {
                checkout scm
            }
        }

        stage('Build Backend') {
            steps {
                script {
                    def services = env.BACKEND_SERVICES.tokenize(' ')

                    services.each { service ->
                        echo "Building ${service}..."
                        dir("backend/${service}") {
                            sh './mvnw -B -ntp clean package -DskipTests'
                        }
                    }
                }
            }
        }

        stage('Test Backend') {
            steps {
                script {
                    def services = env.BACKEND_SERVICES.tokenize(' ')

                    services.each { service ->
                        echo "Running tests for ${service}..."
                        dir("backend/${service}") {
                            sh './mvnw -B -ntp test'
                        }
                    }
                }
            }
        }

        stage('Install Frontend Dependencies') {
            steps {
                dir('frontend') {
                    sh 'npm ci'
                }
            }
        }

        stage('Test Frontend') {
            steps {
                dir('frontend') {
                    sh 'npm run test -- --watch=false --browsers=ChromeHeadlessNoSandbox --code-coverage'
                }
            }
        }

        stage('Build Frontend') {
            steps {
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

test -f sonar-project.properties || {
  echo "ERROR: sonar-project.properties not found in workspace: $PWD"
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
  -Dproject.settings="$PWD/sonar-project.properties" \
  -Dsonar.projectKey="${SONAR_PROJECT_KEY}" \
  -Dsonar.qualitygate.wait=true \
  -Dsonar.qualitygate.timeout=300
'''
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: 'backend/**/target/surefire-reports/*.xml'
            archiveArtifacts allowEmptyArchive: true, artifacts: 'frontend/coverage/**'
            echo 'Pipeline execution complete.'
        }
        success {
            echo 'Build and tests succeeded.'
            echo 'Email notification skipped (SMTP not configured).'
        }
        failure {
            echo 'Pipeline failed. Check stage logs and published reports.'
            echo 'Email notification skipped (SMTP not configured).'
        }
    }
}
