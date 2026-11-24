package service;

import model.Department;

import java.util.List;

public interface DepartmentService {
    void addDepartment(int id,String name);
    void updateDepartment(int id , String name);
    void deleteDepartment(int id);
    List<Department> getAllDepartments();
    Department findByName(String name);
}
