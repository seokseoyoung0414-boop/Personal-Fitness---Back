package com.nody.nodyspring.service;

import com.nody.nodyspring.DB.Board;
import com.nody.nodyspring.DB.User;
import com.nody.nodyspring.dto.BoardDTO.BoardDTO;
import com.nody.nodyspring.repository.BoardRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BoardService {

    private final BoardRepository boardRepository;
    @Autowired
    private StringRedisTemplate redisTemplate;

    public BoardService(BoardRepository boardRepository) {
        this.boardRepository = boardRepository;
    }

    public List<BoardDTO> getAllPosts() {
        List<Board> boardList = boardRepository.findAll();

        return boardList.stream()
                .map(b -> new BoardDTO(
                        b.getIdx(),
                        b.getTitle(),
                        b.getContents(),
                        b.getWriter().getUserid(),  // User 객체에서 ID 추출
                        b.getViewcount(),
                        b.getRegdate()
                ))
                .collect(Collectors.toList());
    }

    public List<BoardDTO> getMyPosts(String userid) {
        List<Object[]> results = boardRepository.findAllFieldsByWriterId(userid);
        return results.stream()
                .map(row -> new BoardDTO(
                        (Integer) row[0],
                        (String) row[1],
                        (String) row[2],
                        (String) row[3],
                        (Integer) row[4],
                        (LocalDateTime) row[5]
                ))
                .collect(Collectors.toList());
    }
    @Transactional
    public BoardDTO getBoardByIdx(Integer idx, String ipAddress) {
        Board board = boardRepository.findById(idx)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글이 존재하지 않습니다."));

        String key = "viewed:" + idx + ":" + ipAddress;

        Boolean alreadyViewed = redisTemplate.hasKey(key);

        if (Boolean.FALSE.equals(alreadyViewed)) {
            board.setViewcount(board.getViewcount() + 1);
            redisTemplate.opsForValue().set(key, "true", Duration.ofHours(24)); // 24시간 후 만료
        }

        // JPA는 @Transactional 하에서 setter만 호출해도 dirty checking으로 update 처리됨
        return new BoardDTO(
                board.getIdx(),
                board.getTitle(),
                board.getContents(),
                board.getWriter().getUserid(),
                board.getViewcount(),  // 증가된 값
                board.getRegdate()
        );
    }
    public Board createBoard(BoardDTO dto, User user) {
        Board board = Board.builder()
                .title(dto.getTitle())
                .contents(dto.getContents())
                .writer(user)
                .viewcount(0)
                .regdate(LocalDateTime.now())
                .build();
        return boardRepository.save(board);
    }

    // 게시글 수정
    public void editBoard(Integer idx, BoardDTO dto, User user) throws Exception {
        Board board = boardRepository.findById(idx)
                .orElseThrow(() -> new Exception("게시글이 없습니다."));
        // 작성자 확인
        if (!board.getWriter().getUserid().equals(user.getUserid())) {
            throw new Exception("작성자만 수정할 수 있습니다.");
        }
        board.setTitle(dto.getTitle());
        board.setContents(dto.getContents());
        boardRepository.save(board);
    }

    public void deleteBoard(Integer idx, User user) throws Exception {
        Board board = boardRepository.findById(idx)
                .orElseThrow(() -> new Exception("게시글이 없습니다."));
        if (!board.getWriter().getUserid().equals(user.getUserid())) {
            throw new Exception("작성자만 삭제할 수 있습니다.");
        }
        boardRepository.delete(board);
    }
}

