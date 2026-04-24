package com.safehome.safehome_api.domain.user.dto;

import com.safehome.safehome_api.domain.user.entity.EmergencyContact;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public class EmergencyContactDto {

    public record CreateRequest(
            @NotBlank String name,
            @NotBlank @Pattern(regexp = "^01[0-9]{8,9}$", message = "올바른 전화번호 형식이 아닙니다.") 
            String phone,
            Integer notifyAfterMin
    ) {}

    public record ContactResponse(
            UUID id,
            String name,
            String phone,
            Integer notifyAfterMin
    ) {
        public static ContactResponse from(EmergencyContact c) {
            return new ContactResponse(
                    c.getId(),
                    c.getName(),
                    c.getPhone(),
                    c.getNotifyAfterMin()
            );
        }
    }
}