package factory;

import model.Employee;
import model.EmployeeType;

import java.util.Map;

/**
 * Knows how to build exactly one kind of {@link Employee}.
 *
 * <p>This is the extension point of the factory package. Supporting a new employee type means
 * writing one new class that implements this interface and registering it with
 * {@link RegistryEmployeeFactory}. No existing class changes.
 */
public interface EmployeeCreator {

    /** The employee type this creator builds. Each type must have exactly one creator. */
    EmployeeType supportedType();

    /**
     * Builds the employee.
     *
     * @param common     the shared fields, already parsed
     * @param typeFields the raw {@code key -> value} text for this type's own fields
     *                   (see {@link model.FieldKeys}); never null
     * @return a new employee; never null
     * @throws exception.EmsException if a type-specific field is missing or invalid
     */
    Employee create(CommonFields common, Map<String, String> typeFields);
}
