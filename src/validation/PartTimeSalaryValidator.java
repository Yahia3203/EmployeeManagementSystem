package validation;

import model.EmployeeData;
import model.EmployeeType;
import model.FieldKeys;

public class PartTimeSalaryValidator implements EmployeeValidator {
    public boolean supports(EmployeeType type) { return type == EmployeeType.PART_TIME; }

    public void validate(EmployeeData data) {
        Guard.parseNonNegativeDouble(data.typeFields().get(FieldKeys.HOURLY_RATE), "Hourly rate");
        Guard.parseNonNegativeDouble(data.typeFields().get(FieldKeys.HOURS_WORKED), "Hours worked");
    }
}
