# Persistence package: design and rationale

This document explains how the `persistence` package works and, for every significant choice,
why it was made and which alternatives were rejected. It is written to support the lab
discussion ("every group member must understand and defend the design").

## 1. Responsibility and scope

The package answers one question for the rest of the application: *how are employees and
departments kept between runs?* Everything about files, CSV syntax, and loading/saving is here.
Nothing else in the project knows that CSV is used.

Requirements it satisfies (lab section 5.7 and 8):

| Requirement | Where it is met |
|---|---|
| Files are mandatory; memory alone is not enough | `CsvFileStore`, repositories save on every change |
| Load saved data when the application starts | `AbstractCsvRepository` constructor |
| Save on every add, update and delete | `AbstractCsvRepository.commit` |
| Departments are saved and loaded too | `CsvDepartmentRepository` + `departments.csv` |
| Type and salary data are rebuilt correctly after restart | `EmployeeCsvMapper` (type + `key=value` fields, rebuilt through the factory) |
| Save the underlying objects, not the table's displayed data | repositories hold real `Employee` objects |
| Missing file on first launch starts empty | `CsvFileStore.readAll` |
| File errors must not terminate the application | everything surfaces as `PersistenceException` (an `EmsException`) |
| Invalid input must not corrupt stored data | write-first-then-swap, atomic file replace, strict loading |

## 2. Structure

```
service layer
    |  depends only on interfaces
    v
Repository<T,ID>  <-- EmployeeRepository, DepartmentRepository      (contracts)
    ^
    |
AbstractCsvRepository<T>  <-- CsvEmployeeRepository, CsvDepartmentRepository   (behaviour)
    |            |
    |            +--> CsvRecordMapper<T> <-- EmployeeCsvMapper, DepartmentCsvMapper (columns)
    v
CsvFileStore                                                          (file + CSV syntax)
```

| Class | Single job |
|---|---|
| `Repository<T,ID>`, `EmployeeRepository`, `DepartmentRepository` | Define what storage offers and promises |
| `AbstractCsvRepository<T>` | Keep entities in memory, save every change, load at start |
| `CsvEmployeeRepository`, `CsvDepartmentRepository` | The one extra query each entity needs |
| `CsvRecordMapper<T>` + two implementations | Convert one entity to/from one CSV record |
| `CsvFileStore` | Read and write a CSV file safely |
| `PersistenceException` | Report that storing or loading failed |

## 3. File formats

`data/departments.csv`
```
id,name
1,IT
2,Human Resources
```

`data/employees.csv`
```
type,id,fullName,age,departmentId,position,typeFields
FULL_TIME,1,Ali Hassan,30,2,Engineer,basicSalary=15000.0,bonus=2000.0
PART_TIME,2,Mona Said,24,1,Tutor,hourlyRate=100.0,hoursWorked=80.5
```

Values that contain a comma, a quote or a line break are quoted: `"Smith, John"`,
`"Ali ""Boss"" Said"`. Encoding is UTF-8, so Arabic names are preserved.

## 4. Design decisions

### D1. Repository pattern behind interfaces

*Decision.* The service layer talks to `EmployeeRepository` / `DepartmentRepository`, never to a
CSV class.

*Why.* Dependency inversion: business code does not change if storage changes (a database, JSON,
or an in-memory fake for tests). It also makes the services testable without touching disk.

*Rejected.* Letting services read and write files directly: that mixes business rules with file
handling, and every service would duplicate the file code.

### D2. Three layers inside the package: store, mapper, repository

*Decision.* `CsvFileStore` knows files and CSV syntax but no entities. A mapper knows one
entity's columns but no files. The repository knows when to save but not how.

*Why.* Single responsibility: each class has exactly one reason to change. A new column changes
only a mapper. A different CSV dialect changes only the store. A different save policy changes
only the repository.

*Rejected.* One `FileHandler` class that does everything (the first version of the diagram).
It would change for three unrelated reasons, and it would need a branch for every entity type.

### D3. One generic base class, `AbstractCsvRepository<T>`

*Decision.* Load, `findAll`, `findById`, `existsById`, `save`, `update`, `deleteById` and the
saving logic are written once. The two concrete repositories add only `findByName` and
`findByDepartment`.

*Why.* Without it, the two repositories would contain the same 100 lines. A fix (for example in
error handling) would have to be repeated and could be forgotten in one of them. It also makes
the open/closed principle concrete: a new entity needs a mapper and a 10-line subclass.

*Detail: the ID is given by a function argument (`ToIntFunction<T>`), not an abstract method.*
The constructor loads the file and needs the ID of every entity it builds. Calling an abstract
method from a constructor runs subclass code before the subclass is initialised, which is a known
source of subtle bugs. Passing `Employee::getId` avoids it with no extra code.

### D4. Keep everything in memory, read the file once

*Decision.* The file is read in the constructor; every query is answered from memory.

*Why.* The data set is small (a lab-sized employee list), and the GUI refreshes tables often.
Reading the file on each query would add I/O for no benefit.

*Rejected.* Reading the file on every call: slower, and it opens a window where another change
could be missed. Memory is the single source of truth while the program runs, and the file is its
durable copy.

### D5. Data structure: `LinkedHashMap<Integer, T>`

*Why.* Constant-time lookup by ID; a duplicate ID is impossible by construction; insertion order
is preserved, so the file lists records in creation order; replacing a record keeps its position.

*Rejected.* `ArrayList` with a linear search: O(n) per lookup and the uniqueness rule would have
to be enforced separately. `HashMap`: no stable order, so file output would be unordered.

### D6. Write first, then change memory ("persist-then-swap")

*Decision.* `commit(next)` builds the new state in a copy, writes it to the file, and only then
replaces the in-memory map.

*Why.* If the write fails (disk full, file locked), memory was never modified, so memory and file
always agree and the user sees an error with the data unchanged. This directly meets "do not
allow invalid input to corrupt the stored data" and "file errors must not terminate the
application".

*Rejected.* "Change memory, write, and undo on failure". It needs separate undo code for add,
update and delete, and undoing a delete must also restore the original position in the list.
More code, more bugs, same result. The copy costs O(n), the same as rewriting the file, which
the design does anyway.

### D7. Rewrite the whole file on every change

*Why.* CSV has no way to update one line in place when line lengths change. Rewriting is simple
and correct, and for a lab-sized data set it takes microseconds.

*Trade-off (accepted).* It does not scale to very large data sets. That would call for a
database, which is outside this lab.

### D8. Atomic file replacement (temporary file, then move)

*Decision.* `writeAll` writes `file.csv.tmp` completely, then moves it over `file.csv` in one
step (`ATOMIC_MOVE`, falling back to `REPLACE_EXISTING` on file systems without it). The
temporary file is deleted in a `finally` block.

*Why.* Writing straight to `employees.csv` truncates it first. If the program is closed or
crashes during the write, the file is left half-written or empty, and every saved employee is
lost. With the temporary file, the old file stays valid until the new one is fully written.

*Limit (accepted).* This protects against program crashes and write errors. It does not call
`fsync`, so a power loss at the exact moment of the move could still lose the latest change.

### D9. A custom CSV reader and writer, not `split(",")` and not a library

*Why not `split(",")`.* It breaks on commas inside quotes (`"Smith, John"`) and drops trailing
empty fields. Names and positions are free text, so this would corrupt data in normal use.

*Why not a library (OpenCSV, Commons CSV).* The project is a plain `src` folder with no build
tool, so adding a jar means setup steps for every group member and for the grader. The format
needed is small: quoting, doubled quotes, line breaks inside quotes, CRLF. About 60 lines.

*What the parser handles.* Quoted commas and quotes; line breaks inside quotes; Windows (CRLF)
and Unix line endings; a leading byte-order mark that Notepad may add; blank lines; a last line
with no line break. It is strict about malformed input: an unterminated quote, a stray quote, or
text after a closing quote produces an error with the line number instead of being guessed at.

*Why strict.* Guessing could load wrong data silently. An error is visible and fixable.

### D10. Why not Java serialization

The lab allows it. CSV was chosen because the file is human-readable and editable (useful for
the demonstration and for debugging), it survives class changes (a serialized file becomes
unreadable when a class gains a field), and it is not tied to Java. The cost, writing mappers,
is small and is what D11 turns into an extension point.

### D11. Employee type data stored as `key=value` pairs

*Decision.* After six fixed columns, the salary data of the type is stored as `key=value`
pairs taken from `Employee.getTypeFields()`.

*Why.* `EmployeeCsvMapper` never names `FullTimeEmployee` or `PartTimeEmployee`. A third employee
type (for example a contractor) needs no change in the mapper or the repository. A fixed layout
with columns for every type's fields would need new columns, and would leave empty cells for
types that do not use them.

*Rejected.* A different column set per type (separate files or a wide row): rigid, and each new
type changes the file format.

### D12. Rebuilding employees through an injected builder (`BiFunction`)

*Decision.* The mapper receives `BiFunction<EmployeeData, Department, Employee>`, in practice
`factory::create`, and rebuilds each employee by calling it.

*Why.*
1. The right subclass is restored from the stored type, and the same creation and validation
   rules apply to data loaded from a file as to data typed in the GUI.
2. The mapper needs one capability only (build an employee), not the whole factory (interface
   segregation), so `persistence` has no dependency on the `factory` package. That also avoids a
   cycle: the factory package does not need to know about storage, and storage does not need to
   know how creators are organised.
3. At the time of writing, the `factory` package did not exist yet, so this design allows the
   persistence package to be finished, compiled and tested independently.

*Rejected.* `new FullTimeEmployee(...)` inside the mapper: it hard-codes the types, and the
mapper would change every time an employee type is added (violates open/closed).

### D13. Store the type with `name()`, never `toString()`

`EmployeeType.toString()` returns the display label ("Full-Time"), and `valueOf("Full-Time")`
fails. The stored value is `FULL_TIME`. The label can change for the GUI without breaking files
that were already saved.

### D14. Store the department by ID, not by name

*Why.* The employee file then does not have to change when a department is renamed, and there is
one place where a name lives. On load, the ID is resolved to the shared `Department` object, so
all employees of a department point at the same instance.

*Consequence.* Departments must be loaded first (the employee mapper looks them up), and an
employee must not refer to a department that does not exist (see D16).

### D15. Sorted `key=value` pairs, locale-independent numbers

`Map.of` iterates in a different order on each JVM run. Sorting the pairs gives identical file
content for identical data, so the file does not change when nothing changed, and Git diffs show
only real edits. Numbers are written with `String.valueOf(double)` (by the model), which does not
depend on the computer's locale and reads back exactly. `String.format("%.2f")` could write a
decimal comma on some systems and break reloading.

### D16. A damaged file stops loading with a clear error

*Decision.* Any bad record (wrong column count, unknown type, unknown department, invalid value,
duplicate ID) throws a `PersistenceException` naming the file and the record number. The
application does not start with partial data.

*Why.* If bad lines were skipped, the next save would rewrite the file from memory and
permanently delete those lines. Stopping lets the user repair the file or restore a copy.

*Rule for the other layers.* Because of this, the service must refuse to delete a department that
still has employees; otherwise the next start fails with "refers to department ID n, which does
not exist".

### D17. Validation on load reuses the model's rules

Mappers rebuild objects through public constructors/factory, so the model's checks (`Guard`)
apply to file data. A hand-edited file with a negative salary or blank name is rejected, not
trusted.

### D18. Two kinds of failure, two exception types

| Situation | Exception | Reason |
|---|---|---|
| File cannot be read/written, file is damaged | `PersistenceException` | Environment problem; the GUI should show a message. It extends `EmsException`, which the GUI already catches. |
| Saving an ID that exists; updating or deleting one that does not | `IllegalArgumentException` | The caller broke the contract. The service must check `existsById` first and give the user a friendly message, so this signals a bug. |

*Why `PersistenceException` is in this package.* It is raised only here, and this work was limited
to the persistence package. Moving it to `exception` later only needs a changed `package` line and
imports. `EmsException` has no constructor taking a cause, so the cause is attached with
`initCause`.

### D19. Case-insensitive department search with `equalsIgnoreCase`

Lab 4.3 and 5.6 require it. `equalsIgnoreCase` does not depend on the default locale (a
`toLowerCase()` comparison gives wrong results for "I" under a Turkish locale). Names are trimmed
before comparing.

### D20. `findByDepartment(Department)` compares IDs

The interface (already in the project) takes a `Department`. Comparing IDs is stable when a
department is renamed and cheaper than comparing names. Name matching belongs to
`DepartmentRepository.findByName`.

### D21. Defensive copies and unmodifiable results

`findAll()` returns an immutable snapshot. A caller cannot change the repository's contents (or
break a loop that is iterating) by modifying the returned list. Inside the class, state is
replaced as a whole and never modified in place, which is what makes D6 safe.

### D22. No thread safety, on purpose

A Swing application changes data from the event dispatch thread. Locking would add complexity and
cost with nothing to protect. If a background thread is ever added, access must be synchronised
at that point.

## 5. How the SOLID principles appear here

| Principle | Where |
|---|---|
| Single responsibility | Store (file), mapper (columns), repository (when to save), exception (error type) |
| Open/closed | New entity: add a mapper and a small subclass. New employee type: nothing changes here (D11, D12) |
| Liskov substitution | Any `Repository<T,ID>` implementation (CSV, in-memory, database) can replace another with the same contract (documented on the interface) |
| Interface segregation | `EmployeeCsvMapper` asks for a `BiFunction` and `DepartmentRepository`, not for a factory or for every repository method |
| Dependency inversion | Services depend on `Repository` interfaces; the mapper depends on an abstraction for building employees |

## 6. Differences from the earlier UML diagrams

Update the class and package diagrams so they match the code:

1. **Added** `AbstractCsvRepository<T>` (base class of both CSV repositories, which now `extend` it
   and `implement` their interface).
2. **Added** `PersistenceException` in package `persistence` (diagram had it in `exception`).
3. `CsvFileStore`: constructor `(filePath : String, header : String...)`; methods `readAll()`,
   `writeAll(records)`, `getFilePath()`; private helpers `moveIntoPlace`, `toLine`, `escape`,
   `describe`; package-private `parse(text)`.
4. `EmployeeCsvMapper` depends on `BiFunction<EmployeeData, Department, Employee>` and
   `DepartmentRepository`, **not** on `EmployeeFactory`. Remove the `persistence -> factory`
   dependency from the package diagram.
5. `EmployeeRepository.findByDepartment(department : Department)` (not a `String`).
6. Both mappers expose `static header() : String[]`.
7. `CsvEmployeeRepository` / `CsvDepartmentRepository` no longer list their own `persist()`
   method; saving is `commit(next)` in the base class.

## 7. Notes for the code outside this package (not changed)

These are consequences of the design that the other layers need to respect. Nothing outside
`persistence` was modified.

1. **Wiring order in `AppConfig`.** Departments first, because employees refer to them:
   ```java
   CsvFileStore deptStore = new CsvFileStore("data/departments.csv", DepartmentCsvMapper.header());
   CsvFileStore empStore  = new CsvFileStore("data/employees.csv",   EmployeeCsvMapper.header());

   DepartmentRepository deptRepo = new CsvDepartmentRepository(deptStore, new DepartmentCsvMapper());
   EmployeeRepository   empRepo  = new CsvEmployeeRepository(empStore,
           new EmployeeCsvMapper(employeeFactory::create, deptRepo));
   ```
   Both constructors can throw `PersistenceException`; catch it in `MainApp`/`AppConfig`, show the
   message, and exit, rather than continuing without data.
2. **`EmployeeFactory.create`** must have the signature `Employee create(EmployeeData, Department)`
   so that `employeeFactory::create` fits the builder.
3. **Searching by department.** The repository takes a `Department`. The service should do
   `Department d = departmentRepo.findByName(name); if (d == null) -> empty/NotFound; else
   employeeRepo.findByDepartment(d)`. (The `EmployeeServiceImpl` sketched earlier passed a
   `String`; it needs this small change.)
4. **Deleting a department** must be refused while employees still use it (D16).
5. **Renaming a department.** `Department.setName` is currently private, so an update would have
   to replace the object. Employees already in memory would then still point at the old object
   (old name) until restart, although the files are correct. The simplest fix is a public
   `setName` (it validates with `Guard`): the service changes the stored instance and calls
   `update(sameInstance)`.
6. **Duplicate department names (ignoring case)** are a business rule and are checked in
   `DepartmentServiceImpl`; the repository guarantees unique IDs only.
7. **`exception/EmsException.java`** has three unused imports from `model`
   (`EmployeeData`, `EmployeeType`, `FieldKeys`). They compile, but they make the `exception`
   package depend on `model`, which also depends on `exception`. Remove them.
8. **`data/` folder.** Paths are relative to the working directory the program starts in (the
   project folder when run from IntelliJ). Decide whether `data/` should be in `.gitignore`.
   State the location and format in the README, as the lab requires.

## 8. Known limitations (accepted)

* The whole file is rewritten on each change (fine for a lab-sized data set).
* No `fsync`: a power loss at the moment of a save could lose that last change.
* Two running copies of the application would overwrite each other's changes; there is no file
  locking.
* `findById` returns the stored object. Changing it through setters changes memory only; the file
  is updated when the object is passed to `update`. For full atomicity, build a new object and
  pass that.
* Fields starting with `=`, `+`, `-` or `@` are not escaped against spreadsheet formula injection;
  that matters only if files from untrusted sources are opened in Excel.

## 9. How it was verified

A separate test program (54 checks, not part of the project) compiled against the real `model`
and `exception` classes and ran these scenarios:

* first launch with no files, folder creation, header and row content;
* restart: both employee subclasses restored with correct salaries, shared department objects,
  order preserved, repeated saves producing identical files;
* queries: case-insensitive department name, by department, unknown IDs, null input;
* contract violations (duplicate save, update/delete of unknown IDs), unmodifiable results;
* update keeps position; update and delete are saved;
* round trips of commas, quotes, Arabic text, line breaks inside a name;
* files edited by other tools: BOM, CRLF, blank lines, no header, no final line break;
* damaged files: unterminated quote, stray quote, wrong column count, non-numeric ID/age,
  negative values, blank name, duplicate ID, invalid UTF-8, unknown type/department, missing
  salary field, malformed `key=value`, with the file and record number in the message;
* write failures: memory unchanged, no leftover `.tmp` file, previous file intact.

## 10. Questions likely to come up in the discussion

**Why is saving "write first, then update memory"?** So a failed write cannot leave memory and file
different, and no undo logic is needed (D6).

**Why can a repository not be created with a bad file?** Skipping bad lines would delete them at
the next save (D16).

**How does a new employee type reach the file with no change here?** Its `getTypeFields()` map is
written as `key=value` pairs and rebuilt through the factory (D11, D12).

**Why is there an abstract base class when you prefer interfaces?** The interfaces are the
contract the services use. The base class is only reuse of implementation inside this package;
nobody outside depends on it (D3).

**Why are the department IDs stored in the employee file instead of names?** One place for the name,
no rewrite on rename (D14).

**What happens if the program is closed during a save?** The old file is still complete, because
the new one is written separately and swapped in (D8).
