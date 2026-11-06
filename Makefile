COMPOSE_ENV := .env
.DEFAULT_GOAL := dev-up
COMPOSE_DEV := docker compose --env-file $(COMPOSE_ENV) -f docker-compose.dev.yml
COMPOSE_JENKINS := DOCKER_GID="$$(stat -c '%g' /var/run/docker.sock)" docker compose --env-file .env -f infra/jenkins/docker-compose.yml
COMPOSE_SONAR := docker compose --env-file .env -f infra/sonarqube/docker-compose.yml
DEV_INFRA_SERVICES := mongo minio discovery-service kafka
BACKEND_SERVICES := discovery-service gateway-service user-service product-service media-service

.PHONY: dev-up dev-down dev-down-v dev-infra-up dev-infra-down kafka-up kafka-logs kafka-topics verify verify-backend verify-frontend
.PHONY: jenkins-config jenkins-up jenkins-agent-up jenkins-down jenkins-ps jenkins-logs jenkins-agent-logs jenkins-agent-check
.PHONY: sonarqube-config sonarqube-build sonarqube-up sonarqube-ps sonarqube-logs sonarqube-down sonar-scan

sonarqube-config:
	$(COMPOSE_SONAR) config --quiet

sonarqube-build:
	$(COMPOSE_SONAR) build --pull sonarqube

sonarqube-up:
	$(COMPOSE_SONAR) up -d --build --wait --wait-timeout 420

sonarqube-ps:
	$(COMPOSE_SONAR) ps

sonarqube-logs:
	$(COMPOSE_SONAR) logs --tail=100 -f

sonarqube-down:
	$(COMPOSE_SONAR) down

sonar-scan:
	sonar-scanner -Dsonar.java.jdkHome="$$JAVA_HOME"

jenkins-config:
	$(COMPOSE_JENKINS) config --quiet

jenkins-up:
	$(COMPOSE_JENKINS) up -d --build --wait --wait-timeout 300 jenkins

jenkins-agent-up:
	$(COMPOSE_JENKINS) up -d --build agent

jenkins-down:
	$(COMPOSE_JENKINS) down

jenkins-ps:
	$(COMPOSE_JENKINS) ps

jenkins-logs:
	$(COMPOSE_JENKINS) logs --tail=100 -f jenkins

jenkins-agent-logs:
	$(COMPOSE_JENKINS) logs --tail=100 -f agent

jenkins-agent-check:
	$(COMPOSE_JENKINS) exec -T agent sh -ec 'uname -m; java -version; node --version; npm --version; git --version; make --version; "$$CHROME_BIN" --version; sonar-scanner --version; docker version; docker buildx version; docker buildx inspect default; ssh -V; test -w /home/jenkins/agent; test -w /home/jenkins/.m2; test -w /home/jenkins/.npm; test -w "$$SONAR_USER_HOME"'

# Keep frontend build and tests sequential, including when make is run with -j.
verify:
	$(MAKE) verify-backend
	$(MAKE) verify-frontend

verify-backend:
	@set -e; for service in $(BACKEND_SERVICES); do \
		printf '\nVerifying %s\n' "$$service"; \
		(cd "backend/$$service" && ./mvnw -B -ntp clean verify); \
	done

verify-frontend:
	cd frontend && npm ci --no-audit --no-fund && npm run build && npm run test:ci

dev-up:
	$(COMPOSE_DEV) up --build -d

dev-down:
	$(COMPOSE_DEV) down

dev-down-v:
	$(COMPOSE_DEV) down -v

dev-infra-up:
	$(COMPOSE_DEV) up --build -d $(DEV_INFRA_SERVICES)

dev-infra-down:
	$(COMPOSE_DEV) stop $(DEV_INFRA_SERVICES)
	$(COMPOSE_DEV) rm -f $(DEV_INFRA_SERVICES)

kafka-up:
	$(COMPOSE_DEV) up -d --wait kafka

kafka-logs:
	$(COMPOSE_DEV) logs -f kafka

kafka-topics:
	$(COMPOSE_DEV) exec -T kafka sh -c 'endpoint=$${KAFKA_ADVERTISED_LISTENERS%%,*}; /opt/kafka/bin/kafka-topics.sh --bootstrap-server "$${endpoint#INTERNAL://}" --list'
