package validation;

import model.EmployeeData;
import model.EmployeeType;

public class CommonFieldsValidator implements EmployeeValidator {
    public boolean supports(EmployeeType type) { return true; }

    public void validate(EmployeeData data) {
        Guard.parsePositiveInt(data.id(), "Employee ID");
        Guard.requireNotBlank(data.fullName(), "Full name");
        Guard.requireAge(Guard.parseInt(data.age(), "Age"), "Age");
        Guard.requireNotBlank(data.departmentName(), "Department");
        Guard.requireNotBlank(data.position(), "Position");
        Guard.requireNonNull(data.type(), "Employee type");
    }
}
