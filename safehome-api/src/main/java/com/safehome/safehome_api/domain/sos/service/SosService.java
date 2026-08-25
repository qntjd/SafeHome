package com.safehome.safehome_api.domain.sos.service;

import com.safehome.safehome_api.domain.sos.dto.SosDto;
import com.safehome.safehome_api.domain.sos.entity.SosLog;
import com.safehome.safehome_api.domain.sos.entity.SosLogRecipient;
import com.safehome.safehome_api.domain.sos.repository.SosLogRepository;
import com.safehome.safehome_api.domain.user.entity.User;
import com.safehome.safehome_api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SosService {

    private final SosLogRepository sosLogRepository;
    private final UserRepository userRepository;

    @Transactional
    public SosDto.LogResponse createLog(String email, SosDto.CreateLogRequest req) {
        User user = findUser(email);

        SosLog log = SosLog.builder()
                .user(user)
                .triggerType(SosLog.TriggerType.valueOf(req.triggerType()))
                .lat(req.lat())
                .lng(req.lng())
                .address(req.address())
                .policeReported(req.policeReported() != null && req.policeReported())
                .build();

        if (req.recipients() != null) {
            req.recipients().forEach(r ->
                    log.getRecipients().add(
                            SosLogRecipient.builder()
                                    .sosLog(log)
                                    .contactName(r.contactName())
                                    .phoneNumber(r.phoneNumber())
                                    .status(SosLogRecipient.Status.valueOf(r.status()))
                                    .errorMessage(r.errorMessage())
                                    .build()
                    )
            );
        }

        return SosDto.LogResponse.from(sosLogRepository.save(log));
    }

    @Transactional(readOnly = true)
    public List<SosDto.LogResponse> getMyLogs(String email) {
        User user = findUser(email);
        return sosLogRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(SosDto.LogResponse::from).toList();
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
    }
}