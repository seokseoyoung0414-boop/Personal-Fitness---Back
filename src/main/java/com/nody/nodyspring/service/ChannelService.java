package com.nody.nodyspring.service;

import com.nody.nodyspring.DB.Channel;
import com.nody.nodyspring.DB.ChannelUser;
import com.nody.nodyspring.DB.FitnessAuth;
import com.nody.nodyspring.DB.MogoDB.ChatRoom;
import com.nody.nodyspring.DB.User;
import com.nody.nodyspring.dto.ChannelDTO.ChannelDTO;
import com.nody.nodyspring.repository.ChannelRepository;
import com.nody.nodyspring.repository.ChannelUserRepository;
import com.nody.nodyspring.repository.FitnessAuthRepository;
import com.nody.nodyspring.repository.UserRepository;
import com.nody.nodyspring.util.JwtUtil;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChannelService {

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private ChannelUserRepository channelUserRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FitnessAuthRepository fitnessAuthRepository;

    @Autowired
    private ChatService chatService;

    // 전체 채널 (완료되지 않은 것들만)
    public List<ChannelDTO> getAllChannels() {
        return channelRepository.findByCompleteFalse().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // 로그인한 사용자의 채널 목록
    public List<ChannelDTO> getMyChannels(String token) throws Exception {
        User user = jwtUtil.validateToken(token);
        List<ChannelUser> userChannels = channelUserRepository.findByUser(user);

        return userChannels.stream()
                .map(ChannelUser::getChannel)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // 공통 DTO 변환 함수
    private ChannelDTO convertToDTO(Channel channel) {
        return ChannelDTO.builder()
                .idx(channel.getIdx())
                .channelTitle(channel.getChannelTitle())
                .sport(channel.getSport())
                .channelComment(channel.getChannelComment())
                .startDate(channel.getStartDate())
                .endDate(channel.getEndDate())
                .modes(channel.getModes())
                .complete(channel.getComplete())
                .channelBuild(channel.getChannelBuild())
                .countUser(channel.getCountUser())
                .build();
    }

    @Transactional
    public void joinChannel(Integer channelId, String userId) {
        User user = userRepository.findByUserid(userId)
                .orElseThrow(() -> new RuntimeException("사용자 없음"));
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new RuntimeException("채널 없음"));

        if (channelUserRepository.existsByUserAndChannel(user, channel)) {
            throw new RuntimeException("이미 참가함");
        }

        // 채널-유저 매핑 저장
        ChannelUser cu = ChannelUser.builder()
                .channel(channel)
                .user(user)
                .build();
        channelUserRepository.save(cu);

        // 인원 수 증가
        int newCount = channel.getCountUser() + 1;
        channel.setCountUser(newCount);

        // ✅ 채널 빌드가 아직 안됐고, 이번에 인원 5명 채우는 경우
        if (newCount >= 5 && !channel.getChannelBuild()) {
            channel.setChannelBuild(true);

            // 현재 채널의 모든 유저 가져오기
            List<ChannelUser> currentUsers = channelUserRepository.findByChannelIdx(channel.getIdx());
            List<String> participantIds = currentUsers.stream()
                    .map(cuu -> cuu.getUser().getUserid())
                    .collect(Collectors.toList());

            try {
                chatService.createRoom(participantIds, channel.getChannelTitle() + " 채팅방", channel.getIdx());
            } catch (Exception e) {
                throw new RuntimeException("채팅방 생성 실패: " + e.getMessage());
            }
        }

        // ✅ 이미 채널 빌드된 상태면, 새로 들어온 유저를 채팅방에 추가
        if (channel.getChannelBuild()) {
            ChatRoom room = chatService.findRoomByChannelId(channel.getIdx());
            if (room != null) {
                List<String> participants = room.getParticipants();
                if (!participants.contains(user.getUserid())) {
                    participants.add(user.getUserid());
                    room.setParticipants(participants);
                    chatService.saveRoom(room);
                }
            }
        }

        channelRepository.save(channel);
    }

    // 채널 정보 로딩
    public ChannelDTO getChannelById(Integer idx) throws Exception {
        Channel channel = channelRepository.findById(idx)
                .orElseThrow(() -> new Exception("채널이 존재하지 않습니다."));

        // Entity → DTO 변환
        ChannelDTO dto = new ChannelDTO();
        dto.setChannelTitle(channel.getChannelTitle());
        dto.setChannelComment(channel.getChannelComment());
        dto.setStartDate(channel.getStartDate());
        dto.setEndDate(channel.getEndDate());
        dto.setSport(channel.getSport());
        dto.setModes(channel.getModes());
        dto.setGoal(channel.getGoal());
        return dto;
    }
    //목표달성 인증로직
    @Transactional
    public void authenticateFitness(String token, Integer channelId, boolean goalMet, double achievedValue) throws Exception {
        User user = jwtUtil.validateToken(token);
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new Exception("채널이 존재하지 않습니다."));

        LocalDate today = LocalDate.now();

        // 중복 인증 체크
        boolean alreadyExists = fitnessAuthRepository.existsByUserAndChannelAndAuthDate(user, channel, today);
        if (alreadyExists) {
            throw new IllegalStateException("이미 오늘 인증하였습니다.");
        }

        // 인증 기록 저장 (달성 수치 포함)
        FitnessAuth auth = FitnessAuth.builder()
                .user(user)
                .channel(channel)
                .authDate(today)
                .goalMet(goalMet)
                .achievedValue(achievedValue)
                .build();
        fitnessAuthRepository.save(auth);
    }

    public List<Map<String, Object>> getUserGoalStatusByChannelId(Integer channelId) {
        List<ChannelUser> channelUsers = channelUserRepository.findByChannelIdx(channelId);
        LocalDate today = LocalDate.now();

        return channelUsers.stream().map(cu -> {
            String nickname = cu.getUser().getNickname();

            // 오늘 인증 기록 조회
            FitnessAuth auth = fitnessAuthRepository
                    .findByUserAndChannelAndAuthDate(cu.getUser(), cu.getChannel(), today)
                    .orElse(null);

            Map<String, Object> map = new java.util.HashMap<>();
            map.put("nickname", nickname);
            map.put("goalMet", auth != null && Boolean.TRUE.equals(auth.getGoalMet()));

            // 인증 수치도 같이 보내기 (인증 안 했으면 null)
            map.put("achievedValue", auth != null ? auth.getAchievedValue() : null);

            return map;
        }).collect(Collectors.toList());
    }
}
