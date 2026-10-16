COMPOSE_ENV := .env
.DEFAULT_GOAL := dev-up
COMPOSE_DEV := docker compose --env-file $(COMPOSE_ENV) -f docker-compose.dev.yml
COMPOSE_PROD := docker compose --env-file .env -f docker-compose.yml
DEV_INFRA_SERVICES := mongo minio discovery-service kafka
BACKEND_SERVICES := discovery-service gateway-service user-service product-service media-service

.PHONY: dev-up dev-down dev-down-v dev-infra-up dev-infra-down kafka-up kafka-logs kafka-topics verify verify-backend verify-frontend prod-config prod-pull prod-up prod-down prod-ps prod-logs

prod-config:
	$(COMPOSE_PROD) config --quiet

prod-pull:
	$(COMPOSE_PROD) pull

prod-up:
	$(COMPOSE_PROD) up -d --no-build --wait --wait-timeout 300

prod-down:
	$(COMPOSE_PROD) down

prod-ps:
	$(COMPOSE_PROD) ps

prod-logs:
	$(COMPOSE_PROD) logs --tail=100 -f

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
