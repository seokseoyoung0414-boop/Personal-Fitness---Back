package com.nody.nodyspring.service;

import com.nody.nodyspring.DB.User;
import com.nody.nodyspring.dto.UserDTO.LoginRequestDto;
import com.nody.nodyspring.dto.UserDTO.ResetPasswdDTO;
import com.nody.nodyspring.dto.UserDTO.SignupRequestDto;
import com.nody.nodyspring.dto.UserDTO.UserUpdateDTO;
import com.nody.nodyspring.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void signupCheck(SignupRequestDto dto) {
        if (userRepository.existsByUserid(dto.getUserid())) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        } else if (userRepository.existsByNickname(dto.getNickname())) {
            throw new IllegalArgumentException("이미 존재하는 닉네임입니다.");
        } else if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        } else if (userRepository.existsByPhone(dto.getPhone())) {
            throw new IllegalArgumentException("이미 존재하는 전화번호입니다.");
        }
    }

    public void signup(SignupRequestDto dto) {
        if (userRepository.existsByUserid(dto.getUserid())) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        } else if (userRepository.existsByNickname(dto.getNickname())) {
            throw new IllegalArgumentException("이미 존재하는 닉네임입니다.");
        } else if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        } else if (userRepository.existsByPhone(dto.getPhone())) {
            throw new IllegalArgumentException("이미 존재하는 전화번호입니다.");
        }

        String encodedPassword = passwordEncoder.encode(dto.getPasswd());
        dto.setPasswd(encodedPassword);


        User user = new User();
        user.setUserid(dto.getUserid());
        user.setPasswd(encodedPassword);
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());

        userRepository.save(user);
    }

    public User login(LoginRequestDto dto) {
        return userRepository.findByUserid(dto.getUserid())
                .filter(user -> passwordEncoder.matches(dto.getPasswd(), user.getPasswd()))
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다."));
    }

    public void resetPassword(ResetPasswdDTO dto, SmsService smsService) {
        String phone = smsService.getPhoneBySessionId(dto.getSessionId());
        if (phone == null) {
            throw new IllegalArgumentException("유효하지 않은 세션입니다.");
        }

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new IllegalArgumentException("해당 번호로 가입된 사용자를 찾을 수 없습니다."));

        user.setPasswd(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
    }

    public String findUserIdByEmail(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        return userOptional.map(User::getUserid).orElse(null);
    }

    public Map<String, String> getAllUserNicknameMap() {
        List<User> userList = userRepository.findAll();
        Map<String, String> map = new HashMap<>();
        for (User user : userList) {
            map.put(user.getUserid(), user.getNickname());
        }
        return map;
    }

    public User findByUserid(String userid) {
        return userRepository.findByUserid(userid)
                .orElseThrow(() -> new UsernameNotFoundException("사용자 없음"));
    }
    @Transactional
    public boolean updateUser(UserUpdateDTO dto) {
        return userRepository.findById(dto.getUserid())
                .map(user -> {
                    if (dto.getNickname() != null && !dto.getNickname().isBlank()) {
                        user.setNickname(dto.getNickname());
                    }
                    if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
                        user.setEmail(dto.getEmail());
                    }
                    if (dto.getPhone() != null && !dto.getPhone().isBlank()) {
                        user.setPhone(dto.getPhone());
                    }
                    if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
                        user.setPasswd(passwordEncoder.encode(dto.getPassword()));
                    }

                    // JPA는 변경 감지를 통해 자동 UPDATE 수행
                    return true;
                })
                .orElse(false);
    }
}