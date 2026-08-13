package com.nody.nodyspring.service;

import com.nody.nodyspring.DB.Board;
import com.nody.nodyspring.DB.Comment;
import com.nody.nodyspring.DB.User;
import com.nody.nodyspring.dto.CommentDTO.CommentCreateDTO;
import com.nody.nodyspring.dto.CommentDTO.CommentDTO;
import com.nody.nodyspring.dto.CommentDTO.CommentDeleteDTO;
import com.nody.nodyspring.repository.BoardRepository;
import com.nody.nodyspring.repository.CommentRepository;
import com.nody.nodyspring.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

    public List<CommentDTO> getCommentsByBoardIdx(Integer boardIdx) {
        List<Comment> comments = commentRepository.findByBoardby_IdxOrderByTimesAsc(boardIdx);
        return comments.stream().map(c -> CommentDTO.builder()
                .idx(c.getIdx())
                .writeby(c.getWriteby().getUserid())
                .texts(c.getTexts())
                .times(c.getTimes())
                .build()
        ).collect(Collectors.toList());
    }

    public void addComment(User user, CommentCreateDTO dto) {
        Board board = boardRepository.findById(dto.getBoardIdx())
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다."));

        Comment comment = Comment.builder()
                .boardby(board)
                .writeby(user)
                .texts(dto.getCommentText())
                .times(LocalDateTime.now())
                .build();

        commentRepository.save(comment);
    }

    public void deleteComment(CommentDeleteDTO dto, User user) {
        Comment comment = commentRepository.findById(dto.getCommentIdx())
                .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다."));

        if (!comment.getWriteby().getUserid().equals(user.getUserid())) {
            throw new SecurityException("본인의 댓글만 삭제할 수 있습니다.");
        }

        commentRepository.delete(comment);
    }
}