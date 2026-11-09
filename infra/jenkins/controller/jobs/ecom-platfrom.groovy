def sourceUrl = System.getenv('JENKINS_REPOSITORY_URL') ?: ''
def parts = sourceUrl.replaceFirst('^https://github.com/', '').replaceFirst(/\.git$/, '').tokenize('/')
if (!sourceUrl.startsWith('https://github.com/') || parts.size() != 2) {
    throw new IllegalArgumentException('JENKINS_REPOSITORY_URL must be https://github.com/OWNER/REPOSITORY[.git]')
}

multibranchPipelineJob('ecom-platfrom') {
    description('Branch and merged-PR CI with SonarQube; successful main builds publish images and deploy automatically.')
    branchSources {
        branchSource {
            source {
                github {
                    id('ecom-github-source')
                    apiUri('https://api.github.com')
                    repoOwner(parts[0])
                    repository(parts[1])
                    repositoryUrl(sourceUrl)
                    configuredByUrl(true)
                    credentialsId('github-app-ecom')
                    traits {
                        // Build every branch, including branches with an open PR.
                        gitHubBranchDiscovery { strategyId(3) }
                        // Build the PR merged with its current target; no fork trait.
                        gitHubPullRequestDiscovery { strategyId(1) }
                    }
                }
            }
        }
    }
    factory {
        workflowBranchProjectFactory {
            scriptPath('Jenkinsfile')
        }
    }
    orphanedItemStrategy {
        discardOldItems {
            daysToKeep(7)
            numToKeep(-1)
        }
    }
}
