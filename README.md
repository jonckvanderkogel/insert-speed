### Running simulations

```
docker compose up -d
```

To run the simulation where we use a sequence for primary key generation:
```
mvn clean spring-boot:run -Dspring-boot.run.arguments="--experiment.simulation-type=sequence"
```

To run the simulation where we use a historized table structure:
```
mvn clean spring-boot:run -Dspring-boot.run.arguments="--experiment.simulation-type=historic"
```

To run the simulation where we use historized table structure with a many-to-many relationship:
```
mvn clean spring-boot:run -Dspring-boot.run.arguments="--experiment.simulation-type=historic_mtm"
```

Generate PlantUML diagrams
```
mvn plantuml:generate
```

### Re-runs
# 📅 Historical Data Strategy

To simulate the historical strategy, we have entities called **Foo** and **Bar**.

* **Business Keys** (`bk1`, `bk2`) – uniquely identify an entity and **never** change.
* **Content** (`c1`, `c2`) – regular attributes that **can** change over time.

Whenever content changes we insert a new row and mark its valid period.  
We also record how Foo and Bar relate to each other in a **join table**.  
This strategy needs extra care whenever we rerun ingestion for **past** dates.

---

## ✅ Initial Data Situation

### Foo Historic Table

| business keys (`bk1`,`bk2`) | content (`c1`,`c2`) | valid_from | valid_until |
|-----------------------------|---------------------|------------|-------------|
| (a, b)                      | (k, l)              | 2025-01-01 | `null`      |

### Bar Historic Table

| business keys (`bk1`,`bk2`) | content (`c1`,`c2`) | valid_from | valid_until |
|-----------------------------|---------------------|------------|-------------|
| (c, d)                      | (q, r)              | 2025-01-01 | `null`      |

### Foo-Bar Join Table

| Foo (`bk1`,`bk2`) | Foo valid_from | Bar (`bk1`,`bk2`) | Bar valid_from |
|-------------------|---------------|-------------------|----------------|
| (a, b)            | 2025-01-01    | (c, d)            | 2025-01-01     |

---

## ✅ Next ingestion (on 2025-02-01)

During the Feb 1 daily load we detected:

* **Foo** content change → **(m, n)**
* **Bar** unchanged → still **(q, r)**

### Foo Historic Table (after Feb 1)

| business keys | content | valid_from | valid_until |
|---------------|---------|------------|-------------|
| (a, b)        | (k, l)  | 2025-01-01 | 2025-02-01  |
| (a, b)        | (m, n)  | 2025-02-01 | `null`      |

#### Actions
* First row needs to be "closed" by setting the valid_until to the reporting date
* Second row needs to be inserted with the new content, valid_from set to the reporting date and valid_until is null.

### Bar Historic Table (unchanged)

| business keys | content | valid_from | valid_until |
|---------------|---------|------------|-------------|
| (c, d)        | (q, r)  | 2025-01-01 | `null`      |

### Join Table (Foo changed, Bar did not)

| Foo | Foo valid_from | Bar | Bar valid_from |
|-----|---------------|-----|----------------|
| (a, b) | 2025-01-01 | (c, d) | 2025-01-01 |
| (a, b) | 2025-02-01 | (c, d) | 2025-01-01 |

#### Actions
* New join row inserted where Foo valid_from was updated to the reporting date. Bar valid_from stays the same since
Bar did not change.

At this point Bar only has one record but is connected to 2 instances of Foo. The join queries become quite tricky to
get the correct instance of Foo joined to Bar, for example if you want to get the correct entity graph for 2025-01-17.

---

## ✅ First *Historical* Rerun (on 2025-01-15)

A rerun on **Jan 15**:

* **Foo** content changed to **(o, p)** (from (k, l)).
* **Bar** content changed to **(s, t)** (from (q, r)).

Applying the rerun produced:

### Foo Historic Table (after Jan 15 rerun)

| business keys | content | valid_from | valid_until |
|---------------|---------|------------|-------------|
| (a, b)        | (k, l)  | 2025-01-01 | 2025-01-15  |
| (a, b)        | (o, p)  | 2025-01-15 | 2025-02-01  |
| (a, b)        | (m, n)  | 2025-02-01 | `null`      |

#### Actions:
* Update the first record and change the valid_until from 2025-02-01 to 2025-01-15.
* A new line needed to be inserted in between with content (o, p) and valid_from 2025-01-15 and valid_until 2025-02-01.

### Bar Historic Table (after Jan 15 rerun)

| business keys | content | valid_from | valid_until |
|---------------|---------|------------|-------------|
| (c, d)        | (q, r)  | 2025-01-01 | 2025-01-15  |
| (c, d)        | (s, t)  | 2025-01-15 | `null`      |

#### Actions
* First row needs to be closed and valid_until needs to be set to the reporting date.
* New row needs to be inserted with the valid_from set to the reporting date, valid_until is set to null.

### Join Table (both changed)

| Foo | Foo valid_from | Bar | Bar valid_from |
|-----|---------------|-----|----------------|
| (a, b) | 2025-01-01 | (c, d) | 2025-01-01 |
| (a, b) | 2025-01-15 | (c, d) | 2025-01-15 |
| (a, b) | 2025-02-01 | (c, d) | 2025-01-15 |

#### Actions
* Second row needs Bar valid_from updated to 2025-01-15
* A new join needs to be put in place for Foo valid_from 2025-01-15 and Bar valid_from is also 2025-01-15.

---

## ✅ Second Rerun (on 2025-01-15) — *Foo Content Reverted*

During a second rerun on 2025-01-15, **Foo actually hadn’t changed**. `(k, l)` was still correct. Bar did have its'
contents changed to (s,t).

Careful cleanup is now required:

1. **Delete the incorrect join**  
   *Remove link* (a, b @ 2025-01-15) → (c, d @ 2025-01-15)
2. **Delete the incorrect Foo row**  
   *Remove* `(o, p)` (valid_from = 2025-01-15)
3. **Restore Foo’s original interval**  
   Extend `(k, l)` to remain valid **until 2025-02-01**.
4. **Insert the correct join**  
   Link **Foo (a, b @ 2025-01-01)** with **Bar (c, d @ 2025-01-15)**.

### Final Corrected State

#### Foo Historic Table

| business keys | content | valid_from | valid_until |
|---------------|---------|------------|-------------|
| (a, b)        | (k, l)  | 2025-01-01 | 2025-02-01  |
| (a, b)        | (m, n)  | 2025-02-01 | `null`      |

#### Bar Historic Table

| business keys | content | valid_from | valid_until |
|---------------|---------|------------|-------------|
| (c, d)        | (q, r)  | 2025-01-01 | 2025-01-15  |
| (c, d)        | (s, t)  | 2025-01-15 | `null`      |

#### Join Table

| Foo | Foo valid_from | Bar | Bar valid_from |
|-----|---------------|-----|----------------|
| (a, b) | 2025-01-01 | (c, d) | 2025-01-01 |
| (a, b) | 2025-01-01 | (c, d) | 2025-01-15 |
| (a, b) | 2025-02-01 | (c, d) | 2025-01-15 |

---
