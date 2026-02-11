package com.moneymoment.lending.users.entities;

import com.moneymoment.lending.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(name = "users")
public class UserEntity extends BaseEntity {
    
    @Column(name = "user_number", unique = true, nullable = false, length = 50)
    private String userNumber;
    
    @Column(name = "employee_id", unique = true, nullable = false, length = 20)
    private String employeeId;
    
    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;
    
    @Column(name = "password", nullable = false)
    private String password;
    
    @Column(name = "email", unique = true, nullable = false, length = 100)
    private String email;
    
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;
    
    @Column(name = "phone", length = 15)
    private String phone;
    
    @Column(name = "department", length = 50)
    private String department;
    
    @Column(name = "designation", length = 50)
    private String designation;
    
    @Column(name = "branch_code", length = 20)
    private String branchCode;
    
    @Column(name = "region_code", length = 20)
    private String regionCode;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private UserEntity manager;
    
    @Column(name = "joining_date")
    private LocalDate joiningDate;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<RoleEntity> roles;
}