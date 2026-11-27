package persistence;

import model.Department;
import model.Employee;

import java.util.List;

public interface EmployeeRepository extends Repository<Employee, Integer> {

    List<Employee> findByDepartment(Department department);
}
