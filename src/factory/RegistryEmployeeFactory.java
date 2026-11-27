package factory;

import exception.Guard;
import model.Department;
import model.Employee;
import model.EmployeeData;
import model.EmployeeType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * {@link EmployeeFactory} that looks up the right {@link EmployeeCreator} by employee type.
 *
 * <p>It parses and validates the fields shared by all employees (id, age, department), then hands
 * the type-specific fields to the creator registered for {@code data.type()}. It never mentions
 * {@code FullTimeEmployee} or {@code PartTimeEmployee}, so it does not change when a new type is added.
 *
 * <p>The registry is checked when it is built: every {@link EmployeeType} must have exactly one
 * creator. A wiring mistake therefore stops the application at startup and cannot surface later
 * as a failure while the user is saving an employee.
 */
public class RegistryEmployeeFactory implements EmployeeFactory {

    private final Map<EmployeeType, EmployeeCreator> creators = new EnumMap<>(EmployeeType.class);

    /**
     * @param creators one creator for every {@link EmployeeType}
     * @throws NullPointerException     if the list or any element is null
     * @throws IllegalArgumentException if two creators support the same type,
     *                                  or a type has no creator
     */
    public RegistryEmployeeFactory(List<? extends EmployeeCreator> creators) {
        Objects.requireNonNull(creators, "creators");
        for (EmployeeCreator creator : creators) {
            Objects.requireNonNull(creator, "creator");
            EmployeeType type = Objects.requireNonNull(creator.supportedType(), "creator.supportedType()");
            if (this.creators.putIfAbsent(type, creator) != null) {
                throw new IllegalArgumentException("more than one creator registered for " + type.name());
            }
        }
        for (EmployeeType type : EmployeeType.values()) {
            if (!this.creators.containsKey(type)) {
                throw new IllegalArgumentException("no creator registered for " + type.name());
            }
        }
    }

    @Override
    public Employee create(EmployeeData data, Department department) {
        Guard.requireNonNull(data, "Employee data");
        Guard.requireNonNull(department, "Department");
        EmployeeType type = Guard.requireNonNull(data.type(), "Employee type");

        CommonFields common = new CommonFields(
                Guard.parsePositiveInt(data.id(), "Employee ID"),
                data.fullName(),
                Guard.parsePositiveInt(data.age(), "Age"),
                department,
                data.position());

        return creators.get(type).create(common, data.typeFields());
    }
}
