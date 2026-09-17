# AGENTS.md

Guidance for AI agents working in this repository.

## What this project is

Spring Content is a multi-module Maven library that adds content-management patterns to Spring: associate binary content
with Spring Data entities, store it in pluggable backends, optionally expose it over Spring Data REST, and add search,
renditions, encryption, and locking/versioning.

- Group ID: `io.github.vsvyatski`
- Current version: `4.1.0-SNAPSHOT` (keep all modules on the parent version)
- Java 17, Spring Boot 4.x (`spring-boot-dependencies` BOM), Jakarta Persistence 3.2
- License: Apache 2.0
- Fork of Paul Warren’s project; this tree targets Spring Boot 4 compatibility

Public Java APIs live under `org.springframework.content.*` and `org.springframework.versions.*` even though Maven
coordinates are `io.github.vsvyatski`.

## Repository layout

Parent POM: `pom.xml`. Build with the Maven wrapper (`./mvnw` on Unix, `.\mvnw.cmd` on Windows).

### Core

| Module                         | Role                                                                |
|--------------------------------|---------------------------------------------------------------------|
| `spring-content-commons`       | Store APIs, annotations, mapping, events, store factory/interceptor |
| `spring-content-autoconfigure` | Spring Boot auto-config (Boot 3+ `AutoConfiguration.imports`)       |
| `spring-content-bom`           | Bill of materials for consumers                                     |

### Storage backends

Each backend has public `Enable*Stores` annotations, store marker interfaces, and an
`internal.org.springframework.content.*` implementation.

| Module                         | Enable annotation         | Typical store type       |
|--------------------------------|---------------------------|--------------------------|
| `spring-content-fs`            | `@EnableFileSystemStores` | `FileSystemContentStore` |
| `spring-content-jpa`           | `@EnableJpaStores`        | `JpaContentStore`        |
| `spring-content-mongo`         | `@EnableMongoStores`      | GridFS `ContentStore`    |
| `spring-content-s3`            | `@EnableS3Stores`         | S3 `ContentStore`        |
| `spring-content-gcs`           | `@EnableGCPStorage`       | GCS store                |
| `spring-content-azure-storage` | `@EnableAzureStorage`     | Azure Blob store         |

JPA ships vendor SQL under `spring-content-jpa/src/main/resources/org/springframework/content/jpa/` (H2, HSQLDB, MySQL,
PostgreSQL, SQL Server, Oracle).

### Cross-cutting modules

| Module                                                 | Role                                                                |
|--------------------------------------------------------|---------------------------------------------------------------------|
| `spring-content-rest`                                  | HTTP content endpoints + HAL links for Spring Data REST             |
| `spring-content-solr` / `spring-content-elasticsearch` | Full-text indexing                                                  |
| `spring-content-renditions`                            | Pluggable content conversion                                        |
| `spring-content-encryption`                            | Encrypting store mixin (`EncryptingContentStore`)                   |
| `spring-versions-commons` / `spring-versions-jpa`      | Locking and versioning (`@LockParticipant`, ancestor/successor IDs) |

`spring-content-docx4j` exists on disk but is **commented out** of the reactor. Do not re-enable it unless that is the
task.

### Boot starters

Prefer the current names: `spring-content-*-boot-starter`.

Legacy aliases still in the reactor (same dependencies, keep in sync if you change a starter):

- `content-fs-spring-boot-starter`
- `content-jpa-spring-boot-starter`
- `content-mongo-spring-boot-starter`
- `content-rest-spring-boot-starter`
- `content-s3-spring-boot-starter`
- `content-solr-spring-boot-starter`

GCS, Azure, and encryption have **no** boot starters. Auto-config currently covers FS, JPA, Mongo, S3, REST, Solr,
Elasticsearch, renditions, and JPA versions.

## Architecture agents must respect

### Store hierarchy (use the non-deprecated types)

Prefer `org.springframework.content.commons.store`:

```
Store<SID>
  └── AssociativeStore<S, SID>
        └── ContentStore<S, SID>
```

- `Store`: `getResource(id)`
- `AssociativeStore`: associate/unassociate entity ↔ content, including `PropertyPath` for nested content properties
- `ContentStore`: `setContent` / `getContent` / `unsetContent`

`org.springframework.content.commons.repository.ContentStore` (and related types) are **deprecated**. New code should
use `org.springframework.content.commons.store`. Store implementations often still implement both for compatibility; do
not drop the old interfaces without an explicit migration.

Backend-specific store interfaces (e.g. `FileSystemContentStore`) extend the commons `ContentStore` and are the types
applications declare. Implementations are created by `*StoreFactoryBean` + method interceptor, not by users
instantiating `Default*StoreImpl`.

### Entity mapping

Content metadata on entities uses annotations in `org.springframework.content.commons.annotations`:

- `@ContentId`, `@ContentLength`, `@MimeType`, `@OriginalFileName`
- Store event handlers: `@StoreEventHandler` plus `@HandleBefore*` / `@HandleAfter*`

### Packages

- Public API: `org.springframework.content.<module>...`, `org.springframework.versions...`, some REST extensions under
  `org.springframework.data.rest.extensions`
- Internals: `internal.org.springframework.content...` and `internal.org.springframework.versions...`

Do not move types out of `internal.*` without a versioning plan. Application-facing configuration stays on the public
`Enable*` / `*Configurer` types; factory beans and default store impls stay internal.

### REST

`spring-content-rest` scans `internal.org.springframework.content.rest.controllers` and Spring Data REST extension
packages. Public knobs: `RestConfiguration`, `ContentRestConfigurer`, `@StoreRestResource`, `@RestResource`. Match
existing content-link and byte-range behavior; many ITs lock that down.

### Boot auto-config

Registered in
`spring-content-autoconfigure/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
Storage type selection uses `spring.content.storage.type.default` (FS is the default when missing). Keep
`@ConditionalOnClass` / `@ConditionalOnMissingBean` patterns when adding auto-config.

## Build and test

JDK 17. Maven 3.6.3+ if not using the wrapper.

```text
./mvnw clean install                    # default: unit tests only (*Test.java)
./mvnw -P tests clean install           # units + ITs (*IT.java, *Tests.java)
./mvnw -P docs clean install            # AsciiDoc reference docs
```

S3-related tests expect `AWS_REGION` (CI uses `us-west-1`). Set it locally when running those modules.

Profiles:

- `dev` — default docs output path
- `tests` — Surefire units on `test`, ITs on `integration-test`, JaCoCo
- `docs` — AsciiDoctor → `target/generated-docs/refs/...`
- `ci` — Central publish, sources/javadoc jars, GPG

CI: `.github/workflows/prs.yml` (PR: `-P tests` then getting-started guides) and `maven.yml` (main/tags).

Change version for the whole reactor with:

```text
./mvnw versions:set -DnewVersion=...
```

### Tests

- Framework: **Ginkgo4j** (JUnit 4 runner), Hamcrest, Mockito, Testcontainers where needed
- `@RunWith(Ginkgo4jRunner.class)` for plain unit tests
- `@RunWith(Ginkgo4jSpringRunner.class)` when a Spring context is required
- `@Ginkgo4jConfiguration(threads = 1)` when tests share mutable context or embedded DBs
- DSL: `Describe` / `Context` / `BeforeEach` / `JustBeforeEach` / `It` from
  `com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL`
- Maven artifact is `io.github.vsvyatski:ginkgo4j`; **keep the `com.github.paulcwarren.ginkgo4j` Java package** unless
  the library itself is migrated
- Put the spec in an instance initializer `{ Describe(...); }`, not in `@Test` methods (except rare ignored JUnit
  leftovers)
- Naming: `*Test.java` = unit (always); `*IT.java` = integration (`-P tests` only)
- Follow existing IT style: inner entity/store/repo types, `@Enable*Stores` + `@EnableJpaRepositories` test configs,
  assert streams with Commons IO + Hamcrest

When changing a store implementation, run that module’s `*Test` and, if behavior or IO/SQL is involved, the matching
`*IT`.

## Coding conventions

- Match surrounding Spring Framework style (formatter: Eclipse `eclipse-code-formatter.xml` when present). Tabs vs
  spaces: copy the file you are editing.
- New `.java` files: Apache 2.0 header (copy from a neighboring file), class Javadoc with `@author`. Substantial edits:
  add `@author`.
- Prefer existing utilities: `BeanUtils`, `PlacementService`, `MappingContext`, `PropertyPath`, Commons IO.
- Store mutators that change content are `@LockParticipant` so versioning/locking still applies.
- Do not add new public APIs in `internal.*`. Do not break binary compatibility of public store interfaces without
  deprecation.
- Dual boot starters: if you change `spring-content-fs-boot-starter`, apply the same change to
  `content-fs-spring-boot-starter` (and the same pairing for jpa/mongo/rest/s3/solr).

## Docs

Per-module AsciiDoc lives under `src/main/asciidoc/`. High-level notes in `README.md` and `CONTRIBUTING.md`.
`spring-content.md` is historical; trust the reactor `pom.xml` over that file for which modules exist.

## PR / commit expectations (from CONTRIBUTING)

- Commit messages in the tbaggery style; `Fixes gh-XXXX` when closing an issue
- Rebase onto the target branch when possible
- CLA required for upstream contributions
- Do not report security issues in GitHub issues

## Agent roles (this repo)

- **Implement:** Grok in the parent chat. Pick Grok in the model picker.
- **Review:** Composer (`composer-2.5-fast` subagent) after code changes, or when asked. See
  `.cursor/rules/grok-dev-composer-review.mdc`.

## Agent do / don’t

Do:

- Touch the smallest set of modules that implement the change
- Mirror patterns from a sibling backend when adding store features (FS and JPA are the usual templates)
- Keep deprecated `commons.repository` types working alongside `commons.store`

Don’t:

- Enable `spring-content-docx4j` or rewrite Boot 2 `spring.factories` as the primary auto-config path
- Replace Ginkgo4j tests with JUnit 5 Jupiter unless the task is a test-framework migration
- Change `groupId` / public package names as a drive-by
- Skip `-P tests` when the change affects storage, REST mapping, or SQL dialects
