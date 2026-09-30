COMPOSE_ENV := .env
COMPOSE_DEV := docker compose --env-file $(COMPOSE_ENV) -f docker-compose.dev.yml
COMPOSE_SONAR := docker compose --env-file $(COMPOSE_ENV) -f sonarQube/docker-compose.yml
DEV_INFRA_SERVICES := mongo minio discovery-service

.PHONY: dev-up dev-down dev-down-v dev-infra-up dev-infra-down jenkins-up jenkins-down sonar-up sonar-down sonar-logs

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

jenkins-up:
	docker compose -f jenkins/docker-compose.yml up --build -d

jenkins-down:
	docker compose -f jenkins/docker-compose.yml down

sonar-up:
	$(COMPOSE_SONAR) up -d

sonar-down:
	$(COMPOSE_SONAR) down

sonar-logs:
	$(COMPOSE_SONAR) logs -f
