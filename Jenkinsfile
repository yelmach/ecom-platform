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
                        script {
                            env.VERIFIED_COMMIT = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
                            env.IMAGE_TAG = env.VERIFIED_COMMIT.take(12)
                            env.DEPLOY_TARGET = env.APP_DOMAIN
                            env.SOURCE_URL = scm.userRemoteConfigs[0].url.replaceFirst(/\.git$/, '')
                        }
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
        stage('Build and publish images') {
            agent { label 'arm64' }
            stages {
                stage('Checkout verified commit') {
                    steps {
                        deleteDir()
                        checkout scm
                        sh '''
                            set -eu
                            git checkout --detach "$VERIFIED_COMMIT"
                            test "$(git rev-parse HEAD)" = "$VERIFIED_COMMIT"
                            test -n "$GHCR_PREFIX"
                            echo "Image tag: $IMAGE_TAG"
                        '''
                    }
                }
                stage('Build images') {
                    steps {
                        sh '''
                            set -eu
                            for component in discovery-service gateway-service user-service product-service media-service frontend; do
                                context="backend/$component"
                                if [ "$component" = frontend ]; then context=frontend; fi
                                docker build \
                                    --label "org.opencontainers.image.source=$SOURCE_URL" \
                                    --label "org.opencontainers.image.revision=$VERIFIED_COMMIT" \
                                    --tag "$GHCR_PREFIX/ecom-$component:$IMAGE_TAG" "$context"
                            done
                        '''
                    }
                }
                stage('Push images') {
                    steps {
                        withCredentials([usernamePassword(credentialsId: 'ghcr-push',
                            usernameVariable: 'GHCR_USER', passwordVariable: 'GHCR_TOKEN')]) {
                            sh '''
                                set +x
                                set -eu
                                export DOCKER_CONFIG="$(mktemp -d)"
                                trap 'rm -rf "$DOCKER_CONFIG"' EXIT
                                trap 'exit 1' HUP INT TERM
                                printf '%s' "$GHCR_TOKEN" | docker login ghcr.io --username "$GHCR_USER" --password-stdin
                                unset GHCR_TOKEN
                                for component in discovery-service gateway-service user-service product-service media-service frontend; do
                                    image="$GHCR_PREFIX/ecom-$component:$IMAGE_TAG"
                                    docker push "$image"
                                done
                            '''
                        }
                    }
                }
            }
        }
        stage('Approve deployment') {
            steps {
                timeout(time: 15, unit: 'MINUTES') {
                    input message: "Deploy ${env.IMAGE_TAG} (${env.VERIFIED_COMMIT}) to https://${env.DEPLOY_TARGET}?",
                        ok: 'Deploy'
                }
            }
        }
        stage('Deploy to Oracle') {
            agent { label 'arm64' }
            options { timeout(time: 25, unit: 'MINUTES') }
            steps {
                deleteDir()
                checkout scm
                sh 'git checkout --detach "$VERIFIED_COMMIT"'
                withCredentials([
                    sshUserPrivateKey(credentialsId: 'oracle-deploy-key', keyFileVariable: 'DEPLOY_KEY', usernameVariable: 'DEPLOY_SSH_USER'),
                    file(credentialsId: 'oracle-known-hosts', variable: 'DEPLOY_KNOWN_HOSTS')
                ]) {
                    sh '''
                        set +x
                        set -eu
                        incoming="$DEPLOY_DIR/.deploy/incoming"
                        git archive "$VERIFIED_COMMIT" docker-compose.yml deploy/deploy.sh | \
                            ssh -i "$DEPLOY_KEY" -p "$DEPLOY_PORT" \
                                -o BatchMode=yes -o IdentitiesOnly=yes -o StrictHostKeyChecking=yes \
                                -o UserKnownHostsFile="$DEPLOY_KNOWN_HOSTS" -o ConnectTimeout=10 \
                                "$DEPLOY_SSH_USER@$DEPLOY_HOST" \
                                "set -eu; umask 077; mkdir -p '$incoming'; trap 'rm -rf $incoming' EXIT; tar -xf - -C '$incoming'; bash '$incoming/deploy/deploy.sh' '$IMAGE_TAG' '$DEPLOY_DIR' '$incoming/docker-compose.yml'"
                    '''
                }
            }
            post {
                success { echo "Deployed ${env.IMAGE_TAG} to https://${env.DEPLOY_TARGET}" }
                failure { echo 'Deployment failed. Check the rollback result in this build log.' }
                aborted { echo 'Deployment interrupted. Check production health before retrying.' }
            }
        }
    }
}
