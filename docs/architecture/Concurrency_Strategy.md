# Concurrency and Locking Strategy

## Current Implementation

The SentinelAI platform currently employs an **in-memory pessimistic locking strategy** utilizing `ConcurrentHashMap<Long, ReentrantLock>` to prevent race conditions during:
1. Duplicate concurrent project scan triggers (`ScanServiceImpl`).
2. Duplicate concurrent AI enrichment tasks (`AiEnrichmentServiceImpl`).
3. Duplicate concurrent report generation tasks (`ReportServiceImpl`).

### Why this approach was selected
- **Simplicity:** It provides immediate protection against duplicate submissions without requiring additional infrastructure (like Redis) or complex database schema modifications.
- **Low Overhead:** In-memory locking is extremely fast and avoids database round-trips for lock acquisition.
- **Thread Safety:** It ensures strict concurrency control for async events and HTTP requests hitting a single JVM.

## Limitations

**This locking mechanism is intentionally designed for a single Spring Boot instance deployment.**

Because `ConcurrentHashMap` and `ReentrantLock` are bound to the heap of the JVM they are instantiated in, they cannot synchronize state across multiple application instances.

In a horizontally scaled environment (e.g., Load Balancer -> Backend A and Backend B), if User 1 hits Backend A and User 2 hits Backend B concurrently for the same project, the locks will be completely independent, causing the concurrency protection to fail. (A secondary database check like `existsByProjectIdAndStatusIn` provides partial fallback protection, but is still vulnerable to precise race conditions between distributed nodes).

## Recommended Migration Path for Horizontal Scaling

Before scaling the backend application to multiple instances, this in-memory locking strategy **must be migrated** to a distributed locking model. Recommended options include:

1. **Distributed Locks (Redis/Zookeeper):** Use ShedLock or Redisson to coordinate locks across all nodes. This requires minimal code changes and provides high performance.
2. **Database-Backed Locking:** Use optimistic locking (e.g., adding an `@Version` column to the `Project` or `ScanHistory` entities) or pessimistic row-level locking (`SELECT ... FOR UPDATE`) in PostgreSQL/MySQL.
3. **Job Queues:** Offload execution to a message broker (RabbitMQ/Kafka) or a task queue (like Quartz or Temporal) that natively handles job deduplication and state orchestration.
