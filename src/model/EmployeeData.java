package model;

import java.util.Map;

public record EmployeeData(
        String id,
        String fullName,
        String age,
        String departmentName,
        String position,
        EmployeeType type,
        Map<String, String> typeFields) {

    public EmployeeData {
        typeFields = (typeFields == null) ? Map.of() : Map.copyOf(typeFields);
    }
}