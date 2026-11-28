package factory;

import validation.Guard;
import model.Employee;
import model.EmployeeType;
import model.FieldKeys;
import model.FullTimeEmployee;

import java.util.Map;

/** Builds {@link FullTimeEmployee} objects from {@code basicSalary} and {@code bonus}. */
public final class FullTimeEmployeeCreator implements EmployeeCreator {

    @Override
    public EmployeeType supportedType() {
        return EmployeeType.FULL_TIME;
    }

    @Override
    public Employee create(CommonFields common, Map<String, String> typeFields) {
        double basicSalary = Guard.parseNonNegativeDouble(typeFields.get(FieldKeys.BASIC_SALARY), "Basic salary");
        double bonus = Guard.parseNonNegativeDouble(typeFields.get(FieldKeys.BONUS), "Bonus");
        return new FullTimeEmployee(common.id(), common.fullName(), common.age(),
                common.department(), common.position(), basicSalary, bonus);
    }
}
