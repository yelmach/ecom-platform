def repositoryUrl = System.getenv('JENKINS_REPOSITORY_URL')
def repositoryBranch = System.getenv('JENKINS_REPOSITORY_BRANCH')

pipelineJob('agent-check') {
    description('Verify the build agent and optionally run the project checks.')
    parameters {
        booleanParam('RUN_PROJECT_CHECKS', false, 'Also run make verify after checking the agent and checkout.')
    }
    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url(repositoryUrl)
                    }
                    branch('*/' + repositoryBranch)
                }
            }
            scriptPath('infra/jenkins/agent-check.Jenkinsfile')
            lightweight(true)
        }
    }
}
