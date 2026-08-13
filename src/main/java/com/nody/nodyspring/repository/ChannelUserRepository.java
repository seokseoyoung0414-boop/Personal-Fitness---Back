package com.nody.nodyspring.repository;

import com.nody.nodyspring.DB.Channel;
import com.nody.nodyspring.DB.ChannelUser;
import com.nody.nodyspring.DB.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChannelUserRepository extends JpaRepository<ChannelUser, Integer> {
    List<ChannelUser> findByUser(User user);
    List<ChannelUser> findByChannelIdx(Integer idx);
    boolean existsByUserAndChannel(User user, Channel channel);
    void deleteByChannel(Channel channel);
    long countByChannel(Channel channel);
}
