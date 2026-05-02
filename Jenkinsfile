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
        CHROME_BIN = '/usr/bin/chromium'
        DEPLOY_DIR = '/home/opc/ecom-platform-deploy'
        LAST_SUCCESSFUL_DEPLOY_FILE = '/home/opc/ecom-platform-deploy/.jenkins-last-successful-deploy'
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
            when {
                allOf {
                    branch 'main'
                    not { changeRequest() }
                }
            }
            steps {
                script {
                    currentStageName = 'Health Check'

                    echo "Checking gateway on https://${params.DEPLOY_HOST}:8443/actuator/health"
                    sh """
                        set -e

                        for attempt in \$(seq 1 6); do
                          if curl -kfsS "https://${params.DEPLOY_HOST}:8443/actuator/health" | grep -q '"status":"UP"'; then
                            echo "Gateway is healthy on attempt \${attempt}."
                            exit 0
                          fi

                          echo "Gateway not ready yet (attempt \${attempt}/6). Waiting 5 seconds..."
                          sleep 5
                        done

                        echo 'Gateway health check did not succeed in time.'
                        exit 1
                    """

                    echo "Checking frontend on https://${params.DEPLOY_HOST}:4200"
                    sh """
                        set -e

                        for attempt in \$(seq 1 6); do
                          if curl -kfsS "https://${params.DEPLOY_HOST}:4200" > /dev/null; then
                            echo "Frontend is reachable on attempt \${attempt}."
                            exit 0
                          fi

                          echo "Frontend not ready yet (attempt \${attempt}/6). Waiting 5 seconds..."
                          sleep 5
                        done

                        echo 'Frontend health check did not succeed in time.'
                        exit 1
                    """

                    sh """printf '%s\\n' '${env.CURRENT_COMMIT}' > '${env.LAST_SUCCESSFUL_DEPLOY_FILE}'"""
                    healthCheckPassed = true
                }
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: 'backend/**/target/surefire-reports/*.xml,frontend/reports/junit/*.xml'
            archiveArtifacts allowEmptyArchive: true, artifacts: 'backend/**/target/site/jacoco/**,frontend/coverage/**'
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
