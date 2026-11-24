package model;

import exception.Guard; 

public class Department {
    private int id;
    private String name;

    public Department(int id, String name) {
        setId(id);
        setName(name);
    }

    public int getId() {
        return id;
    }

    private void setId(int id) {
        Guard.requirePositive(id , "Department ID");
        this.id = id;
    }

    public String getName() {
        return name;
    }

    private void setName(String name) {
        Guard.requireNotBlank(name , "Department Name");
        this.name = name;
    }
}
