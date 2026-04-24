package com.safehome.safehome_api.domain.user.controller;

import com.safehome.safehome_api.domain.user.dto.EmergencyContactDto;
import com.safehome.safehome_api.domain.user.service.EmergencyContactService;
import com.safehome.safehome_api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Emergency Contact", description = "비상연락처 API")
@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
public class EmergencyContactController {

    private final EmergencyContactService contactService;

    @Operation(summary = "비상연락처 목록 조회")
    @GetMapping
    public ApiResponse<List<EmergencyContactDto.ContactResponse>> getContacts(
            @AuthenticationPrincipal UserDetails user
    ) {
        return ApiResponse.success(contactService.getContacts(user.getUsername()));
    }

    @Operation(summary = "비상연락처 추가")
    @PostMapping
    public ApiResponse<EmergencyContactDto.ContactResponse> addContact(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody EmergencyContactDto.CreateRequest req
    ) {
        return ApiResponse.success(contactService.addContact(user.getUsername(), req));
    }

    @Operation(summary = "비상연락처 삭제")
    @DeleteMapping("/{contactId}")
    public ApiResponse<Void> deleteContact(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable UUID contactId
    ) {
        contactService.deleteContact(user.getUsername(), contactId);
        return ApiResponse.success(null);
    }
}