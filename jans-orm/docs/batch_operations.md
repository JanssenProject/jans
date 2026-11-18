## Atomic batch insert and multi-get by DN

`PersistenceEntryManager` has four methods for callers that write or read many entries at once:

```java
default boolean isAtomicBatchSupported();
default boolean isAtomicBatchSupported(String primaryKey);
default void persistBatch(List<?> entries);
default <T> Map<String, T> findByDns(Class<T> entryClass, Collection<String> dns);
```

### When to use

Use `persistBatch` for many inserts that must commit together, such as the receipt rows and record rows of one
Lock TRACE bulk request. It is not a general transaction API: it only inserts, it cannot update, delete or
read inside the transaction, and the batch is the only unit of atomicity. For inserts that can succeed or fail
one by one, use `persist`.

### SQL only

Only the SQL backend implements `persistBatch` for real. On LDAP, Couchbase and Spanner
`isAtomicBatchSupported()` returns `false` and `persistBatch` throws
`io.jans.orm.exception.UnsupportedOperationException`. Check the capability first and fall back to
`persist` per entry:

```java
if (entryManager.isAtomicBatchSupported()) {
    entryManager.persistBatch(entries);
} else {
    for (Object entry : entries) {
        entryManager.persist(entry);
    }
}
```

The fallback is not atomic. Use it only when the caller can handle a partial write.

### `persistBatch`

- Inserts all entries in one transaction: every entry is stored or none is. There are no partial results.
- Entries can be of different entity classes and go to different tables. The insert order is the list order.
- Each entry is mapped as `persist` maps it: DN from the `@DN` property, object classes, expiration and
  `@Version` initialization are the same. The entry objects are updated in place, as with `persist`.
- An empty list is a no-op on a backend that supports the batch. A `null` list or a `null` element is
  rejected with `MappingException`.
- A DN that already exists fails the whole batch with an `EntryPersistenceException` whose cause is a
  `DuplicateEntryException` (a checked exception, wrapped the same way `persist` wraps it). Its `getDn()`
  is the failing DN when the database reports it. Any other failure is an `EntryPersistenceException`
  without that cause. In both cases nothing is stored.
- The call is stateless and thread safe. On SQL the batch is one transaction on one pooled connection.
  There is no general `begin`/`commit` API: the batch is the unit of atomicity.

### SQL backend notes

- **One commit.** `persistBatch` takes one pooled connection, turns auto-commit off, runs all inserts and
  commits once. Any failure rolls the transaction back, then auto-commit is restored and the connection
  goes back to the pool. On PostgreSQL a batch of 100 entries is one write transaction (one WAL flush)
  instead of 100.
- **JDBC batching.** Consecutive entries with the same table and the same set of non-empty columns are
  sent as one JDBC batch (`PreparedStatement.addBatch`). Entries of other tables, or with other columns,
  start a new statement, so the insert order is always the list order. Values are converted by the same
  code as `persist` (JSON and multivalued columns, binary columns, dates).
- **Size limit.** A batch has at most `SqlOperationService.MAX_BATCH_INSERT_SIZE` (1000) entries; a larger
  batch is rejected with `InvalidArgumentException` before any database call. All mapped entries and
  their converted values stay in memory until the commit, and the transaction keeps its row locks until
  then, so keep batches small (Lock limits a bulk request to `maxBulkRecords` records, 100 by default).
- **Duplicates.** A unique key violation is SQLState `23505` on PostgreSQL and SQLState `23000` with
  error code `1062` on MySQL/MariaDB. It applies to an existing row and to the same DN twice in one
  batch. `DuplicateEntryException.getDn()` is the failing DN when the driver message names the
  duplicate `doc_id` (PostgreSQL `Key (doc_id)=(...) already exists`, MySQL
  `Duplicate entry '...' for key 'PRIMARY'`); otherwise it is `null` and the message names the batch index
  range of the failing statement. The method never retries. Two concurrent batches that share a DN
  serialize on that key: exactly one commits, the other gets the duplicate error and stores nothing.
- **`findByDns`** runs `SELECT ... WHERE doc_id IN (...)` with bound parameters, at most
  `SqlOperationService.LOOKUP_BY_KEYS_CHUNK_SIZE` (500) keys per statement, and converts rows the same way
  as `find`. All DNs are read as entries of the given entity class (one table per call): a DN of
  another entity class is reported as missing.
- `persist` (single entry, auto-commit) reports a unique key violation the same way: an
  `EntryPersistenceException` whose cause is a `DuplicateEntryException` with `getDn()` set to the DN of
  the entry. Other failures are unchanged. Before this change SQL `persist` had no duplicate cause.

### `findByDns`

- Returns the found entries keyed by the DNs as passed. Missing DNs are absent from the result.
- The result order follows the first occurrence of each DN in the input. A repeated DN is read once.
- Works on every backend. The default implementation reads one DN at a time (`contains` plus `find`), so
  no capability check is needed. The SQL backend reads the entries in one round trip per table.
- On SQL every read failure is an `EntryPersistenceException`. The default implementation relies on
  `contains`, which returns `false` when its read fails, so on other backends a failed read can look like
  a missing DN.

### Hybrid persistence

- `isAtomicBatchSupported()` is `true` only when every configured backend supports the batch.
  `isAtomicBatchSupported(dn)` reports the capability of the backend that stores `dn`.
- `persistBatch` requires all entries to map to the same backend. A batch across backends is rejected with
  `InvalidArgumentException`, because one transaction cannot span several databases. The batch is then
  delegated to that backend, which throws `UnsupportedOperationException` if it has no atomic batch.
- `findByDns` groups the DNs by backend and delegates each group.
