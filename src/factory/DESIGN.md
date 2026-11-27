# Factory package: design and documentation

Package `factory` turns raw employee data (text from the GUI form or from `employees.csv`) into a valid `Employee` object of the correct subclass. It contains no file access, no GUI code and no business rules about which employees may exist. Those belong to `persistence`, `gui` and `service`.

---

## 1. Contents

| File | Kind | Responsibility |
|---|---|---|
| `EmployeeFactory` | interface | The contract other layers depend on: `Employee create(EmployeeData, Department)` |
| `RegistryEmployeeFactory` | class | Implements the contract. Parses the shared fields, then delegates to the creator registered for the employee type |
| `EmployeeCreator` | interface | Builds one kind of employee from the shared fields and the type-specific text values |
| `FullTimeEmployeeCreator` | class | Reads `basicSalary` and `bonus`, builds `FullTimeEmployee` |
| `PartTimeEmployeeCreator` | class | Reads `hourlyRate` and `hoursWorked`, builds `PartTimeEmployee` |
| `CommonFields` | record | The shared fields (id, name, age, department, position) after parsing |

Dependencies: `factory` uses `model` (`Employee` and subclasses, `EmployeeData`, `EmployeeType`, `FieldKeys`, `Department`) and `exception` (`Guard`, `EmsException`). It does not use `persistence`, `service`, `validation` or `gui`.

```
service.EmployeeServiceImpl ──┐
                              ├──► factory.EmployeeFactory  (interface)
persistence.EmployeeCsvMapper ┘            ▲
 (through the method reference             │ implements
  employeeFactory::create)        RegistryEmployeeFactory
                                           │ looks up by EmployeeType
                                           ▼
                                   EmployeeCreator  (interface)
                                     ▲            ▲
                     FullTimeEmployeeCreator   PartTimeEmployeeCreator
                                     │            │
                                     ▼            ▼
                            FullTimeEmployee   PartTimeEmployee   (model)
```

---

## 2. How `create` works

`RegistryEmployeeFactory.create(data, department)` runs these steps in order:

1. `data`, `department` and `data.type()` must not be null.
2. `data.id()` is parsed as a positive integer. Failure: `Employee ID ...`.
3. `data.age()` is parsed as a positive integer. Failure: `Age ...`.
4. A `CommonFields` record is built from the parsed id and age, the unchanged name and position, and the department.
5. The creator registered for `data.type()` is called with the `CommonFields` and `data.typeFields()`.
6. The creator parses its own values with `Guard.parseNonNegativeDouble` and calls the model constructor.
7. The model constructor validates name, position and department, trims the text, and returns the object.

The first invalid value stops the process with an `EmsException` (in practice a `ValidationException`) whose message names the field.

Example: `create(new EmployeeData("1","Ali Hassan","30","HR","Engineer",FULL_TIME, {basicSalary=15000, bonus=2000}), hrDepartment)` returns a `FullTimeEmployee` with `calculateSalary() == 17000.0`.

---

## 3. Class by class

### `EmployeeFactory` (interface)
One method. Callers (the service, and the persistence mapper through `employeeFactory::create`) depend on this type and not on the implementation. Its Javadoc states the two rules callers rely on: the `department` argument is authoritative, and any invalid value raises an `EmsException`.

### `RegistryEmployeeFactory`
- Field `creators`: an `EnumMap<EmployeeType, EmployeeCreator>`. An `EnumMap` is used because the key is an enum: it is fast, ordered by constant, and cannot hold a key that is not an `EmployeeType`.
- Constructor: takes a list of creators, registers each by `supportedType()`, then checks the registry (decision D6).
- `create`: described in section 2. The lookup `creators.get(type)` never returns `null`, because the constructor guarantees every type has a creator.
- It never names a concrete employee class.

### `EmployeeCreator` (interface)
`supportedType()` says which type the creator builds. `create(CommonFields, Map<String,String>)` builds the object. It is the only extension point of the package.

### `FullTimeEmployeeCreator` and `PartTimeEmployeeCreator`
Each reads exactly its own keys from `FieldKeys`, parses them as non-negative numbers, and calls the matching constructor. Unrelated keys in the map are ignored (D7). Each class is `final` and has no fields, so it is stateless.

### `CommonFields` (record)
Carries the six shared values so that creator signatures stay short and every creator receives the same pre-parsed data. It adds no validation: validation of name and position stays in `Employee`.

---

## 4. Design decisions

### D1. `EmployeeFactory` is an interface with one implementation
- **Chosen:** interface plus `RegistryEmployeeFactory`.
- **Reason:** the service layer and persistence layer then depend on an abstraction (Dependency Inversion). A test can substitute a different factory without touching them.
- **Rejected:** a single concrete class. It works, but every caller would be tied to the implementation.

### D2. One creator class per employee type, found through a registry
- **Chosen:** `EmployeeCreator` implementations stored in a map keyed by `EmployeeType`.
- **Reason:** a new type is added by writing a new creator. `RegistryEmployeeFactory` does not change (Open/Closed). Each creator has one reason to change: the fields of its own type (Single Responsibility).
- **Rejected:** `switch (data.type())` inside one factory method. That method would change for every new type and would mix the knowledge of all types in one place.

### D3. Shared fields are parsed once, in the factory
- **Chosen:** the factory parses id and age and passes a `CommonFields` record to the creators.
- **Reason:** parsing `id` and `age` is identical for every type. Putting it in each creator would repeat it, and a fix would have to be made twice.
- **Rejected:** creators that receive `EmployeeData` and parse everything. That duplicates code. Also rejected: creators with a six-argument `create(int, String, int, Department, String, Map)` method. A record is clearer and does not break when a field is added.

### D4. The factory validates even though a `validation` package exists
- **Chosen:** the factory parses and validates all values it uses.
- **Reason:** there are two ways into the factory. From the GUI, the service runs the validators first. From a file, `EmployeeCsvMapper` calls the factory directly and no validator runs. Data in a file can be edited by hand, so it must be checked too. The factory is the one place both paths share.
- **Result:** three layers use the same `Guard` rules: validators (early check of form values), factory (authoritative parsing), model constructors and setters (last line of defense for objects).
- **Rejected:** trusting the caller to validate first. A damaged `employees.csv` would then create invalid objects, or fail with a less clear error.

### D5. The `department` argument wins over `data.departmentName()`
- **Chosen:** the factory ignores `data.departmentName()`.
- **Reason:** the caller has already turned the name into a real `Department` (the service uses `findByName`, the mapper uses the stored department ID). Reading the name again could contradict that object. Looking up departments is also not the factory's job, because the factory would then need a repository.
- **Rejected:** having the factory take a `DepartmentRepository` and look the department up itself. It would couple `factory` to `persistence`, and the mapper already holds the resolved department.

### D6. The registry is checked when it is built
- **Chosen:** the constructor throws `IllegalArgumentException` if two creators support the same type or if any `EmployeeType` has no creator.
- **Reason:** both are wiring mistakes. The application should stop at startup with a clear message, and not later while a user saves an employee.
- **Consequence:** adding an enum constant without a creator stops the application at startup. This is intended.
- **Rejected:** checking at call time and throwing an `EmsException`. A configuration error would look like a data error.

### D7. Extra keys in `typeFields` are ignored
- **Chosen:** a creator reads only the keys it needs.
- **Reason:** the GUI form or an older file may carry additional keys. Missing keys are always detected, because the creator requires its own keys.
- **Trade-off:** a misspelled extra key is silently ignored. The required keys still have to be present, so wrong data cannot produce a wrong employee.
- **Rejected:** rejecting unknown keys. It is stricter but makes the factory fail on harmless input.

### D8. Name and position are passed on unchanged
- **Chosen:** `Employee` trims and validates them.
- **Reason:** the rule exists once, in the model. A second copy in the factory could drift.

### D9. No exception wrapping
- **Chosen:** `ValidationException` (an `EmsException`) propagates as it is.
- **Reason:** its message already names the field, for example `Basic salary must be a non-negative number`. The caller adds context where it has some: the mapper adds the file name and record number, and the GUI shows the message.

### D10. Stateless, no static access, no singleton
- **Chosen:** the factory is an ordinary object created once in the composition root (`AppConfig`) and passed to the classes that need it.
- **Reason:** it holds only an `EnumMap` filled in the constructor and never changed afterwards. Creators have no fields. The objects can be shared safely by all threads, and tests can create their own.
- **Rejected:** a static `EmployeeFactory.create(...)` or an enum singleton. Both hide the dependency and cannot be replaced in a test.

---

## 5. SOLID

| Principle | Where it applies |
|---|---|
| Single responsibility | The factory parses shared fields and dispatches. Each creator knows one type. Validation of names lives in the model. |
| Open/closed | A new employee type adds a creator. `RegistryEmployeeFactory`, the mapper and the service stay unchanged. |
| Liskov substitution | Any `EmployeeCreator` or `EmployeeFactory` implementation can replace another. A test replaces a creator with a stub and the factory uses it. |
| Interface segregation | Both interfaces have one or two methods, and callers use all of them. |
| Dependency inversion | The service and the mapper depend on `EmployeeFactory` (the mapper only on a `BiFunction`), never on `RegistryEmployeeFactory` or a concrete employee class. |

Patterns: this is a **registry-based factory**. The registry maps a type to an `EmployeeCreator`, and each creator is a small strategy for building one type. It is not the GoF Abstract Factory, which builds families of related objects.

---

## 6. Wiring (in `AppConfig`)

```java
EmployeeFactory employeeFactory = new RegistryEmployeeFactory(
        List.of(new FullTimeEmployeeCreator(), new PartTimeEmployeeCreator()));

// persistence: the mapper only needs a function, not the factory class
EmployeeCsvMapper employeeMapper = new EmployeeCsvMapper(employeeFactory::create, departmentRepository);

// service
EmployeeService employeeService = new EmployeeServiceImpl(
        employeeRepository, departmentService, employeeFactory,
        List.of(new CommonFieldsValidator(), new FullTimeSalaryValidator(), new PartTimeSalaryValidator()));
```

Order: department repository, then the factory, then the employee mapper and repository, then the services.

---

## 7. Adding a new employee type (example: `Intern`)

1. `model`: add `INTERN("Intern")` to `EmployeeType`, new keys to `FieldKeys`, and an `InternEmployee` class.
2. `validation`: add an `InternSalaryValidator` (`supports` returns true for `INTERN`).
3. `factory`: add `InternEmployeeCreator implements EmployeeCreator`.
4. `AppConfig`: add `new InternEmployeeCreator()` and the new validator to the two lists.

Unchanged: `RegistryEmployeeFactory`, `EmployeeFactory`, `EmployeeCsvMapper` (type fields travel as `key=value` pairs), all repositories, and `EmployeeServiceImpl`. If step 3 or the registration in step 4 is forgotten, the application stops at startup (D6).

---

## 8. Error reference

| Input | Result |
|---|---|
| id null, blank, not a whole number, zero, negative | `Employee ID ...` |
| age null, blank, not a whole number, zero, negative | `Age ...` |
| name or position null or blank | `Full name is required` / `Position is required` (from `Employee`) |
| `data`, `department` or `type` null | `Employee data/Department/Employee type is required` |
| required type-specific key missing | `<field> is required` |
| type-specific value not a number, negative, `NaN` or infinite | `<field> must be a number` or `... non-negative number` |
| two creators for one type, or a type without a creator | `IllegalArgumentException` when the factory is built |

When the error happens while loading a file, `EmployeeCsvMapper` and `AbstractCsvRepository` wrap it, for example: `'data/employees.csv', record 3: invalid employee: Basic salary must be a non-negative number`.

---

## 9. Verification

The whole project (all packages, including the persistence and service classes) compiles with `javac -Xlint:all`. The factory package produces no warnings.

95 automated checks pass. They cover:
- creation of both types (type, id, name, age, position, salary, same `Department` instance);
- trimming of id, age, name, position and numbers; the department argument winning over `departmentName`; extra keys ignored; zero values accepted;
- every error in section 8, with the field name checked in the message;
- creators used on their own; registry construction errors; registration order not mattering; a stub creator being used;
- end-to-end through `EmployeeCsvMapper`, `CsvEmployeeRepository`, `DepartmentServiceImpl` and `EmployeeServiceImpl`: add, restart and reload, update with a type change, department rename, delete, search, salary, all service error paths, and damaged employee files reported with the record number.

The test file is not part of your project.

---

## 10. Findings and limitations

1. **`Department.setName` stores the untrimmed name.** It calls `Guard.requireNotBlank(name, ...)` and ignores the returned trimmed text, then assigns `this.name = name`. Through the service this is hidden, because `DepartmentServiceImpl` passes a trimmed name. A hand-edited `departments.csv` with `" HR "` would keep the spaces, and `findByName("HR")` would not find it. Fix: `this.name = Guard.requireNotBlank(name, "Department Name");`.
2. **`EmsException` has three unused imports** from `model`. They compile but should be removed.
3. **Number format.** `Double.parseDouble` accepts a decimal point only (`1.5`, not `1,5`) and also accepts forms such as `1e3` and `10d`. The GUI should tell users to use a point.
4. **Only the first error is reported.** A form with several mistakes shows them one at a time.
5. **`EmployeeServiceImpl` was still the empty stub when this package was written.** Replace it with the implementation from the conversation. It imports `factory.EmployeeFactory`, which now exists.

---

## 11. Likely questions

**Why not put `create` inside `Employee` as a static method?** `Employee` would then have to know all of its subclasses, and adding a type would change the base class.

**Why does the factory not read `departmentName`?** See D5: the department was already resolved, and a second lookup could disagree with it.

**Why is the registry checked in the constructor and not in `create`?** See D6: a wiring mistake is found at startup in all cases, and not only for the type that happens to be used.

**Why do validators and the factory both parse numbers?** See D4: the file-loading path never runs the validators.

**What happens if a creator returns `null`?** That is a bug in the creator. The mapper rejects a `null` result with a `NullPointerException` and the repositories reject a `null` entity, so the bug becomes visible and is not saved.
