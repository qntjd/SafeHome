package com.safehome.safehome_api.domain.user.service;

import com.safehome.safehome_api.domain.user.dto.EmergencyContactDto;
import com.safehome.safehome_api.domain.user.entity.EmergencyContact;
import com.safehome.safehome_api.domain.user.entity.User;
import com.safehome.safehome_api.domain.user.repository.EmergencyContactRepository;
import com.safehome.safehome_api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmergencyContactService {

    private final EmergencyContactRepository contactRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<EmergencyContactDto.ContactResponse> getContacts(String email) {
        User user = findUser(email);
        return contactRepository.findAllByUserId(user.getId())
                .stream()
                .map(EmergencyContactDto.ContactResponse::from)
                .toList();
    }

    @Transactional
    public EmergencyContactDto.ContactResponse addContact(String email,
                                                           EmergencyContactDto.CreateRequest req) {
        User user = findUser(email);

        // 최대 5명 제한
        long count = contactRepository.findAllByUserId(user.getId()).size();
        if (count >= 5) {
            throw new IllegalStateException("비상연락처는 최대 5명까지 등록할 수 있습니다.");
        }

        EmergencyContact contact = EmergencyContact.builder()
                .user(user)
                .name(req.name())
                .phone(req.phone())
                .notifyAfterMin(req.notifyAfterMin() != null ? req.notifyAfterMin() : 10)
                .build();

        return EmergencyContactDto.ContactResponse.from(contactRepository.save(contact));
    }

    @Transactional
    public void deleteContact(String email, UUID contactId) {
        User user = findUser(email);
        EmergencyContact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new IllegalArgumentException("연락처를 찾을 수 없습니다."));

        if (!contact.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        contactRepository.delete(contact);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
    }
}