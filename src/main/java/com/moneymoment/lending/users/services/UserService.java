package com.moneymoment.lending.users.services;

import com.moneymoment.lending.common.constants.AppConstants;
import com.moneymoment.lending.common.exception.DuplicateRecordException;
import com.moneymoment.lending.common.exception.ResourceNotFoundException;
import com.moneymoment.lending.common.utils.NumberGenerator;
import com.moneymoment.lending.users.dtos.RoleResponseDto;
import com.moneymoment.lending.users.dtos.UserRequestDto;
import com.moneymoment.lending.users.dtos.UserResponseDto;
import com.moneymoment.lending.users.entities.RoleEntity;
import com.moneymoment.lending.users.entities.UserEntity;
import com.moneymoment.lending.users.repos.RoleRepository;
import com.moneymoment.lending.users.repos.UserRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final RoleRepository roleRepository;

    private final UserRepository userRepository;

    UserService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        // Constructor for UserService
    }

    public UserResponseDto createUser(UserRequestDto request) {

        // 1. Validate duplicates
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new DuplicateRecordException("User with username already exists");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new DuplicateRecordException("User with email already exists");
        }
        if (userRepository.findByEmployeeId(request.getEmployeeId()).isPresent()) {
            throw new DuplicateRecordException("User with employee ID already exists");
        }

        // 2. Fetch manager (if provided)
        UserEntity manager = null;
        if (request.getManagerEmployeeId() != null && !request.getManagerEmployeeId().isEmpty()) {
            manager = userRepository.findByEmployeeId(request.getManagerEmployeeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager", "employeeId",
                            request.getManagerEmployeeId()));
        }

        // 3. Fetch roles
        Set<RoleEntity> roles = new HashSet<>();
        for (String roleCode : request.getRoleCodes()) {
            var role = roleRepository.findByRoleCode(roleCode)
                    .orElseThrow(() -> new ResourceNotFoundException("Role", "roleCode", roleCode));
            roles.add(role);
        }

        // 4. Generate userNumber
        String userNumber = NumberGenerator.numberGeneratorWithPrifix(AppConstants.USER_NUMBER_PREFIX);

        // 5. Create entity
        UserEntity userEntity = new UserEntity();
        userEntity.setUserNumber(userNumber);
        userEntity.setEmployeeId(request.getEmployeeId());
        userEntity.setUsername(request.getUsername());
        userEntity.setPassword(request.getPassword()); // TODO: Hash in Week 4
        userEntity.setEmail(request.getEmail());
        userEntity.setFullName(request.getFullName());
        userEntity.setPhone(request.getPhone());
        userEntity.setDepartment(request.getDepartment());
        userEntity.setDesignation(request.getDesignation());
        userEntity.setBranchCode(request.getBranchCode());
        userEntity.setRegionCode(request.getRegionCode());
        userEntity.setManager(manager);
        userEntity.setJoiningDate(request.getJoiningDate());
        userEntity.setRoles(roles);
        userEntity.setIsActive(true);

        // 6. Save
        userEntity = userRepository.save(userEntity);

        // 7. Convert to DTO
        return toDto(userEntity);
    }

    private RoleResponseDto roleToDto(RoleEntity role) {
        RoleResponseDto dto = new RoleResponseDto();
        dto.setId(role.getId());
        dto.setRoleCode(role.getRoleCode());
        dto.setRoleName(role.getRoleName());
        dto.setDescription(role.getDescription());
        dto.setMaxApprovalAmount(role.getMaxApprovalAmount());
        dto.setCanApprove(role.getCanApprove());
        dto.setCanRecommend(role.getCanRecommend());
        dto.setCanVeto(role.getCanVeto());
        dto.setApprovalLevel(role.getApprovalLevel());
        return dto;
    }

    private UserResponseDto toDto(UserEntity userEntity) {
        UserResponseDto dto = new UserResponseDto();
        dto.setId(userEntity.getId());
        dto.setUserNumber(userEntity.getUserNumber());
        dto.setEmployeeId(userEntity.getEmployeeId());
        dto.setUsername(userEntity.getUsername());
        dto.setEmail(userEntity.getEmail());
        dto.setFullName(userEntity.getFullName());
        dto.setPhone(userEntity.getPhone());
        dto.setDepartment(userEntity.getDepartment());
        dto.setDesignation(userEntity.getDesignation());
        dto.setBranchCode(userEntity.getBranchCode());
        dto.setRegionCode(userEntity.getRegionCode());

        if (userEntity.getManager() != null) {
            dto.setManagerId(userEntity.getManager().getId());
            dto.setManagerName(userEntity.getManager().getFullName());
            dto.setManagerEmployeeId(userEntity.getManager().getEmployeeId());
        }

        dto.setJoiningDate(userEntity.getJoiningDate());
        dto.setIsActive(userEntity.getIsActive());

        // Convert roles to RoleResponseDto
        // Convert roles to RoleResponseDto
        Set<RoleResponseDto> roleDtos = userEntity.getRoles().stream()
                .map(this::roleToDto)
                .collect(Collectors.toSet());
        dto.setRoles(roleDtos);

        dto.setCreatedAt(userEntity.getCreatedAt());
        dto.setUpdatedAt(userEntity.getUpdatedAt());

        return dto;
    }

    // get user by id
    public UserResponseDto getUserById(Long id) {
        UserEntity userEntity = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return toDto(userEntity);
    }

    // get user by employeeId
    public UserResponseDto getUserByEmployeeId(String employeeId) {
        UserEntity userEntity = userRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "employeeId", employeeId));
        return toDto(userEntity);
    }

    // get all users
    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // get user by role code
    public List<UserResponseDto> getUsersByRoleCode(String roleCode) {
        return userRepository.findAll().stream()
                .filter(user -> user.getRoles().stream()
                        .anyMatch(role -> role.getRoleCode().equals(roleCode)))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // get all users by branch code
    public List<UserResponseDto> getUsersByBranchCode(String branchCode) {
        return userRepository.findAll().stream()
                .filter(user -> user.getBranchCode().equals(branchCode))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // get all assigned roles
    public UserResponseDto assignRoles(String employeeId, Set<String> roleCodes) {
        // Fetch user
        UserEntity user = userRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "employeeId", employeeId));

        // Fetch roles
        Set<RoleEntity> newRoles = new HashSet<>();
        for (String roleCode : roleCodes) {
            RoleEntity role = roleRepository.findByRoleCode(roleCode)
                    .orElseThrow(() -> new ResourceNotFoundException("Role", "roleCode", roleCode));
            newRoles.add(role);
        }

        // Add new roles to existing roles
        user.getRoles().addAll(newRoles);

        // Save
        user = userRepository.save(user);

        return toDto(user);
    }

    // Remove role from user
    public UserResponseDto removeRole(String employeeId, String roleCode) {
        UserEntity user = userRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "employeeId", employeeId));

        RoleEntity role = roleRepository.findByRoleCode(roleCode)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "roleCode", roleCode));

        user.getRoles().remove(role);
        user = userRepository.save(user);

        return toDto(user);
    }

}