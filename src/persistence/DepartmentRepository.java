package persistence;

import model.Department;

public interface DepartmentRepository extends Repository<Department, Integer> {

    Department findByName(String name);
}
