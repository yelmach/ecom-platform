FROM jenkins/jenkins:lts
USER root

# Install dependencies to add the Docker repository
RUN apt-get update && apt-get install -y lsb-release curl ca-certificates chromium

# Add Docker's official GPG key and repository
RUN curl -fsSLo /usr/share/keyrings/docker-archive-keyring.asc https://download.docker.com/linux/debian/gpg

RUN echo "deb [arch=$(dpkg --print-architecture) \
  signed-by=/usr/share/keyrings/docker-archive-keyring.asc] \
  https://download.docker.com/linux/debian \
  $(lsb_release -cs) stable" > /etc/apt/sources.list.d/docker.list

# Install Docker CLI and Docker Compose V2 plugin
RUN apt-get update && apt-get install -y docker-ce-cli docker-compose-plugin

USER jenkins
