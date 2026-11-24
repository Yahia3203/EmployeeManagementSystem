package service;

import model.Employee;
import model.EmployeeData;

import java.util.List;

public interface EmployeeService{
    void addEmployee(EmployeeData data);
    void updateEmployee(int id , EmployeeData data);
    void deleteEmployee(int id);
    Employee findById(int id);
    List<Employee> searchByDepartment(String departmentName);
    List<Employee> getAllEmployees();
    double getSalary(int id);

}
