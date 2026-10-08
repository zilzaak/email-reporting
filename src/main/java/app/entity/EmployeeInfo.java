package app.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name="UM_HR_Employee_Info")
@Data
public class EmployeeInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="employee_id")
    private String employeeId;

    @Column(name="email")
    private String email;

    @Column(name="active")
    private Boolean active;

    @Column(name = "employee_type_id")
    private Long empTypeId;
}
