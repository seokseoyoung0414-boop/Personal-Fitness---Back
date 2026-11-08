
package com.nody.nodyspring.service;

import com.nody.nodyspring.DB.*;
import com.nody.nodyspring.dto.ActivityDTO.ActivityDTO.GoalResponse;
import com.nody.nodyspring.dto.ActivityDTO.ActivityDTO.DetailResponse;
import com.nody.nodyspring.dto.FriendDTO.FriendDTO.*;
import com.nody.nodyspring.repository.ChannelRepository;
import com.nody.nodyspring.repository.FriendRepository;
import com.nody.nodyspring.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FriendService {

    private final FriendRepository friendRepository;
    private final UserRepository userRepository;
    private final ChannelRepository channelRepository;

    // 기존 운동 통계 서비스 재사용
    private final ActivityService activityService;

    // =====================================================
    // 친구 신청
    // =====================================================

    public FriendRequestResponse requestFriend(
            String requesterUserid,
            String targetUserid
    ) {

        if (targetUserid == null || targetUserid.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "친구의 userid를 입력해주세요."
            );
        }

        if (requesterUserid.equals(targetUserid)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "자기 자신에게 친구 신청할 수 없습니다."
            );
        }

        User requester = userRepository.findById(requesterUserid)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "로그인 사용자를 찾을 수 없습니다."
                ));

        User receiver = userRepository.findById(targetUserid)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "해당 userid의 사용자가 없습니다."
                ));

        if (friendRepository.existsFriendRelation(
                requesterUserid,
                targetUserid,
                FriendStatus.ACCEPTED
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 친구인 사용자입니다."
            );
        }

        var existing = friendRepository
                .findByRequester_UseridAndReceiver_Userid(
                        requesterUserid,
                        targetUserid
                );

        if (existing.isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 친구 신청한 사용자입니다."
            );
        }

        var reverseExisting = friendRepository
                .findByRequester_UseridAndReceiver_Userid(
                        targetUserid,
                        requesterUserid
                );

        if (reverseExisting.isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "상대방이 이미 친구 신청을 보냈습니다. 받은 신청에서 수락해주세요."
            );
        }

        Friend friend = Friend.builder()
                .requester(requester)
                .receiver(receiver)
                .status(FriendStatus.PENDING)
                .build();

        Friend saved = friendRepository.save(friend);

        return new FriendRequestResponse(
                saved.getIdx(),
                receiver.getUserid(),
                receiver.getNickname(),
                saved.getCreatedAt()
        );
    }

    // =====================================================
    // 받은 친구 신청 목록
    // =====================================================

    @Transactional(readOnly = true)
    public List<FriendRequestResponse> getReceivedRequests(
            String userid
    ) {

        return friendRepository
                .findByReceiver_UseridAndStatusOrderByCreatedAtDesc(
                        userid,
                        FriendStatus.PENDING
                )
                .stream()
                .map(friend -> new FriendRequestResponse(
                        friend.getIdx(),
                        friend.getRequester().getUserid(),
                        friend.getRequester().getNickname(),
                        friend.getCreatedAt()
                ))
                .toList();
    }

    // =====================================================
    // 보낸 친구 신청 목록
    // =====================================================

    @Transactional(readOnly = true)
    public List<FriendRequestResponse> getSentRequests(
            String userid
    ) {

        return friendRepository
                .findByRequester_UseridAndStatusOrderByCreatedAtDesc(
                        userid,
                        FriendStatus.PENDING
                )
                .stream()
                .map(friend -> new FriendRequestResponse(
                        friend.getIdx(),
                        friend.getReceiver().getUserid(),
                        friend.getReceiver().getNickname(),
                        friend.getCreatedAt()
                ))
                .toList();
    }

    // =====================================================
    // 친구 신청 수락
    // =====================================================

    public void acceptRequest(
            String userid,
            Long requestIdx
    ) {

        Friend friend = friendRepository.findById(requestIdx)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "친구 신청을 찾을 수 없습니다."
                ));

        if (!friend.getReceiver().getUserid().equals(userid)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "친구 신청을 받은 사용자만 수락할 수 있습니다."
            );
        }

        if (friend.getStatus() != FriendStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 처리된 친구 신청입니다."
            );
        }

        friend.setStatus(FriendStatus.ACCEPTED);
        friend.setAcceptedAt(LocalDateTime.now());
    }

    // =====================================================
    // 친구 신청 거절
    // =====================================================

    public void rejectRequest(
            String userid,
            Long requestIdx
    ) {

        Friend friend = friendRepository.findById(requestIdx)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "친구 신청을 찾을 수 없습니다."
                ));

        if (!friend.getReceiver().getUserid().equals(userid)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "친구 신청을 받은 사용자만 거절할 수 있습니다."
            );
        }

        if (friend.getStatus() != FriendStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 처리된 친구 신청입니다."
            );
        }

        friendRepository.delete(friend);
    }

    // =====================================================
    // 친구 목록
    // =====================================================

    @Transactional(readOnly = true)
    public List<FriendResponse> getFriendList(
            String userid
    ) {

        return friendRepository
                .findAllByUserAndStatus(userid, FriendStatus.ACCEPTED)
                .stream()
                .map(friend -> {

                    User otherUser =
                            friend.getRequester().getUserid().equals(userid)
                                    ? friend.getReceiver()
                                    : friend.getRequester();

                    return new FriendResponse(
                            friend.getIdx(),
                            otherUser.getUserid(),
                            otherUser.getNickname(),
                            friend.getAcceptedAt()
                    );
                })
                .toList();
    }

    // =====================================================
    // 친구 삭제
    // =====================================================

    public void deleteFriend(
            String userid,
            String friendUserid
    ) {

        Friend friend = friendRepository
                .findByRequester_UseridAndReceiver_UseridAndStatus(
                        userid,
                        friendUserid,
                        FriendStatus.ACCEPTED
                )
                .orElseGet(() -> friendRepository
                        .findByRequester_UseridAndReceiver_UseridAndStatus(
                                friendUserid,
                                userid,
                                FriendStatus.ACCEPTED
                        )
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "친구 관계를 찾을 수 없습니다."
                        ))
                );

        friendRepository.delete(friend);
    }

    // =====================================================
    // 친구 관계 확인
    // =====================================================

    private void validateFriendRelation(
            String userid,
            String friendUserid
    ) {

        if (!friendRepository.existsFriendRelation(
                userid,
                friendUserid,
                FriendStatus.ACCEPTED
        )) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "친구 관계가 성립된 사용자만 조회할 수 있습니다."
            );
        }
    }

    // =====================================================
    // 친구 채널 목록
    // GET /friend/{friendUserid}/channels
    // =====================================================

    @Transactional(readOnly = true)
    public FriendChannelsResponse getFriendChannels(
            String userid,
            String friendUserid
    ) {

        validateFriendRelation(userid, friendUserid);

        User friend = userRepository.findById(friendUserid)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "친구 정보를 찾을 수 없습니다."
                ));

        List<FriendChannelResponse> channels =
                channelRepository
                        .findByCreator_UseridOrderByIdxDesc(friendUserid)
                        .stream()
                        .map(channel -> new FriendChannelResponse(
                                channel.getIdx(),
                                channel.getChannelTitle(),
                                channel.getSport(),
                                channel.getChannelComment(),
                                channel.getStartDate(),
                                channel.getEndDate(),
                                channel.getComplete(),
                                channel.getGoal()
                        ))
                        .toList();

        return new FriendChannelsResponse(
                friend.getUserid(),
                friend.getNickname(),
                channels
        );
    }

    // =====================================================
    // 친구 채널 상세 정보
    // GET /friend/{friendUserid}/channels/{channelIdx}
    // =====================================================

    @Transactional(readOnly = true)
    public GoalResponse getFriendChannelDetail(
            String userid,
            String friendUserid,
            Integer channelIdx
    ) {

        // 1. 친구 관계 확인
        validateFriendRelation(userid, friendUserid);

        // 2. ActivityService에서 채널 소유자 확인 및 상세 조회
        // 친구 userid를 전달하므로 친구 소유 채널만 조회 가능
        return activityService
                .getDetail(friendUserid, channelIdx)
                .activity();
    }

    // =====================================================
    // 친구 채널 운동 통계 및 기록
    // GET /friend/{friendUserid}/channels/{channelIdx}/activity/detail
    // =====================================================

    @Transactional(readOnly = true)
    public DetailResponse getFriendChannelActivityDetail(
            String userid,
            String friendUserid,
            Integer channelIdx
    ) {

        // 1. 친구 관계 확인
        validateFriendRelation(userid, friendUserid);

        // 2. 기존 ActivityService 통계 로직 재사용
        // 친구 채널의 합계, 달성률, 그래프, 기록 목록 반환
        return activityService.getDetail(
                friendUserid,
                channelIdx
        );
    }
}
