def backendServices = [
    'discovery-service',
    'gateway-service',
    'user-service',
    'product-service',
    'media-service'
]

pipeline {
    agent any

    parameters {
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

    tools {
        nodejs 'nodejs'
    }

    environment {
        CHROME_BIN = '/usr/bin/chromium'
    }

    stages {
        stage('Checkout Code') {
            steps {
                checkout scm
            }
        }

        stage('Verify Backend') {
            steps {
                script {
                    backendServices.each { service ->
                        echo "Building ${service}..."
                        dir("backend/${service}") {
                            sh './mvnw -B -ntp clean verify'
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
                    echo 'Running Angular unit tests...'
                    sh 'npm run test:ci'
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

        stage('Deploy') {
            steps {
                script {
                    def currentBranch = sh(
                        script: 'git rev-parse --abbrev-ref HEAD',
                        returnStdout: true
                    ).trim()

                    if (currentBranch != 'main') {
                        echo "Skipping deploy because current branch is ${currentBranch}."
                        return
                    }

                    echo 'Deploying application with Docker Compose...'
                    sh 'make prod-up'
                }
            }
        }

        stage('Health Check') {
            steps {
                script {
                    def currentBranch = sh(
                        script: 'git rev-parse --abbrev-ref HEAD',
                        returnStdout: true
                    ).trim()

                    if (currentBranch != 'main') {
                        echo "Skipping health check because current branch is ${currentBranch}."
                        return
                    }

                    echo 'Checking gateway health endpoint...'
                    sh 'curl -kfsS https://localhost:8443/actuator/health | grep -q "\"status\":\"UP\""'

                    echo 'Checking frontend availability...'
                    sh 'curl -kfsS https://localhost:4200 > /dev/null'
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
Branch: ${env.BRANCH_NAME ?: env.GIT_BRANCH ?: 'N/A'}
Commit: ${env.GIT_COMMIT ?: 'N/A'}
"""
                )
            }
        }
        failure {
            echo 'Pipeline failed. Check stage logs and published reports.'
            script {
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
Branch: ${env.BRANCH_NAME ?: env.GIT_BRANCH ?: 'N/A'}
Commit: ${env.GIT_COMMIT ?: 'N/A'}
"""
                )
            }
        }
    }
}
