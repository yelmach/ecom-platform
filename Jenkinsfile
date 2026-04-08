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
        DEPLOY_ATTEMPTED = 'false'
        ROLLBACK_TRIGGERED = 'false'
    }

    stages {
        stage('Checkout Code') {
            steps {
                checkout scm
                script {
                    env.CURRENT_COMMIT = sh(
                        script: 'git rev-parse HEAD',
                        returnStdout: true
                    ).trim()

                    echo "Detected commit: ${env.CURRENT_COMMIT}"
                }
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
                    if (!params.ENABLE_DEPLOY) {
                        echo 'Skipping deploy because ENABLE_DEPLOY is false.'
                        return
                    }

                    echo 'Deploying application with Docker Compose...'
                    env.DEPLOY_ATTEMPTED = 'true'
                    sh """
                        set -e

                        mkdir -p "${DEPLOY_DIR}"

                        rsync -a --delete \
                          --exclude '.git/' \
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
                }
            }
        }

        stage('Health Check') {
            steps {
                script {
                    if (!params.ENABLE_DEPLOY) {
                        echo 'Skipping health check because ENABLE_DEPLOY is false.'
                        return
                    }

                    echo 'Checking gateway health endpoint...'
                    sh 'curl -kfsS https://localhost:8443/actuator/health | grep -q "\"status\":\"UP\""'

                    echo 'Checking frontend availability...'
                    sh 'curl -kfsS https://localhost:4200 > /dev/null'

                    echo "Saving ${env.CURRENT_COMMIT} as the last successful deployed commit..."
                    sh """printf '%s\\n' '${env.CURRENT_COMMIT}' > '${env.LAST_SUCCESSFUL_DEPLOY_FILE}'"""
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
Commit: ${env.CURRENT_COMMIT ?: 'N/A'}
Deploy enabled: ${params.ENABLE_DEPLOY}
"""
                )
            }
        }
        failure {
            echo 'Pipeline failed. Check stage logs and published reports.'
            script {
                if (env.DEPLOY_ATTEMPTED == 'true' && fileExists(env.LAST_SUCCESSFUL_DEPLOY_FILE)) {
                    def previousCommit = readFile(env.LAST_SUCCESSFUL_DEPLOY_FILE).trim()

                    if (previousCommit && previousCommit != env.CURRENT_COMMIT) {
                        echo "Rolling back to previous successful commit ${previousCommit}..."
                        env.ROLLBACK_TRIGGERED = 'true'
                        sh "git checkout ${previousCommit}"
                        sh """
                            set -e

                            mkdir -p "${DEPLOY_DIR}"

                            rsync -a --delete \
                              --exclude '.git/' \
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
                    echo 'Skipping rollback because deployment was not attempted or no successful deploy snapshot exists yet.'
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
Commit: ${env.CURRENT_COMMIT ?: 'N/A'}
Deploy enabled: ${params.ENABLE_DEPLOY}
Rollback triggered: ${env.ROLLBACK_TRIGGERED}
"""
                )
            }
        }
    }
}
