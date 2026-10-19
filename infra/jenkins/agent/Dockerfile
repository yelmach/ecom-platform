FROM node:24-bookworm-slim AS node

FROM jenkins/inbound-agent:jdk21

USER root

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
       git curl ca-certificates make chromium \
       python3 build-essential libatomic1 \
    && rm -rf /var/lib/apt/lists/*

COPY --from=node /usr/local/bin/node /usr/local/bin/node
COPY --from=node /usr/local/lib/node_modules/ /usr/local/lib/node_modules/

RUN ln -s /usr/local/lib/node_modules/npm/bin/npm-cli.js /usr/local/bin/npm \
    && ln -s /usr/local/lib/node_modules/npm/bin/npx-cli.js /usr/local/bin/npx

ENV CHROME_BIN=/usr/bin/chromium

USER jenkins
RUN mkdir -p /home/jenkins/agent /home/jenkins/.m2 /home/jenkins/.npm
WORKDIR /home/jenkins/agent
