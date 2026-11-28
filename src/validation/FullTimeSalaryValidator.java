package validation;

import model.EmployeeData;
import model.EmployeeType;
import model.FieldKeys;

public class FullTimeSalaryValidator implements EmployeeValidator {
    public boolean supports(EmployeeType type) { return type == EmployeeType.FULL_TIME; }

    public void validate(EmployeeData data) {
        Guard.parseNonNegativeDouble(data.typeFields().get(FieldKeys.BASIC_SALARY), "Basic salary");
        Guard.parseNonNegativeDouble(data.typeFields().get(FieldKeys.BONUS), "Bonus");
    }
}
