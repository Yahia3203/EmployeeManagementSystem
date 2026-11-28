package factory;

import validation.Guard;
import model.Employee;
import model.EmployeeType;
import model.FieldKeys;
import model.PartTimeEmployee;

import java.util.Map;

/** Builds {@link PartTimeEmployee} objects from {@code hourlyRate} and {@code hoursWorked}. */
public final class PartTimeEmployeeCreator implements EmployeeCreator {

    @Override
    public EmployeeType supportedType() {
        return EmployeeType.PART_TIME;
    }

    @Override
    public Employee create(CommonFields common, Map<String, String> typeFields) {
        double hourlyRate = Guard.parseNonNegativeDouble(typeFields.get(FieldKeys.HOURLY_RATE), "Hourly rate");
        double hoursWorked = Guard.parseNonNegativeDouble(typeFields.get(FieldKeys.HOURS_WORKED), "Hours worked");
        return new PartTimeEmployee(common.id(), common.fullName(), common.age(),
                common.department(), common.position(), hourlyRate, hoursWorked);
    }
}
