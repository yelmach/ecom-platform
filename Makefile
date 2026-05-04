COMPOSE_ENV := backend/docker.env
COMPOSE_PROD := docker compose --env-file $(COMPOSE_ENV) -f docker-compose.yml
COMPOSE_DEV := docker compose --env-file $(COMPOSE_ENV) -f docker-compose.dev.yml
COMPOSE_SONAR := docker compose --env-file $(COMPOSE_ENV) -f sonarQube/docker-compose.yml

.PHONY: prod-up prod-down prod-down-v dev-infra-up dev-infra-down jenkins-up jenkins-down sonar-up sonar-down sonar-logs

prod-up:
	$(COMPOSE_PROD) up --build -d

prod-down:
	$(COMPOSE_PROD) down

prod-down-v:
	$(COMPOSE_PROD) down -v

dev-infra-up:
	$(COMPOSE_DEV) up --build -d

dev-infra-down:
	$(COMPOSE_DEV) down

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
