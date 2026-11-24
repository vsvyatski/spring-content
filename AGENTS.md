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

| Module                         | Enable annotation         | Typical store type                      |
|--------------------------------|---------------------------|-----------------------------------------|
| `spring-content-fs`            | `@EnableFileSystemStores` | `FileSystemContentStore`                |
| `spring-content-jpa`           | `@EnableJpaStores`        | `JpaContentStore`                       |
| `spring-content-mongo`         | `@EnableMongoStores`      | GridFS `ContentStore`                   |
| `spring-content-s3`            | `@EnableS3Stores`         | S3 `ContentStore`                       |
| `spring-content-gcs`           | `@EnableGCPStorage`       | GCS store                               |
| `spring-content-azure-storage` | `@EnableAzureStorage`     | Azure Blob store (Spring Cloud Azure 7) |

JPA ships vendor SQL under `spring-content-jpa/src/main/resources/org/springframework/content/jpa/` (H2, HSQLDB, MariaDB,
PostgreSQL, SQL Server).

### Cross-cutting modules

| Module                                                 | Role                                                                |
|--------------------------------------------------------|---------------------------------------------------------------------|
| `spring-content-rest`                                  | HTTP content endpoints + HAL links for Spring Data REST             |
| `spring-content-solr` / `spring-content-elasticsearch` | Full-text indexing                                                  |
| `spring-content-renditions`                            | Pluggable content conversion                                        |
| `spring-content-encryption`                            | Encrypting store mixin (`EncryptingContentStore`)                   |
| `spring-versions-commons` / `spring-versions-jpa`      | Locking and versioning (`@LockParticipant`, ancestor/successor IDs) |

`spring-content-docx4j` converts Word documents to text, HTML, and PDF. Renditions auto-configuration imports it
when `Docx4jConfiguration` is on the classpath.

### Boot starters

Use `spring-content-*-boot-starter`.

GCS, Azure, and encryption have **no** boot starters. Auto-config currently covers FS, JPA, Mongo, S3, REST, Solr,
Elasticsearch, renditions, and JPA versions.

## Architecture agents must respect

### Store hierarchy

Use `org.springframework.content.commons.store`:

```
Store<SID>
  └── AssociativeStore<S, SID>
        └── ContentStore<S, SID>
```

- `Store`: `getResource(id)`
- `AssociativeStore`: associate/unassociate entity ↔ content, including `PropertyPath` for nested content properties
- `ContentStore`: `setContent` / `getContent` / `unsetContent`

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

JDK 17. Maven 3.9.0+ if not using the wrapper (the wrapper ships 3.9.x). On Windows with another JDK on `PATH`, source
`scripts/wsl-java17-env.sh` in AlmaLinux WSL before `./mvnw`.

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

CI: `.github/workflows/prs.yml` (PRs into `develop`: `-P tests` then getting-started guides) and `maven.yml` (`develop`/tags).

Change version for the whole reactor with:

```text
./mvnw versions:set -DnewVersion=...
```

Check dependency, property, and plugin updates with `./mvnw` so the run matches the enforcer floor (`[3.9.0,)` on
`maven-enforcer-plugin`):

```text
./mvnw versions:display-dependency-updates
./mvnw versions:display-property-updates
./mvnw -N versions:display-plugin-updates
```

### Tests

- Framework: JUnit Jupiter, AssertJ, Mockito, Testcontainers where needed
- `@Test`, `@BeforeEach`, `@AfterEach`, `@Nested` from `org.junit.jupiter.api`
- Naming: `*Test.java` = unit (always); `*IT.java` = integration (`-P tests` only)
- `@Nested` classes use PascalCase identifiers without articles (`a`, `an`, `the`) or BDD prefixes (`Given`, `When`,
  `Then`) in the type name. Use `@DisplayName` when the old Ginkgo phrase should still
  appear in test reports (for example `class GetWithAcceptWildcard` with `@DisplayName("GET to {store ID} with
  Accept */*")`). HTTP scenario nests name the verb first (`Get`, `Put`, `Post`, `Delete`, `Head`,
  `Options`). Setup nests describe state (`ApplicationSetsId`, `EntityHasTextPlainContent`). Test methods
  may stay `should…` unless you are aligning a file to another style deliberately.
- In `@DisplayName`, comments, and other human-readable test prose, use **sentence case** (like the REST
  `controllers/support` fixtures), not title case on every word: `Given a class with uncorrelated attributes`,
  not `Given a Class With Uncorrelated Attributes`. Capitalize HTTP verbs (`GET`, `PUT`), header names (`If-Match`), and
  the usual **MIME type** / **store ID** spellings; keep articles and prepositions lowercase.
  Place `@Nested`, `@DisplayName`, and the nested `class` on consecutive lines with no blank line between them.
- In prose use **MIME type**, **mime type**, or **mime-type** (two words or hyphenated — never one word
  `mimetype`). In Java identifiers use **`MimeType`** / **`mimeType`** (not `Mimetype`). Write **ID** in prose (not `id`
  in phrases like "store ID"), unless a framework literal requires another spelling (for example a
  fixed header or property name).
- Follow existing IT style: inner entity/store/repo types, `@Enable*Stores` + `@EnableJpaRepositories` test configs,
  assert streams with Commons IO + AssertJ
- Test entities: provide a public no-arg constructor; add parameterized constructors only when a test calls
  `new Entity(...)` with arguments (JPA and Spring Data materialize entities via the no-arg constructor)
- `ContentStore` marker interfaces use a `*Store` suffix (e.g. `TestEntityContentStore`), not `*ContentRepository`.
  Reserve `*Repository` for Spring Data `CrudRepository` / `JpaRepository` types only.
- Shared fixtures live in `spring-content-commons` test-jar (`org.springframework.content.test.support.**`).
  Reuse `TestSupportData` for synthetic content ids, default UTF-8 payloads (`sampleTextStream()`, etc.), and
  `UNASSOCIATED_RESOURCE_LOCATION` instead of `RandomString` or letter-salad literals.
  `@Enable*Stores` unit tests scan `TestSupportPackages` (`.jpa`, `.mongo`, `.fs`, `.s3`, `.gcs`, `.azure`) instead of
  inner `TestEntity` types. `EnableJpaStoresTest` asserts `DualContentJpaStringIdStore` (dual `@ContentId` entity in the
  test-jar); other backends use single-content store marker interfaces. REST module fixtures live under
  `internal.org.springframework.content.rest.support` with purpose names (e.g. `JsonMappedContentEntity`);
  `@RepositoryRestResource(path = …)` uses purpose-driven collection paths aligned with fixture type names (for
  example `jsonMappedContentEntities`).
- Store backend ITs group scenarios under nested `Store`, `AssociativeStore`, and `ContentStore` (not `*Cases`). Drop
  empty `*StorageCases` / `Default*ImplCases` shells; do not hoist nests that own `@BeforeEach` onto the outer IT class.
- Search REST IT fixtures live in `it.rest.extensions.content.search.support` (`SeparateContentIdFields` shared by
  separate content ID entities). `@RepositoryRestResource(path = …)` uses collection paths aligned with fixture type
  names (for example `searchSharedContentIdEntities`).

When changing a store implementation, run that module’s `*Test` and, if behavior or IO/SQL is involved, the matching
`*IT`.

## Coding conventions

- **Java 17 only** — see `.cursor/rules/java-17.mdc`. No Java 8/11 language level, no `javax.*` EE APIs in new code, no
  legacy JDK patterns when modern ones fit.
- Follow repo `.editorconfig`: `insert_final_newline = true` and `trim_trailing_whitespace = true` on every edited file
  (including new files created in a change).
- Match surrounding Spring Framework style (formatter: Eclipse `eclipse-code-formatter.xml` when present). Tabs vs
  spaces: copy the file you are editing.
- New `.java` files: Apache 2.0 header (copy from a neighboring file), class Javadoc with `@author`. Substantial edits:
  add `@author`.
- Prefer existing utilities: `BeanUtils`, `PlacementService`, `MappingContext`, `PropertyPath`, Commons IO.
- Store mutators that change content are `@LockParticipant` so versioning/locking still applies.
- Do not add new public APIs in `internal.*`. Do not break binary compatibility of public store interfaces without
  deprecation.

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
- **Review:** Composer (`composer-2.5` subagent, not Fast) after code changes, or when asked. See
  `.cursor/rules/grok-dev-composer-review.mdc`, `agent-review.mdc`, and `agent-models.mdc`.

## Agent do / don’t

Do:

- Touch the smallest set of modules that implement the change
- Mirror patterns from a sibling backend when adding store features (FS and JPA are the usual templates)

Don’t:

- Rewrite Boot 2 `spring.factories` as the primary auto-config path
- Change `groupId` / public package names as a drive-by
- Skip `-P tests` when the change affects storage, REST mapping, or SQL dialects
