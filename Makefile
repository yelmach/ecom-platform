COMPOSE_ENV := backend/docker.env
COMPOSE_PROD := docker compose --env-file $(COMPOSE_ENV) -f docker-compose.yml
COMPOSE_DEV := docker compose --env-file $(COMPOSE_ENV) -f docker-compose.dev.yml

.PHONY: prod-up prod-down prod-down-v dev-infra-up dev-infra-down

prod-up:
	$(COMPOSE_PROD) up --build

prod-down:
	$(COMPOSE_PROD) down

prod-down-v:
	$(COMPOSE_PROD) down -v

dev-infra-up:
	$(COMPOSE_DEV) up --build

dev-infra-down:
	$(COMPOSE_DEV) down
