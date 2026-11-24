package model;

import java.util.Map;
import exception.*;

public abstract class Employee {
    private final int id;
    private String fullName;
    private int age;
    private Department department;
    private String position;

    public Employee(int id, String fullName, int age, Department department, String position) {
        this.id = Guard.requirePositive(id, "Employee ID");
        setFullName(fullName);
        setAge(age);
        setDepartment(department);
        setPosition(position);
    }

    public int getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public final void setFullName(String fullName) {
        this.fullName = Guard.requireNotBlank(fullName, "Full name");
    }

    public int getAge() {
        return age;
    }

    public final void setAge(int age) {
        this.age = Guard.requirePositive(age, "Age");
    }

    public Department getDepartment() {
        return department;
    }

    public final void setDepartment(Department department) {
        this.department = Guard.requireNonNull(department, "Department");
    }

    public String getPosition() {
        return position;
    }

    public final void setPosition(String position) {
        this.position = Guard.requireNotBlank(position, "Position");
    }

    public abstract double calculateSalary();

    public abstract EmployeeType getType();

    public abstract Map<String, String> getTypeFields();

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Employee e && id == e.id);
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}