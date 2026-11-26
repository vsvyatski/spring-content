# Upgrading

## 4.1.0-SNAPSHOT

### Store entity arguments must not be null

`ContentStore`, `AssociativeStore`, and `ReactiveContentStore` entity parameters are annotated
`@NonNull` (JSpecify). Passing `null` for an entity to `setContent`, `getContent`, `unsetContent`,
`associate`, `unassociate`, or entity-scoped `getResource` overloads now fails with
`IllegalArgumentException` and the message `entity must not be null` instead of returning `null` or
completing quietly.

`getResource` by **content id** (`Store#getResource(SID)`) is unchanged: a `null` id may still be
handled by the backing store (for example returning `null`).

Use `org.springframework.content.commons.store.StoreEntityValidation#requireEntity` in custom store
implementations to match framework behavior.

Spring `Assert.notNull` is intended for **method arguments**. Validating uninitialized fields or
fragment wiring should use `IllegalStateException` (or `Objects.requireNonNull` when an NPE is
acceptable), not `Assert.notNull` on locals or instance fields.

`ContentProperty` methods that require configured type descriptors now throw `IllegalStateException`
when mapping was not initialized (previously `IllegalArgumentException` from `Assert.notNull` on
fields).

Entity null checks on store proxies follow `@NonNull` on the invoked method's first parameter (`Store#getResource(SID)`
is unchanged because `id` is not `@NonNull`).
