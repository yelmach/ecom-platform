import com.cloudbees.plugins.credentials.CredentialsScope
import com.cloudbees.plugins.credentials.SystemCredentialsProvider
import com.cloudbees.plugins.credentials.domains.Domain
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl

def username = System.getenv('GHCR_USERNAME')
def token = System.getenv('GHCR_TOKEN')
if (!username || !token) {
    println 'GHCR credential setup skipped: set GHCR_USERNAME and GHCR_TOKEN.'
    return
}

def provider = SystemCredentialsProvider.getInstance()
def store = provider.getStore()
def existing = store.getCredentials(Domain.global()).find { it.id == 'ghcr-push' }
def credential = new UsernamePasswordCredentialsImpl(
    CredentialsScope.GLOBAL, 'ghcr-push',
    'Publish ecom application images to GHCR', username, token
)
if (existing) {
    store.updateCredentials(Domain.global(), existing, credential)
} else {
    store.addCredentials(Domain.global(), credential)
}
println 'Configured ghcr-push credential.'
