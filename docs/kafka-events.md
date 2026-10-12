# Kafka cleanup events

Topics are created at application startup using Spring Kafka `NewTopic` beans:

| Application | Topic | Kafka record key |
| --- | --- | --- |
| Product service | `product.events` | Product ID |
| User service | `user.events` | User ID |

Each topic has one partition, replication factor 1, and seven-day retention
with `cleanup.policy=delete`. These settings suit the local single-broker setup.
Spring Boot configures `KafkaAdmin` automatically and creates missing topics
from the beans. Existing topics are reused.

Start Kafka with `make kafka-up`, then start the applications. They use
`KAFKA_BOOTSTRAP_SERVERS` from `.env` (Docker: `kafka:9092`) or `.env.local`
(IDE: `localhost:29092`). Startup fails if Kafka is unavailable.
Start product service before media service so its topic exists.
List the topics after starting product and user services with `make kafka-topics`.

Topic names and event payloads are unversioned for this learning project.
Older `.v1` topics are separate topics; their records are not migrated.

Events use JSON values with `eventId`, `eventType`, `occurredAt` (UTC), and `payload`:

| Event | Payload | Trigger |
| --- | --- | --- |
| `PRODUCT_DELETED` | `productId` | Successful product deletion; clean up all product-owned media |
| `PRODUCT_IMAGES_DETACHED` | `productId`, removed `mediaIds` | Successful update that removes image references |
| `USER_AVATAR_CHANGED` | `userId`, `previousMediaId`, `currentMediaId` | Successful avatar replacement or clearing |

Avatar IDs can be null: first selection has no previous avatar; clearing has no
current avatar. Uploading an image alone does not trigger cleanup. Reordering
product images or saving the same avatar emits no cleanup event.

## Product deletion (implemented)

`DELETE /products/{id}` checks seller ownership, physically removes the product
from MongoDB, and directly publishes `PRODUCT_DELETED` using the product ID as
the Kafka key. It waits for the publish attempt and returns `204` even if
publication fails after the product is deleted. Updates remain
conditional MongoDB updates so a concurrent edit cannot recreate the product.

There is no outbox, stored pending event, producer scheduler, or product
tombstone. If publishing fails, the producer logs the error and the API still
returns `204`. The database deletion is not rolled back and no
application-level replay is queued. A failed publish or a crash
between deletion and publication can also leave images behind. Retrying DELETE
will return `404` once the product has been removed.

Media consumes the topic using group `media-product-cleanup`. It validates
deletion events, then deletes all product-owned images, including unlinked uploads. Each MinIO object is
deleted before its MongoDB metadata. Missing objects/images are safe on retry;
avatars and other products are not selected by the cleanup query.
A final sweep of `products/{productId}/` removes objects lacking metadata after
an interrupted upload. The trailing slash keeps neighboring product IDs out
of that sweep; avatar objects live under a different prefix.

Storage/database failures receive three retries, five seconds apart, after the
initial attempt (four attempts total). After retries are exhausted, the failed
event is logged and skipped; remaining images may be left behind and no replay
is queued. Invalid deletion events are logged and skipped without retry;
other event types are ignored. Offsets advance after successful processing or
after a failed event is skipped. New consumers start at the earliest retained
event when their group has no valid committed offset.

Uploads still verify ownership with product service before storing files.
There are no deletion markers or scheduled reconciliation. An upload already
in progress can finish after cleanup and leave an image behind.

The seven-day Kafka retention bounds recovery when Media has never consumed a
deletion event. A cleanup failure delays subsequent records in that partition
while retries run; consumption continues after success or retry exhaustion.

Avatar and detached-image events remain planned; their producers and consumers
are not implemented in this change.

Reference: [Spring Kafka topic configuration](https://docs.spring.io/spring-kafka/reference/kafka/configuring-topics.html).
