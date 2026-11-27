package persistence;

import exception.EmsException;
import model.Department;
import model.Employee;
import model.EmployeeData;
import model.EmployeeType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.BiFunction;

public class EmployeeCsvMapper implements CsvRecordMapper<Employee> {

    private static final String[] HEADER =
            {"type", "id", "fullName", "age", "departmentId", "position", "typeFields"};
    private static final int FIXED_COLUMNS = 6;
    private static final char KEY_VALUE_SEPARATOR = '=';

    private final BiFunction<EmployeeData, Department, Employee> employeeBuilder;
    private final DepartmentRepository departments;
    public EmployeeCsvMapper(BiFunction<EmployeeData, Department, Employee> employeeBuilder,
                             DepartmentRepository departments) {
        this.employeeBuilder = Objects.requireNonNull(employeeBuilder, "employeeBuilder");
        this.departments = Objects.requireNonNull(departments, "departments");
    }


    public static String[] header() {
        return HEADER.clone();
    }

    @Override
    public String[] toRecord(Employee employee) {
        Objects.requireNonNull(employee, "employee");
        List<String> fields = new ArrayList<>();
        fields.add(employee.getType().name());
        fields.add(String.valueOf(employee.getId()));
        fields.add(employee.getFullName());
        fields.add(String.valueOf(employee.getAge()));
        fields.add(String.valueOf(employee.getDepartment().getId()));
        fields.add(employee.getPosition());

        for (Map.Entry<String, String> pair : new TreeMap<>(employee.getTypeFields()).entrySet()) {
            if (pair.getKey().indexOf(KEY_VALUE_SEPARATOR) >= 0) {
                throw new PersistenceException("type field key '" + pair.getKey()
                        + "' must not contain '" + KEY_VALUE_SEPARATOR + "'");
            }
            fields.add(pair.getKey() + KEY_VALUE_SEPARATOR + pair.getValue());
        }
        return fields.toArray(new String[0]);
    }

    @Override
    public Employee fromRecord(String[] record) {
        if (record.length < FIXED_COLUMNS) {
            throw new PersistenceException("expected at least " + FIXED_COLUMNS
                    + " columns but found " + record.length);
        }
        try {
            EmployeeType type = EmployeeType.valueOf(record[0].trim());
            int departmentId = Integer.parseInt(record[4].trim());

            Department department = departments.findById(departmentId);
            if (department == null) {
                throw new PersistenceException("employee refers to department ID "
                        + departmentId + ", which does not exist");
            }

            Map<String, String> typeFields = new LinkedHashMap<>();
            for (int i = FIXED_COLUMNS; i < record.length; i++) {
                int separator = record[i].indexOf(KEY_VALUE_SEPARATOR);
                if (separator < 1) {
                    throw new PersistenceException("'" + record[i] + "' must look like key"
                            + KEY_VALUE_SEPARATOR + "value");
                }
                typeFields.put(record[i].substring(0, separator).trim(),
                               record[i].substring(separator + 1).trim());
            }

            EmployeeData data = new EmployeeData(record[1].trim(), record[2], record[3].trim(),
                    department.getName(), record[5], type, typeFields);
            return Objects.requireNonNull(employeeBuilder.apply(data, department),
                    "the employee builder returned null");

        } catch (PersistenceException e) {
            throw e;
        } catch (EmsException | IllegalArgumentException e) {
            throw new PersistenceException("invalid employee: " + e.getMessage(), e);
        }
    }
}
