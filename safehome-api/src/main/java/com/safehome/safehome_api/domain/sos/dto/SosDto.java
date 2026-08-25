package com.safehome.safehome_api.domain.sos.dto;

import com.safehome.safehome_api.domain.sos.entity.SosLog;
import com.safehome.safehome_api.domain.sos.entity.SosLogRecipient;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
import java.time.LocalDateTime;
import java.util.UUID;

public class SosDto {

    public record RecipientRequest(
            String contactName,
            @NotNull String phoneNumber,
            @NotNull String status,
            String errorMessage
    ) {}

    public record CreateLogRequest(
            @NotNull String triggerType,
            Double lat,
            Double lng,
            String address,
            Boolean policeReported,
            List<RecipientRequest> recipients
    ) {}

    public record RecipientResponse(
            String contactName,
            String phoneNumber,
            String status,
            String errorMessage
    ) implements Serializable {
        public static RecipientResponse from(SosLogRecipient r) {
            return new RecipientResponse(
                r.getContactName(), r.getPhoneNumber(), r.getStatus().name(), r.getErrorMessage());
        }
    }

    public record LogResponse(
            UUID id,
            String triggerType,
            Double lat,
            Double lng,
            String address,
            Boolean policeReported,
            LocalDateTime createdAt,
            List<RecipientResponse> recipients
    ) implements Serializable {
        public static LogResponse from(SosLog log) {
            return new LogResponse(log.getId(), log.getTriggerType().name(), log.getLat(), 
                log.getLng(), log.getAddress(),log.getPoliceReported(),
                log.getCreatedAt(),log.getRecipients().stream().map(RecipientResponse::from).toList());
        }
    }

}
