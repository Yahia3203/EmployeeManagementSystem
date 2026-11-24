package model;

import java.util.Map;

public class FullTimeEmployee extends Employee {
    private double basicSalary;
    private double bonus;

    public FullTimeEmployee(int id, String fullName, int age, Department department, String position, double basicSalary , double bonus) {
        super(id, fullName, age, department, position);
        this.basicSalary = basicSalary;
        this.bonus = bonus;
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
                FieldKeys.BASIC_SALARY, String.valueOf(basicSalary),
                FieldKeys.BONUS, String.valueOf(bonus));
    }

    public double getBasicSalary() {
        return basicSalary;
    }

    public void setBasicSalary(double basicSalary) {
        this.basicSalary = basicSalary;
    }

    public double getBonus() {
        return bonus;
    }

    public void setBonus(double bonus) {
        this.bonus = bonus;
    }

}
