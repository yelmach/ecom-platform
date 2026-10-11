# Kafka cleanup events

Topics are created at application startup using Spring Kafka `NewTopic` beans:

| Application | Topic | Kafka record key |
| --- | --- | --- |
| Product service | `product.events.v1` | Product ID |
| User service | `user.events.v1` | User ID |

Each topic has one partition, replication factor 1, and seven-day retention
with `cleanup.policy=delete`. These settings suit the local single-broker setup.
Spring Boot configures `KafkaAdmin` automatically and creates missing topics
from the beans. Existing topics are reused.

Start Kafka with `make kafka-up`, then start the applications. They use
`KAFKA_BOOTSTRAP_SERVERS` from `.env` (Docker: `kafka:9092`) or `.env.local`
(IDE: `localhost:29092`). Startup fails if Kafka is unavailable.
List the topics after starting both applications with `make kafka-topics`.

The planned events use JSON values with `eventId`, `eventType`, `schemaVersion`
(`1`), `occurredAt` (UTC), and `payload`:

| Event | Payload | Trigger |
| --- | --- | --- |
| `PRODUCT_DELETED` | `productId` | Successful product deletion; clean up all product-owned media |
| `PRODUCT_IMAGES_DETACHED` | `productId`, removed `mediaIds` | Successful update that removes image references |
| `USER_AVATAR_CHANGED` | `userId`, `previousMediaId`, `currentMediaId` | Successful avatar replacement or clearing |

Avatar IDs can be null: first selection has no previous avatar; clearing has no
current avatar. Uploading an image alone does not trigger cleanup. Reordering
product images or saving the same avatar emits no cleanup event.

Producers and Media cleanup consumers are not implemented yet.

Reference: [Spring Kafka topic configuration](https://docs.spring.io/spring-kafka/reference/kafka/configuring-topics.html).
