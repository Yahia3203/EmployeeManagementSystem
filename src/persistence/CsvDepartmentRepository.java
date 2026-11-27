package persistence;

import model.Department;

public class CsvDepartmentRepository extends AbstractCsvRepository<Department>
        implements DepartmentRepository {

    public CsvDepartmentRepository(CsvFileStore store, CsvRecordMapper<Department> mapper) {
        super("Department", store, mapper, Department::getId);
    }


    @Override
    public Department findByName(String name) {
        if (name == null) {
            return null;
        }
        String wanted = name.trim();
        for (Department department : all()) {
            if (department.getName().trim().equalsIgnoreCase(wanted)) {
                return department;
            }
        }
        return null;
    }
}
