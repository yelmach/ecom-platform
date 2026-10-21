FROM node:24-bookworm-slim AS node

FROM jenkins/inbound-agent:jdk21

USER root

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
    git curl ca-certificates make chromium \
    python3 build-essential libatomic1 unzip \
    && rm -rf /var/lib/apt/lists/*

COPY --from=node /usr/local/ /usr/local/

RUN curl -fsSL https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-8.1.0.6389.zip -o /tmp/sonar-scanner.zip \
    && unzip -q /tmp/sonar-scanner.zip -d /opt \
    && rm /tmp/sonar-scanner.zip

ENV PATH="/opt/sonar-scanner-8.1.0.6389/bin:${PATH}" \
    CHROME_BIN=/usr/bin/chromium \
    SONAR_USER_HOME=/home/jenkins/.sonar

USER jenkins
RUN mkdir -p /home/jenkins/agent /home/jenkins/.m2 /home/jenkins/.npm /home/jenkins/.sonar
WORKDIR /home/jenkins/agent
