package model;

import validation.Guard;

import java.util.Map;

public class FullTimeEmployee extends Employee {
    private double basicSalary;
    private double bonus;

    public FullTimeEmployee(int id, String fullName, int age, Department department,
                            String position, double basicSalary, double bonus) {
        super(id, fullName, age, department, position);
        setBasicSalary(basicSalary);
        setBonus(bonus);
    }

    @Override
    public double calculateSalary() {
        return basicSalary + bonus;
    }

    @Override
    public EmployeeType getType() {
        return EmployeeType.FULL_TIME;
    }

    @Override
    public Map<String, String> getTypeFields() {
        return Map.of(
                FieldKeys.BASIC_SALARY, plain(basicSalary),
                FieldKeys.BONUS, plain(bonus));
    }

    public double getBasicSalary() {
        return basicSalary;
    }

    public final void setBasicSalary(double basicSalary) {
        this.basicSalary = Guard.requireNonNegative(basicSalary, "Basic salary");
    }

    public double getBonus() {
        return bonus;
    }

    public final void setBonus(double bonus) {
        this.bonus = Guard.requireNonNegative(bonus, "Bonus");
    }
}
