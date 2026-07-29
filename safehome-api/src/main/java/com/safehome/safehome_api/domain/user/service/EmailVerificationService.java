package com.safehome.safehome_api.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final JavaMailSender mailSender;

    private final Map<String, String> codeStore = new ConcurrentHashMap<>();
    private final Map<String, Long> expireStore = new ConcurrentHashMap<>();

    private static final long EXPIRE_MS = 5 * 60 * 1000L;

    public void sendVerificationCode(String email) {
        String code = generateCode();
        codeStore.put(email, code);
        expireStore.put(email, System.currentTimeMillis() + EXPIRE_MS);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[SafeHome] 이메일 인증 코드");
        message.setText(
            "SafeHome 회원가입을 위한 이메일 인증 코드입니다.\n\n" +
            "인증 코드: " + code + "\n\n" +
            "이 코드는 5분 동안 유효합니다\n." +
            "본인이 요청하지 않은 경우 이 메일을 무시해주세요."
        );
        mailSender.send(message);
    }
    public boolean verifyCode (String email, String code) {
        String stored = codeStore.get(email);
        Long expire = expireStore.get(email);
        if (stored == null || expire == null) {
            return false;
        }
        if(System.currentTimeMillis() > expire) {
            codeStore.remove(email);
            expireStore.remove(email);
            return false;
        }
        if (!stored.equals(code)) {
            return false;
        }
        codeStore.remove(email);
        expireStore.remove(email);
        return true;
    }

    private String generateCode() {
        return String.format("%06d", new Random().nextInt(999999));
    }
    
}
