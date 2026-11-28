package model;

import validation.Guard;

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
        this.id = Guard.requirePositive(id, "Department ID");
    }

    public String getName() {
        return name;
    }

    public final void setName(String name) {
        this.name = Guard.requireNotBlank(name, "Department Name");
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Department d && id == d.id);
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return name;
    }
}
