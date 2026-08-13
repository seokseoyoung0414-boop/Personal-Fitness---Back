package com.nody.nodyspring.scheduler;

import com.nody.nodyspring.DB.Channel;
import com.nody.nodyspring.repository.ChannelRepository;
import com.nody.nodyspring.repository.ChannelUserRepository;
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

    @Autowired
    private ChannelUserRepository channelUserRepository;

    @Scheduled(cron = "0 10 0 * * *") // → KST 00:10
    @Transactional
    public void processChannelsDaily() {
        LocalDate today = LocalDate.now();

        // 1. 시작일이 오늘인데 channelBuild = false → 삭제
        List<Channel> toDelete = channelRepository.findByStartDateAndChannelBuild(today, false);

        for (Channel channel : toDelete) {
            // 자식 레코드 먼저 삭제
            channelUserRepository.deleteByChannel(channel);
        }

        // 이제 부모 채널 삭제 가능
        channelRepository.deleteAll(toDelete);

        // 2. 종료일 + 1 <= 오늘 → complete = true 처리
        List<Channel> toComplete = channelRepository.findByEndDateBeforeAndCompleteFalse(today);
        toComplete.forEach(channel -> channel.setComplete(true));
        channelRepository.saveAll(toComplete);
    }
}
