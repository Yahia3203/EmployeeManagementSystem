package service;

import exception.EmsException;
import validation.Guard;
import model.Department;
import persistence.DepartmentRepository;
import persistence.EmployeeRepository;

import java.util.List;
import java.util.Objects;

public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository deptRepo;
    private final EmployeeRepository employeeRepo;

    public DepartmentServiceImpl(DepartmentRepository deptRepo , EmployeeRepository employeeRepo) {
        this.deptRepo = Objects.requireNonNull(deptRepo, "deptRepo");
        this.employeeRepo = Objects.requireNonNull(employeeRepo, "employeeRepo");
    }

    @Override
    public void addDepartment(int id, String name) {
        Guard.requirePositive(id, "Department ID");
        String cleanName = Guard.requireNotBlank(name, "Department Name");

        if (deptRepo.existsById(id))
            throw new EmsException("Department ID " + id + " already exists!");
        if (deptRepo.findByName(cleanName) != null)
            throw new EmsException("Department name '" + cleanName + "' already exists!");

        deptRepo.save(new Department(id, cleanName));
    }
    @Override
    public void updateDepartment(int id, String name) {
        Guard.requirePositive(id, "Department ID");
        String cleanName = Guard.requireNotBlank(name, "Department Name");

        Department department = deptRepo.findById(id);
        if (department == null)
            throw new EmsException("Department with ID: " + id + " does not exist!");

        Department sameName = deptRepo.findByName(cleanName);
        if (sameName != null && sameName.getId() != id)
            throw new EmsException("Department name '" + cleanName + "' already exists!");

        String oldName = department.getName();
        department.setName(cleanName);
        try {
            deptRepo.update(department);
        } catch (RuntimeException e) {
            department.setName(oldName);
            throw e;
        }
    }

    @Override
    public void deleteDepartment(int id) {
        Guard.requirePositive(id, "Department ID");

        Department department = deptRepo.findById(id);
        if (department == null)
            throw new EmsException("Department with ID: " + id + " does not exist!");

        if (!employeeRepo.findByDepartment(department).isEmpty())
            throw new EmsException("Cannot delete department '" + department.getName()
                    + "' because it still contains employees");

        deptRepo.deleteById(id);
    }

    @Override
    public List<Department> getAllDepartments() {
        return deptRepo.findAll();
    }

    @Override
    public Department findByName(String name) {
        String cleanName = Guard.requireNotBlank(name, "Department Name");
        Department department = deptRepo.findByName(cleanName);
        if(department == null)
            throw new EmsException("Department named: "+ cleanName +" cannot be found!");
        return department;
    }
}
