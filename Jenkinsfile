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
def sonarAnalysisAttempted = false
def qualityGateStatus = 'NOT_RUN'
def currentStageName = 'Not started'

pipeline {
    agent any

    parameters {
        booleanParam(
            name: 'ENABLE_SONAR_ANALYSIS',
            defaultValue: true,
            description: 'Run SonarQube analysis and enforce the Quality Gate before deployment'
        )
        string(
            name: 'EMAIL_RECIPIENTS',
            defaultValue: '',
            description: 'Comma-separated email recipients'
        )
        string(
            name: 'DEPLOY_HOST',
            defaultValue: 'host.docker.internal',
            description: 'Host used for post-deploy health checks, for example 129.x.x.x'
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
        // DEPLOY_DIR = (provided by Jenkins Global Config)
        RELEASE_ENV_FILE = "${env.DEPLOY_DIR}/.release.env"
        LAST_SUCCESSFUL_RELEASE_FILE = "${env.DEPLOY_DIR}/.last-successful-release.env"
        IMAGE_REGISTRY = 'ghcr.io/yelmach'
    }

    stages {
        stage('Checkout Code') {
            steps {
                script {
                    currentStageName = 'Checkout Code'
                }

                checkout scm

                script {
                    env.CURRENT_COMMIT = sh(
                        script: 'git rev-parse HEAD',
                        returnStdout: true
                    ).trim()

                    env.CURRENT_SHORT_COMMIT = sh(
                        script: 'git rev-parse --short HEAD',
                        returnStdout: true
                    ).trim()

                    if (!params.ENABLE_SONAR_ANALYSIS) {
                        qualityGateStatus = 'SKIPPED'
                    }

                    echo "Branch: ${env.BRANCH_NAME ?: env.CHANGE_BRANCH ?: 'unknown'}"
                    echo "PR ID: ${env.CHANGE_ID ?: 'N/A'}"
                    echo "Commit: ${env.CURRENT_COMMIT}"
                }
            }
        }

        stage('Verify Backend') {
            steps {
                script {
                    currentStageName = 'Verify Backend'

                    backendServices.each { service ->
                        echo "Verifying ${service}..."
                        dir("backend/${service}") {
                            sh './mvnw -B -ntp clean verify'
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
                    sh 'npm run build'
                }
            }
        }

        stage('SonarQube Analysis') {
            when {
                expression { return params.ENABLE_SONAR_ANALYSIS }
            }
            steps {
                script {
                    currentStageName = 'SonarQube Analysis'
                    sonarAnalysisAttempted = true

                    def scannerHome = tool 'sonar-scanner'

                    withSonarQubeEnv('sonarqube') {
                        sh """
                            "${scannerHome}/bin/sonar-scanner" \
                              -Dsonar.projectVersion="${env.BUILD_NUMBER}" \
                              -Dsonar.scm.revision="${env.CURRENT_COMMIT}"
                        """
                    }
                }
            }
        }

        stage('Quality Gate') {
            when {
                expression { return params.ENABLE_SONAR_ANALYSIS }
            }
            steps {
                script {
                    currentStageName = 'Quality Gate'

                    timeout(time: 10, unit: 'MINUTES') {
                        def qualityGate = waitForQualityGate()
                        qualityGateStatus = qualityGate.status

                        if (qualityGate.status != 'OK') {
                            error "Pipeline aborted because the SonarQube Quality Gate returned ${qualityGate.status}."
                        }
                    }
                }
            }
        }

        stage('Build and Push Docker Images') {
            when {
                allOf {
                    branch 'main'
                    not { changeRequest() }
                }
            }
            steps {
                script {
                    currentStageName = 'Build and Push Docker Images'
                    env.IMAGE_TAG = env.CURRENT_SHORT_COMMIT
                }
                withCredentials([usernamePassword(
                    credentialsId: 'ghcr-credentials',
                    usernameVariable: 'GHCR_USER',
                    passwordVariable: 'GHCR_TOKEN'
                )]) {
                    sh './scripts/ci/build-push-images.sh'
                }
            }
        }

        stage('Deploy') {
            when {
                allOf {
                    branch 'main'
                    not { changeRequest() }
                }
            }
            steps {
                script {
                    currentStageName = 'Deploy'
                    deployAttempted = true

                    sh './scripts/ci/deploy-prod.sh'

                    deploySucceeded = true
                }
            }
        }

        stage('Health Check') {
            when {
                allOf {
                    branch 'main'
                    not { changeRequest() }
                }
            }
            steps {
                script {
                    currentStageName = 'Health Check'
                    env.DEPLOY_HOST = params.DEPLOY_HOST
                    sh './scripts/ci/health-check.sh'
                    sh """cp '${env.RELEASE_ENV_FILE}' '${env.LAST_SUCCESSFUL_RELEASE_FILE}'"""
                    healthCheckPassed = true
                }
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: 'backend/**/target/surefire-reports/*.xml,frontend/reports/junit/*.xml'
            archiveArtifacts allowEmptyArchive: true, artifacts: 'backend/**/target/site/jacoco/**,frontend/coverage/**,.release.env'
            echo 'Pipeline execution complete.'
        }

        success {
            echo 'Build and tests succeeded.'

            script {
                def targetEmail = params.EMAIL_RECIPIENTS?.trim() ?: env.EMAIL_RECIPIENTS
                
                if (!targetEmail) {
                    echo 'Skipping success email: No recipient provided.'
                    return
                }

                mail(
                    to: targetEmail,
                    subject: "[Jenkins] SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                    body: """Pipeline succeeded.

Job: ${env.JOB_NAME}
Build: #${env.BUILD_NUMBER}
Branch: ${env.BRANCH_NAME ?: 'N/A'}
PR ID: ${env.CHANGE_ID ?: 'N/A'}
Commit: ${env.CURRENT_COMMIT ?: 'N/A'}
SonarQube enabled: ${params.ENABLE_SONAR_ANALYSIS}
SonarQube attempted: ${sonarAnalysisAttempted}
Quality Gate: ${qualityGateStatus}
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
                        script: "[ -f '${env.LAST_SUCCESSFUL_RELEASE_FILE}' ]",
                        returnStatus: true
                    ) == 0

                    if (rollbackFileExists) {
                        echo 'Rolling back to the last successful GHCR image release...'
                        rollbackTriggered = true

                        sh """
                            set -e
                            cd "${DEPLOY_DIR}"
                            docker compose --env-file backend/docker.env --env-file .last-successful-release.env -f docker-compose.prod.yml pull
                            docker compose --env-file backend/docker.env --env-file .last-successful-release.env -f docker-compose.prod.yml up -d --remove-orphans
                        """
                    } else {
                        echo "Skipping rollback because ${env.LAST_SUCCESSFUL_RELEASE_FILE} does not exist."
                    }
                } else {
                    echo 'Skipping rollback because deployment was not attempted.'
                }

                def targetEmail = params.EMAIL_RECIPIENTS?.trim() ?: env.EMAIL_RECIPIENTS
                
                if (!targetEmail) {
                    echo 'Skipping success email: No recipient provided.'
                    return
                }

                mail(
                    to: targetEmail,
                    subject: "[Jenkins] FAILURE: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                    body: """Pipeline failed.

Job: ${env.JOB_NAME}
Build: #${env.BUILD_NUMBER}
Branch: ${env.BRANCH_NAME ?: 'N/A'}
PR ID: ${env.CHANGE_ID ?: 'N/A'}
Commit: ${env.CURRENT_COMMIT ?: 'N/A'}
Failed stage: ${currentStageName}
SonarQube enabled: ${params.ENABLE_SONAR_ANALYSIS}
SonarQube attempted: ${sonarAnalysisAttempted}
Quality Gate: ${qualityGateStatus}
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
