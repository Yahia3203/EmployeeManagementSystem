package factory;

import model.Department;
import model.Employee;
import model.EmployeeData;

/**
 * Turns raw form or file data into a validated {@link Employee} object.
 *
 * <p>Used by the service layer (when the user adds or edits an employee) and by the persistence
 * layer (when {@code employees.csv} is loaded), through the same method. Both callers therefore
 * get identical parsing and validation.
 */
public interface EmployeeFactory {

    /**
     * Builds an employee of the type given in {@code data}.
     *
     * <p>The {@code department} argument is authoritative. {@code data.departmentName()} is not
     * read, because the caller has already resolved the name to a real {@link Department}.
     *
     * @param data       raw values; id and age are text and the type-specific values are in
     *                   {@code data.typeFields()}
     * @param department the department the employee belongs to; must not be null
     * @return a new employee of the matching subclass; never null
     * @throws exception.EmsException if any value is missing or invalid
     */
    Employee create(EmployeeData data, Department department);
}
