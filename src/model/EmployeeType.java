package model;

public enum EmployeeType {
    FULL_TIME("Full-Time"),
    PART_TIME("Part-Time");

    private final String label;

    EmployeeType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
