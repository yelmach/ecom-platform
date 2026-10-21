pipeline {
    agent { label 'arm64' }

    options {
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
        skipDefaultCheckout(true)
    }

    parameters {
        booleanParam(name: 'RUN_PROJECT_CHECKS', defaultValue: false,
            description: 'Also run make verify after checking the agent and checkout.')
    }

    stages {
        stage('Check tools') {
            steps {
                sh '''
                    set -e
                    uname -m
                    java -version
                    node --version
                    npm --version
                    git --version
                    make --version
                    "$CHROME_BIN" --version
                    sonar-scanner --version
                    test -w /home/jenkins/agent
                    test -w /home/jenkins/.m2
                    test -w /home/jenkins/.npm
                    test -w "$SONAR_USER_HOME"
                '''
            }
        }

        stage('Checkout') {
            steps {
                checkout scm
                sh 'git rev-parse HEAD'
            }
        }

        stage('Verify project') {
            when { expression { params.RUN_PROJECT_CHECKS } }
            steps { sh 'make verify' }
            post {
                always {
                    junit testResults: 'backend/*/target/surefire-reports/TEST-*.xml,frontend/reports/junit/*.xml',
                        allowEmptyResults: true
                }
            }
        }
    }
}
