pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    tools {
        nodejs 'nodejs'
    }

    environment {
        CHROME_BIN = '/usr/bin/chromium'
        BACKEND_SERVICES = 'discovery-service gateway-service user-service product-service media-service'
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
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: 'backend/**/target/surefire-reports/*.xml'
            archiveArtifacts allowEmptyArchive: true, artifacts: 'frontend/coverage/**'
            echo 'Pipeline execution complete.'
        }
        success {
            echo 'Build and tests succeeded.'
        }
        failure {
            echo 'Pipeline failed. Check stage logs and published reports.'
        }
    }
}
