package persistence;

import model.Department;
import model.Employee;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CsvEmployeeRepository extends AbstractCsvRepository<Employee>
        implements EmployeeRepository {

    public CsvEmployeeRepository(CsvFileStore store, CsvRecordMapper<Employee> mapper) {
        super("Employee", store, mapper, Employee::getId);
    }

    @Override
    public List<Employee> findByDepartment(Department department) {
        Objects.requireNonNull(department, "department");
        List<Employee> result = new ArrayList<>();
        for (Employee employee : all()) {
            if (employee.getDepartment().getId() == department.getId()) {
                result.add(employee);
            }
        }
        return List.copyOf(result);
    }
}
