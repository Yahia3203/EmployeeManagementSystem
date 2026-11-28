package model;

import java.util.Collections;
import java.util.LinkedHashMap;
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
        Map<String, String> copy = new LinkedHashMap<>();
        if (typeFields != null) {
            typeFields.forEach((k, v) -> {
                if (k != null && v != null) copy.put(k, v);   // a null value means "not provided"
            });
        }
        typeFields = Collections.unmodifiableMap(copy);
    }
}
