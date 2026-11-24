package model;

import java.util.Map;

public abstract class Employee {
    private final int id;
    private String fullName;
    private int age;
    private Department department;
    private String position;


    public Employee(int id , String fullName , int age , Department department , String position) {
        this.id = id;
        this.fullName = fullName;
        this.age = age;
        this.department  = department;
        this.position = position;
    }

    public int getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }
    public abstract double calculateSalary();
    public abstract EmployeeType getType();
    public abstract Map<String , String> getTypeFields();

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Employee e && id == e.id);
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}


