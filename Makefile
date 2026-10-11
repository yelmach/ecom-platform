COMPOSE_ENV := .env
COMPOSE_DEV := docker compose --env-file $(COMPOSE_ENV) -f docker-compose.dev.yml
COMPOSE_SONAR := docker compose --env-file $(COMPOSE_ENV) -f sonarQube/docker-compose.yml
DEV_INFRA_SERVICES := mongo minio discovery-service kafka

.PHONY: dev-up dev-down dev-down-v dev-infra-up dev-infra-down kafka-up kafka-logs kafka-topics jenkins-up jenkins-down sonar-up sonar-down sonar-logs

dev-up:
	$(COMPOSE_DEV) up --build -d

dev-down:
	$(COMPOSE_DEV) down

dev-down-v:
	$(COMPOSE_DEV) down -vdo

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

kafka-up:
	$(COMPOSE_DEV) up -d --wait kafka

kafka-logs:
	$(COMPOSE_DEV) logs -f kafka

kafka-topics:
	$(COMPOSE_DEV) exec -T kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --list
