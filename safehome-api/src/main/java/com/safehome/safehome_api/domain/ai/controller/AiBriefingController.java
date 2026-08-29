package com.safehome.safehome_api.domain.ai.controller;

import com.safehome.safehome_api.domain.ai.service.AiBriefingService;
import com.safehome.safehome_api.domain.safety.service.SafetyScoreService;
import com.safehome.safehome_api.domain.user.entity.User;
import com.safehome.safehome_api.domain.user.repository.UserRepository;
import com.safehome.safehome_api.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiBriefingController {

    private final AiBriefingService aiBriefingService;
    private final UserRepository userRepository;

    @GetMapping("/briefing")
    public ApiResponse<String> getBriefing(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String districtCode,
            @RequestParam String districtName,
            @RequestParam int safetyScore,
            @RequestParam String grade
    ) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        String briefing = aiBriefingService.getTodayBriefing(user, districtName, safetyScore, grade);
        return ApiResponse.success(briefing);
    }
}