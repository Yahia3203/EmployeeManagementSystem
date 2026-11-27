package persistence;

import exception.EmsException;
import model.Department;

import java.util.Objects;

public class DepartmentCsvMapper implements CsvRecordMapper<Department> {

    private static final String[] HEADER = {"id", "name"};

    public static String[] header() {
        return HEADER.clone();
    }

    @Override
    public String[] toRecord(Department department) {
        Objects.requireNonNull(department, "department");
        return new String[] {String.valueOf(department.getId()), department.getName()};
    }

    @Override
    public Department fromRecord(String[] record) {
        if (record.length != HEADER.length) {
            throw new PersistenceException("expected " + HEADER.length
                    + " columns (id,name) but found " + record.length);
        }
        try {
            return new Department(Integer.parseInt(record[0].trim()), record[1].trim());
        } catch (NumberFormatException e) {
            throw new PersistenceException("department ID '" + record[0] + "' is not a whole number", e);
        } catch (EmsException e) {               // the model rejected the values
            throw new PersistenceException("invalid department: " + e.getMessage(), e);
        }
    }
}
