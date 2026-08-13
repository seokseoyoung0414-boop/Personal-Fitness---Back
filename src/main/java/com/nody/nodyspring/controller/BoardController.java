package com.nody.nodyspring.controller;

import com.nody.nodyspring.DB.Board;
import com.nody.nodyspring.DB.Image;
import com.nody.nodyspring.DB.User;
import com.nody.nodyspring.config.JwtConfig;
import com.nody.nodyspring.dto.BoardDTO.BoardDTO;
import com.nody.nodyspring.dto.CommentDTO.CommentCreateDTO;
import com.nody.nodyspring.dto.CommentDTO.CommentDTO;
import com.nody.nodyspring.dto.CommentDTO.CommentDeleteDTO;
import com.nody.nodyspring.repository.ImageRepository;
import com.nody.nodyspring.service.BoardService;
import com.nody.nodyspring.service.CommentService;
import com.nody.nodyspring.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/post")
public class BoardController {

    @Autowired
    private BoardService boardService;
    @Autowired
    private JwtConfig jwtConfig;
    @Autowired
    private CommentService commentService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private ImageRepository imageRepo;

    public BoardController(BoardService boardService,
                           JwtUtil jwtUtil,
                           ImageRepository imageRepo) {
        this.boardService = boardService;
        this.jwtUtil = jwtUtil;
        this.imageRepo = imageRepo;
    }

    @GetMapping("/list")
    public List<BoardDTO> getAllPosts() {
        return boardService.getAllPosts();
    }

    @GetMapping("/mine")
    public ResponseEntity<?> getMyPosts(HttpServletRequest request) {
        // JWT 토큰 쿠키 추출
        String token = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("token".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        if (token == null) {
            return ResponseEntity.status(401).body("토큰이 없습니다.");
        }

        String userid = jwtConfig.getUserIdFromToken(token);
        List<BoardDTO> posts = boardService.getMyPosts(userid);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/view/{idx}")
    public ResponseEntity<?> getBoardByIdx(@PathVariable Integer idx,
                                           HttpServletRequest request) {

        String ip = extractClientIp(request);          // ⭐ IP 추출
        BoardDTO dto = boardService.getBoardByIdx(idx, ip);

        return ResponseEntity.ok(dto);
    }

    /** 프록시, 로컬 테스트 모두 대응 */
    private String extractClientIp(HttpServletRequest req) {
        String xf = req.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isEmpty()) {
            return xf.split(",")[0].trim();            // 첫 번째 IP
        }
        return req.getRemoteAddr();                    // 직접 접속
    }

    @GetMapping("/comment/{boardIdx}")
    public ResponseEntity<List<CommentDTO>> getComments(@PathVariable Integer boardIdx) {
        List<CommentDTO> commentList = commentService.getCommentsByBoardIdx(boardIdx);
        return ResponseEntity.ok(commentList);
    }

    @PostMapping("/comment")
    public ResponseEntity<?> addComment(@RequestBody CommentCreateDTO dto,
                                        @CookieValue(name = "token", required = false) String token) {
        try {
            User user = jwtUtil.validateToken(token);
            commentService.addComment(user, dto);
            return ResponseEntity.ok("댓글 작성 완료");
        } catch (Exception e) {
            return ResponseEntity.status(403).body("인증 실패: " + e.getMessage());
        }
    }

    // ✅ 댓글 삭제
    @PostMapping("/commentdel")
    public ResponseEntity<?> deleteComment(@RequestBody CommentDeleteDTO dto,
                                           @CookieValue(name = "token", required = false) String token) {
        try {
            User user = jwtUtil.validateToken(token);
            commentService.deleteComment(dto, user);
            return ResponseEntity.ok("댓글 삭제 완료");
        } catch (Exception e) {
            return ResponseEntity.status(403).body("인증 실패: " + e.getMessage());
        }
    }

    @PostMapping("/make")
    public ResponseEntity<?> createPost(@RequestBody BoardDTO dto,
                                        @CookieValue(name = "token", required = false) String token) {
        try {
            User user = jwtUtil.validateToken(token);
            dto.setWriter(user.getUserid());  // 보안용으로 로그인한 userid 덮어쓰기
            Board created = boardService.createBoard(dto, user);
            return ResponseEntity.ok(Map.of("idx", created.getIdx()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("작성 실패: " + e.getMessage());
        }
    }

    // 게시글 수정
    @PostMapping("/edit/{idx}")
    public ResponseEntity<?> editPost(@PathVariable Integer idx,
                                      @RequestBody BoardDTO dto,
                                      @CookieValue(name = "token", required = false) String token) {
        try {
            User user = jwtUtil.validateToken(token);
            boardService.editBoard(idx, dto, user);
            return ResponseEntity.ok(Map.of("idx", idx));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("수정 실패: " + e.getMessage());
        }
    }

    @PostMapping("/del")
    public ResponseEntity<?> deletePost(@RequestBody Map<String, Integer> payload,
                                        @CookieValue(name = "token", required = false) String token) {
        try {
            User user = jwtUtil.validateToken(token);
            Integer idx = payload.get("idx");
            boardService.deleteBoard(idx, user);          // 이것만 있으면 충분
            return ResponseEntity.ok("삭제 성공");
        } catch (Exception e) {
            return ResponseEntity.status(403).body("삭제 실패: " + e.getMessage());
        }
    }

    @PostMapping(value = "/img/{boardIdx}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImages(@PathVariable Integer boardIdx,
                                          @RequestParam("files") List<MultipartFile> files,
                                          @CookieValue(name="token", required=false) String token) {
        try {
            jwtUtil.validateToken(token);

            for (MultipartFile f : files) {
                Image img = new Image();
                img.setBoardimg(boardIdx);
                img.setContentType(f.getContentType());  // MIME 타입 저장
                img.setImageData(f.getBytes());
                imageRepo.save(img);
            }

            return ResponseEntity.ok("이미지 업로드 완료");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(403).body("업로드 실패: " + e.getMessage());
        }
    }

    @GetMapping("/img/file/{imgIdx}")
    public ResponseEntity<byte[]> serveImage(@PathVariable Integer imgIdx) {
        Image img = imageRepo.findById(imgIdx)
                .orElseThrow(() -> new RuntimeException("이미지를 찾을 수 없습니다."));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, img.getContentType())  // 동적 MIME 설정
                .body(img.getImageData());
    }

    @PostMapping("/img/delete")
    public ResponseEntity<?> deleteImages(@RequestBody List<Integer> imgIdxList,
                                          @CookieValue(name="token",required=false) String token){
        try {
            jwtUtil.validateToken(token);
            imageRepo.deleteAllById(imgIdxList);   // 파일 삭제 로직 전부 제거
            return ResponseEntity.ok("삭제 완료");
        } catch (Exception e){
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }
    @GetMapping("/img/{boardIdx}")
    public ResponseEntity<List<Integer>> getBoardImages(@PathVariable Integer boardIdx) {
        // 경로 대신 이미지 PK만 넘김
        List<Integer> ids = imageRepo.findByBoardimg(boardIdx)
                .stream().map(Image::getIdx).toList();
        return ResponseEntity.ok(ids);
    }
}
