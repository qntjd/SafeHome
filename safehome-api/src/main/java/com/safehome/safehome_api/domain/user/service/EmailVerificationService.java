package com.safehome.safehome_api.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Locale;


@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(30);
    private static final Duration VERIFIED_TTL = Duration.ofMinutes(30);
    private static final int MAX_FAIL_COUNT = 5;

    private static final String CODE_KEY = "email:code:";
    private static final String FAIL_KEY = "email:fail:";
    private static final String COOLDOWN_KEY = "email:cooldown:";
    private static final String VERIFIED_KEY = "email:verified:";

    private final JavaMailSender mailSender;
    private final RedisTemplate<String, String> redisTemplate;
    private final SecureRandom random = new SecureRandom();

    public void sendVerificationCode(String rawEmail) {
        String email = normalize(rawEmail);

        
        Boolean first = redisTemplate.opsForValue()
                .setIfAbsent(COOLDOWN_KEY + email, "1", RESEND_COOLDOWN);
        if (!Boolean.TRUE.equals(first)) {
            throw new IllegalArgumentException("잠시 후 다시 요청해주세요.");
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        redisTemplate.opsForValue().set(CODE_KEY + email, code, CODE_TTL);
        redisTemplate.delete(FAIL_KEY + email);   

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[SafeHome] 이메일 인증 코드");
        message.setText(
                "SafeHome 회원가입을 위한 이메일 인증 코드입니다.\n\n" +
                "인증 코드: " + code + "\n\n" +
                "이 코드는 5분 동안 유효합니다.\n" +
                "본인이 요청하지 않은 경우 이 메일을 무시해주세요."
        );
        mailSender.send(message);
    }

    public boolean verifyCode(String rawEmail, String code) {
        String email = normalize(rawEmail);
        String stored = redisTemplate.opsForValue().get(CODE_KEY + email);
        if (stored == null || code == null) {
            return false;   // 발송한 적 없거나 만료됨
        }

        if (!constantTimeEquals(stored, code.trim())) {
            Long fails = redisTemplate.opsForValue().increment(FAIL_KEY + email);
            if (fails != null && fails == 1L) {
                redisTemplate.expire(FAIL_KEY + email, CODE_TTL);
            }
            if (fails != null && fails >= MAX_FAIL_COUNT) {
                // 무작위 대입 방지: 5번 틀리면 인증번호 폐기
                redisTemplate.delete(CODE_KEY + email);
                redisTemplate.delete(FAIL_KEY + email);
                throw new IllegalArgumentException("인증번호를 5번 틀렸어요. 인증번호를 다시 받아주세요.");
            }
            return false;
        }

        redisTemplate.delete(CODE_KEY + email);
        redisTemplate.delete(FAIL_KEY + email);
        redisTemplate.opsForValue().set(VERIFIED_KEY + email, "1", VERIFIED_TTL);
        return true;
    }

    
    public boolean consumeVerified(String rawEmail) {
        return Boolean.TRUE.equals(redisTemplate.delete(VERIFIED_KEY + normalize(rawEmail)));
    }

    private String normalize(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("이메일을 입력해주세요.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /** 비교 시간으로 정답을 추측하지 못하도록 일정한 시간에 비교 */
    private boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }
}