# Migrations and content versioning

Two independent version axes. Confusing them is the main risk this document exists to prevent.

| Axis | Versioned thing | Stored in | Changed by |
| --- | --- | --- | --- |
| **Schema version** | table and column shape | `IteraDatabase(version = N)` | a Room `Migration` |
| **Content version** | catalog and curriculum data | `contentVersion` in the JSON assets, mirrored in `user_prefs.content_version` | `ContentReconciler` |

A content change usually needs **no** schema migration. A schema change is never triggered by editing prose.

## 1. Schema migrations

- Version 1 is the first shipped schema (`01-room-schema.md`).
- `exportSchema = true`; `app/schemas/*.json` is committed and reviewed as part of any schema change.
- Every version bump ships a hand-written `Migration(n, n+1)` plus a `MigrationTest` using `MigrationTestHelper` that opens the previous schema, inserts representative rows, migrates, and asserts the data.
- `fallbackToDestructiveMigration()` is **forbidden** in release. Debug may use `fallbackToDestructiveMigrationOnDowngrade()`.
- Migrations are registered in a single `IteraMigrations.ALL` array so none can be forgotten.

### Patterns

| Change | Pattern |
| --- | --- |
| Add a nullable column | `ALTER TABLE x ADD COLUMN y TEXT` |
| Add a non-null column | add with a `DEFAULT`, then backfill in the same migration |
| Add a table or index | `CREATE TABLE` / `CREATE INDEX` |
| Rename or retype a column | create the new table, `INSERT ... SELECT`, drop the old, rename - all in one transaction |
| Change a `@SerialName` in a result payload | a migration that reads, rewrites and writes back each affected `resultPayload`, in batches of 500 |

## 2. Content reconciliation

Runs once per app start, before the first plan generation, in `ContentReconciler.reconcile()`:

```
catalog := load()
if (catalog.contentVersion == prefs.contentVersion) return

for each technique in catalog:
    if no technique_state row exists:
        insert (techniqueId, unlockedAt = null)
    if technique.introDay != null && technique.introDay <= prefs.currentProgramDay && row.unlockedAt == null:
        unlock it, recording unlockedOnProgramDay = prefs.currentProgramDay

prefs.contentVersion = catalog.contentVersion
```

Properties:

- **Idempotent** - running it twice changes nothing the second time.
- **Never re-locks** - a technique the user has met stays met, even if its `introDay` moved later.
- **Never regenerates past days** - `training_day.generatorVersion` guards that independently.
- **Never deletes history** - a retired technique keeps its rows; the mapper resolves its name from the (still present) string resources.

## 3. Allowed content changes within a major version

| Change | Allowed | Effect |
| --- | --- | --- |
| Edit any prose string | yes | immediate; history re-renders with the new text |
| Add a technique with a new `introDay` past the current maximum | yes | appears in the library as locked; enters the curriculum for everyone |
| Add a template block to an existing technique | yes | old payloads lack the key and render it empty |
| Change `estimatedMinutes`, `related`, `defaults` | yes | cosmetic |
| Mark a technique `retired` | yes | disappears from curriculum and library; history intact |
| Change a technique's `skill` | **no** | would retroactively rewrite skill progress |
| Change a technique's `id` | **no** | orphans every historical row |
| Rename a template block `key` | **no** | orphans draft and result payloads |
| Remove a technique entry outright | **no** | use `retired` |
| Reorder `introDay` for existing days | **discouraged** | in-flight users keep their generated days; new users get the new order. Allowed only in a major version, with a note in the release checklist |

The four "no" rows are asserted by `CatalogCompatibilityTest`, which compares the shipped catalog against a committed snapshot of the previously released one and fails on any forbidden change.

## 4. Downgrade

A user who sideloads an older APK over a newer database hits Room's downgrade path. Release builds throw; the app shows a blocking "This version is older than your data" screen with no destructive option. This is rare, is not worth destructive handling, and losing a user's journal to a downgrade would be unforgivable.

## 5. Checklist for any change

- [ ] Does this change table shape? -> schema migration + migration test + committed schema JSON.
- [ ] Does this change catalog data? -> bump `contentVersion` in **both** JSON files.
- [ ] Does it touch a frozen id, skill, block key or `@SerialName`? -> stop; this is a major version change.
- [ ] Does `CatalogValidationTest` still pass?
- [ ] Does `CatalogCompatibilityTest` still pass?
- [ ] Are new tables assigned to a reset tier (ADR-0014)?
