package com.moneymoment.lending.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moneymoment.lending.common.response.ApiResponse;
import com.moneymoment.lending.common.response.PagedResponse;
import com.moneymoment.lending.dtos.AssignRolesDto;
import com.moneymoment.lending.dtos.UserRequestDto;
import com.moneymoment.lending.dtos.UserResponseDto;
import com.moneymoment.lending.repos.RoleRepository;
import com.moneymoment.lending.services.UserService;

import jakarta.websocket.server.PathParam;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("api/users")
public class UserController {

    private final UserService userService;

    UserController(UserService userService) {
        this.userService = userService;

    }

    // create user
    @PostMapping()
    public ResponseEntity<ApiResponse<UserResponseDto>> createUser(@RequestBody UserRequestDto userRequestDto) {

        return ResponseEntity
                .ok(ApiResponse.success(userService.createUser(userRequestDto),
                        "User Created successfully"));

    }

    // bulk create users
    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<Void>> bulkCreateUsers(@RequestBody List<UserRequestDto> userRequestDtos) {
        String message = userService.bulkCreateUsers(userRequestDtos);
        return ResponseEntity.ok(ApiResponse.success(null, message));
    }

    // get all users
    @GetMapping()
    public ResponseEntity<ApiResponse<PagedResponse<UserResponseDto>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(userService.getAllUsers(page, size), "All users fetched"));
    }

    // get user by id
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponseDto>> getUserById(Long userId) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(userId), userId + " user found"));
    }

    // get user by id
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<ApiResponse<UserResponseDto>> getUserByEmployeeId(@PathVariable String employeeId) {
        return ResponseEntity
                .ok(ApiResponse.success(userService.getUserByEmployeeId(employeeId), employeeId + " user found"));
    }

    // get all users by role code
    @GetMapping("/role/{roleCode}")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponseDto>>> getAllUsersByRole(
            @PathVariable String roleCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                ApiResponse.success(userService.getUsersByRoleCode(roleCode, page, size), "All " + roleCode + " users fetched"));
    }

    // get all users by branchCode
    @GetMapping("/branch/{branchCode}")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponseDto>>> getAllUsersByBranch(
            @PathVariable String branchCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                ApiResponse.success(userService.getUsersByBranchCode(branchCode, page, size),
                        "All " + branchCode + " users fetched"));
    }

    // get all roles for users by emp id
    @PostMapping("/{employeeId}/roles")
    public ResponseEntity<ApiResponse<UserResponseDto>> assignRoles(
            @PathVariable String employeeId,
            @RequestBody AssignRolesDto assignRolesDto) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        userService.assignRoles(employeeId, assignRolesDto.getRoleCodes()),
                        "Roles assigned successfully"));
    }

    @DeleteMapping("/{employeeId}/roles/{roleCode}")
    public ResponseEntity<ApiResponse<UserResponseDto>> removeRole(
            @PathVariable String employeeId,
            @PathVariable String roleCode) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        userService.removeRole(employeeId, roleCode),
                        "Role removed successfully"));
    }

}
