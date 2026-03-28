pipeline {
    agent any

    tools {
        nodejs 'nodejs' 
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
                    def services = [
                        'discovery-service', 
                        'gateway-service', 
                        'user-service', 
                        'product-service', 
                        'media-service'
                    ]
                    
                    services.each { service ->
                        echo "Building ${service}..."
                        dir("backend/${service}") {
                            sh './mvnw clean package -DskipTests'
                        }
                    }
                }
            }
        }

        stage('Build Frontend') {
            steps {
                dir('frontend') { 
                    echo 'Installing dependencies and building Angular app...'
                    sh 'npm ci'
                    sh 'npm run build'
                }
            }
        }
    }

    post {
        always {
            echo 'Pipeline execution complete.'
        }
        success {
            echo 'Build Successful!'
        }
        failure {
            echo 'Build Failed. Please check the logs.'
        }
    }
}
