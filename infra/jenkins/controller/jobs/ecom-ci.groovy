def repositoryUrl = System.getenv('JENKINS_REPOSITORY_URL')
def repositoryBranch = System.getenv('JENKINS_REPOSITORY_BRANCH')

pipelineJob('ecom-ci') {
    description('Build, test and analyze the app, then enforce its SonarQube quality gate.')
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
            scriptPath('Jenkinsfile')
            lightweight(true)
        }
    }
}
