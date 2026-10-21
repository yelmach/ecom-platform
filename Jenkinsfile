pipeline {
    agent none

    options {
        timeout(time: 60, unit: 'MINUTES')
        disableConcurrentBuilds()
        skipDefaultCheckout(true)
    }

    stages {
        stage('Build and analyze') {
            agent { label 'arm64' }
            stages {
                stage('Checkout') {
                    steps {
                        deleteDir()
                        checkout scm
                        sh 'git rev-parse HEAD'
                    }
                }
                stage('Build and test') {
                    steps { sh 'make verify' }
                    post {
                        always {
                            junit testResults: 'backend/*/target/surefire-reports/TEST-*.xml,frontend/reports/junit/*.xml',
                                allowEmptyResults: true
                        }
                    }
                }
                stage('SonarQube analysis') {
                    steps {
                        withSonarQubeEnv('sonarqube') {
                            sh '''
                                set +x
                                export SONAR_TOKEN="$SONAR_AUTH_TOKEN"
                                make sonar-scan
                            '''
                        }
                    }
                }
            }
        }
        stage('Quality gate') {
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    script {
                        def gate = waitForQualityGate(webhookSecretId: 'sonarqube-webhook-secret')
                        if (gate.status != 'OK') {
                            error "SonarQube quality gate failed: ${gate.status}"
                        }
                    }
                }
            }
        }
    }
}
