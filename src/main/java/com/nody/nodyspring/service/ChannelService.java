
package com.nody.nodyspring.service;

import com.nody.nodyspring.DB.Channel;
import com.nody.nodyspring.DB.FitnessAuth;
import com.nody.nodyspring.DB.User;
import com.nody.nodyspring.dto.ChannelDTO.ChannelDTO;
import com.nody.nodyspring.repository.ActivityRecordRepository;
import com.nody.nodyspring.repository.ChannelRepository;
import com.nody.nodyspring.repository.FitnessAuthRepository;
import com.nody.nodyspring.util.JwtUtil;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChannelService {

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private FitnessAuthRepository fitnessAuthRepository;

    @Autowired
    private ActivityRecordRepository activityRecordRepository;


    // ==========================================
    // 로그인한 사용자가 직접 생성한 채널 목록
    // ==========================================
    public List<ChannelDTO> getMyChannels(String token) throws Exception {

        User user = jwtUtil.validateToken(token);

        return channelRepository.findByCreator(user).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }


    // ==========================================
    // 공통 DTO 변환 함수
    // ==========================================
    private ChannelDTO convertToDTO(Channel channel) {

        return ChannelDTO.builder()
                .idx(channel.getIdx())
                .channelTitle(channel.getChannelTitle())
                .sport(channel.getSport())
                .channelComment(channel.getChannelComment())
                .startDate(channel.getStartDate())
                .endDate(channel.getEndDate())
                .complete(channel.getComplete())
                .goal(channel.getGoal())
                .build();
    }


    // ==========================================
    // 채널 정보 조회
    // ==========================================
    public ChannelDTO getChannelById(Integer idx) throws Exception {

        Channel channel = channelRepository.findById(idx)
                .orElseThrow(() ->
                        new Exception("채널이 존재하지 않습니다.")
                );

        return convertToDTO(channel);
    }


    // ==========================================
    // 개인 운동 목표 달성 인증
    // ==========================================
    @Transactional
    public void authenticateFitness(
            String token,
            Integer channelId,
            boolean goalMet,
            double achievedValue
    ) throws Exception {

        User user = jwtUtil.validateToken(token);

        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() ->
                        new Exception("채널이 존재하지 않습니다.")
                );

        // 본인이 생성한 채널인지 확인
        if (channel.getCreator() == null ||
                !channel.getCreator().getUserid().equals(user.getUserid())) {

            throw new IllegalStateException(
                    "본인이 생성한 채널만 인증할 수 있습니다."
            );
        }

        LocalDate today = LocalDate.now();

        // 오늘 인증 여부 확인
        boolean alreadyExists =
                fitnessAuthRepository.existsByUserAndChannelAndAuthDate(
                        user,
                        channel,
                        today
                );

        if (alreadyExists) {
            throw new IllegalStateException("이미 오늘 인증하였습니다.");
        }

        // 운동 인증 기록 저장
        FitnessAuth auth = FitnessAuth.builder()
                .user(user)
                .channel(channel)
                .authDate(today)
                .goalMet(goalMet)
                .achievedValue(achievedValue)
                .build();

        fitnessAuthRepository.save(auth);
    }


    // ==========================================
    // 개인 운동 채널 삭제
    // 연결된 운동 기록 및 인증 기록도 함께 삭제
    // ==========================================
    @Transactional
    public void deleteChannel(
            String token,
            Integer idx
    ) throws Exception {

        // 1. JWT 인증
        User user = jwtUtil.validateToken(token);

        // 2. 채널 존재 여부 확인
        Channel channel = channelRepository.findById(idx)
                .orElseThrow(() ->
                        new Exception("삭제할 채널을 찾을 수 없습니다.")
                );

        // 3. 채널 소유자 확인
        if (channel.getCreator() == null ||
                !channel.getCreator().getUserid().equals(user.getUserid())) {

            throw new IllegalStateException(
                    "본인이 생성한 채널만 삭제할 수 있습니다."
            );
        }

        // 4. 채널에 연결된 운동 기록 삭제
        activityRecordRepository.deleteByChannel_Idx(idx);

        // 5. 채널에 연결된 운동 인증 기록 삭제
        fitnessAuthRepository.deleteByChannel(channel);

        // 6. 채널 삭제
        channelRepository.delete(channel);
    }
}
