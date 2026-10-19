def repositoryUrl = System.getenv('JENKINS_REPOSITORY_URL')
def repositoryBranch = System.getenv('JENKINS_REPOSITORY_BRANCH')
def gitCredentialsId = System.getenv('JENKINS_GIT_CREDENTIALS_ID')

pipelineJob('agent-check') {
    description('Verify the build agent and optionally run the project checks. Managed in Git.')
    parameters {
        booleanParam('RUN_PROJECT_CHECKS', false, 'Also run make verify after checking the agent and checkout.')
    }
    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url(repositoryUrl)
                        if (gitCredentialsId) {
                            credentials(gitCredentialsId)
                        }
                    }
                    branch('*/' + repositoryBranch)
                }
            }
            scriptPath('infra/jenkins/agent-check.Jenkinsfile')
            lightweight(true)
        }
    }
}
