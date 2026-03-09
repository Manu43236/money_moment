package com.moneymoment.lending.services;

import com.moneymoment.lending.common.constants.AppConstants;
import com.moneymoment.lending.common.exception.DuplicateRecordException;
import com.moneymoment.lending.common.exception.ResourceNotFoundException;
import com.moneymoment.lending.common.utils.NumberGenerator;
import com.moneymoment.lending.common.exception.BusinessLogicException;
import com.moneymoment.lending.dtos.ChangePasswordDto;
import com.moneymoment.lending.dtos.LoginRequestDto;
import com.moneymoment.lending.dtos.LoginResponseDto;
import com.moneymoment.lending.dtos.RoleResponseDto;
import com.moneymoment.lending.dtos.UpdateProfileDto;
import com.moneymoment.lending.dtos.UserRequestDto;
import com.moneymoment.lending.dtos.UserResponseDto;
import com.moneymoment.lending.entities.RoleEntity;
import com.moneymoment.lending.entities.UserEntity;
import com.moneymoment.lending.repos.RoleRepository;
import com.moneymoment.lending.repos.UserRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moneymoment.lending.common.response.PagedResponse;

import com.moneymoment.lending.security.JwtUtil;

@Service
public class UserService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    UserService(UserRepository userRepository, RoleRepository roleRepository,
            PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
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
        userEntity.setPassword(passwordEncoder.encode(request.getPassword()));
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

    @Transactional
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
    @Transactional(readOnly = true)
    public UserResponseDto getUserById(Long id) {
        UserEntity userEntity = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return toDto(userEntity);
    }

    // get user by employeeId
    @Transactional
    public UserResponseDto getUserByEmployeeId(String employeeId) {
        UserEntity userEntity = userRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "employeeId", employeeId));
        return toDto(userEntity);
    }

    // get all users
    @Transactional
    public PagedResponse<UserResponseDto> getAllUsers(int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PagedResponse.of(userRepository.findAll(pageable).map(this::toDto));
    }

    // get user by role code
    @Transactional
    public PagedResponse<UserResponseDto> getUsersByRoleCode(String roleCode, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PagedResponse.of(userRepository.findByRoles_RoleCode(roleCode, pageable).map(this::toDto));
    }

    // get all users by branch code
    @Transactional
    public PagedResponse<UserResponseDto> getUsersByBranchCode(String branchCode, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PagedResponse.of(userRepository.findByBranchCode(branchCode, pageable).map(this::toDto));
    }

    // get all assigned roles
    @Transactional
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

    // Bulk create users
    public String bulkCreateUsers(List<UserRequestDto> requests) {
        List<String> skipped = new ArrayList<>();
        for (UserRequestDto request : requests) {
            try {
                createUser(request);
            } catch (DuplicateRecordException e) {
                skipped.add(request.getUsername());
            }
        }
        if (skipped.isEmpty()) {
            return "All users are created";
        }
        return "Except " + String.join(", ", skipped) + " rest of all users are created";
    }

    // Login
    @Transactional(readOnly = true)
    public LoginResponseDto login(LoginRequestDto request) {
        UserEntity user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", request.getUsername()));

        if (!user.getIsActive()) {
            throw new RuntimeException("User account is inactive");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        LoginResponseDto response = new LoginResponseDto();
        response.setId(user.getId());
        response.setUserNumber(user.getUserNumber());
        response.setEmployeeId(user.getEmployeeId());
        response.setUsername(user.getUsername());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setDepartment(user.getDepartment());
        response.setDesignation(user.getDesignation());
        response.setBranchCode(user.getBranchCode());
        response.setRegionCode(user.getRegionCode());
        response.setRoles(user.getRoles().stream().map(this::roleToDto).collect(Collectors.toSet()));

        String roles = user.getRoles().stream()
                .map(r -> r.getRoleCode())
                .collect(Collectors.joining(","));
        response.setToken(jwtUtil.generateToken(user.getUsername(), user.getEmployeeId(), roles));

        return response;
    }

    // Get my profile
    @Transactional(readOnly = true)
    public UserResponseDto getMyProfile(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        return toDto(user);
    }

    // Update my profile
    @Transactional
    public UserResponseDto updateMyProfile(String username, UpdateProfileDto request) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        if (request.getFullName() != null && !request.getFullName().isBlank())
            user.setFullName(request.getFullName());

        if (request.getPhone() != null && !request.getPhone().isBlank())
            user.setPhone(request.getPhone());

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            userRepository.findByEmail(request.getEmail())
                    .filter(existing -> !existing.getId().equals(user.getId()))
                    .ifPresent(e -> { throw new DuplicateRecordException("Email already in use"); });
            user.setEmail(request.getEmail());
        }

        return toDto(userRepository.save(user));
    }

    // Change my password
    @Transactional
    public void changeMyPassword(String username, ChangePasswordDto request) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword()))
            throw new BusinessLogicException("Current password is incorrect");

        if (request.getNewPassword().length() < 6)
            throw new BusinessLogicException("New password must be at least 6 characters");

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    // Remove role from user
    @Transactional
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