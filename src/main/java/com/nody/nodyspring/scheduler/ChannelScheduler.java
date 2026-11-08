package com.nody.nodyspring.scheduler;

import com.nody.nodyspring.DB.Channel;
import com.nody.nodyspring.repository.ChannelRepository;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class ChannelScheduler {

    @Autowired
    private ChannelRepository channelRepository;

    /**
     * 매일 한국 시간 00:10에 실행
     *
     * 종료일이 지난 채널의 complete를 true로 변경
     */
    @Scheduled(
            cron = "0 10 0 * * *",
            zone = "Asia/Seoul"
    )
    @Transactional
    public void processChannelsDaily() {

        LocalDate today = LocalDate.now();

        // 종료일이 오늘보다 이전이고 아직 완료되지 않은 채널 조회
        List<Channel> toComplete =
                channelRepository.findByEndDateBeforeAndCompleteFalse(today);

        // 완료 상태 변경
        toComplete.forEach(channel -> channel.setComplete(true));

        // 변경사항 저장
        if (!toComplete.isEmpty()) {
            channelRepository.saveAll(toComplete);
        }
    }
}