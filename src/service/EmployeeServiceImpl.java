package service;

import exception.EmsException;
import exception.Guard;
import factory.EmployeeFactory;
import model.Department;
import model.Employee;
import model.EmployeeData;
import persistence.EmployeeRepository;
import validation.EmployeeValidator;

import java.util.List;

public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepo;
    private final DepartmentService departmentService;
    private final EmployeeFactory factory;
    private final List<EmployeeValidator> validators;

    public EmployeeServiceImpl(EmployeeRepository employeeRepo,
                               DepartmentService departmentService,
                               EmployeeFactory factory,
                               List<EmployeeValidator> validators) {
        this.employeeRepo = employeeRepo;
        this.departmentService = departmentService;
        this.factory = factory;
        this.validators = List.copyOf(validators);
    }

    @Override
    public void addEmployee(EmployeeData data) {
        validate(data);
        int id = Guard.parsePositiveInt(data.id(), "Employee ID");

        if (employeeRepo.existsById(id))
            throw new EmsException("Employee ID " + id + " already exists!");

        Department department = departmentService.findByName(data.departmentName());
        employeeRepo.save(factory.create(data, department));
    }

    @Override
    public void updateEmployee(int id, EmployeeData data) {
        Guard.requirePositive(id, "Employee ID");
        validate(data);

        if (Guard.parseInt(data.id(), "Employee ID") != id)
            throw new EmsException("Employee ID cannot be changed");
        if (!employeeRepo.existsById(id))
            throw new EmsException("Employee with ID: " + id + " does not exist!");

        Department department = departmentService.findByName(data.departmentName());
        employeeRepo.update(factory.create(data, department));
    }

    @Override
    public void deleteEmployee(int id) {
        Guard.requirePositive(id, "Employee ID");

        if (!employeeRepo.existsById(id))
            throw new EmsException("Employee with ID: " + id + " does not exist!");

        employeeRepo.deleteById(id);
    }

    @Override
    public Employee findById(int id) {
        Guard.requirePositive(id, "Employee ID");

        Employee employee = employeeRepo.findById(id);
        if (employee == null)
            throw new EmsException("Employee with ID: " + id + " does not exist!");
        return employee;
    }

    @Override
    public List<Employee> searchByDepartment(String departmentName) {
        Department department = departmentService.findByName(departmentName);
        return employeeRepo.findByDepartment(department);
    }

    @Override
    public List<Employee> getAllEmployees() {
        return employeeRepo.findAll();
    }

    @Override
    public double getSalary(int id) {
        return findById(id).calculateSalary();
    }

    private void validate(EmployeeData data) {
        Guard.requireNonNull(data, "Employee data");
        for (EmployeeValidator validator : validators) {
            if (validator.supports(data.type()))
                validator.validate(data);
        }
    }
}