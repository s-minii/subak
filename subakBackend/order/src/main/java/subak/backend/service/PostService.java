package subak.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import subak.backend.domain.Heart;
import subak.backend.domain.Member;
import subak.backend.domain.Post;
import subak.backend.domain.PostImage;
import subak.backend.domain.enumType.PostStatus;
import subak.backend.domain.enumType.ProductStatus;
import subak.backend.dto.request.post.CreatePostRequest;
import subak.backend.dto.request.post.UpdatePostRequest;
import subak.backend.dto.response.comment.CommentResponse;
import subak.backend.dto.response.post.MainResponse;
import subak.backend.dto.response.post.PostDetailResponse;
import subak.backend.exception.PostException;
import subak.backend.repository.HeartRepository;
import subak.backend.repository.PostRepository;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Service
@Slf4j
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final FileUploadService fileUploadService;
    private final HeartRepository heartRepository;


    /**
     * 메인페이지 글 보기
     */
    public List<MainResponse> getMainPosts(int offset, int limit) {
        Pageable pageable = PageRequest.of(offset / limit, limit, Sort.by(Sort.Direction.DESC, "postDateTime"));
        Page<Post> posts = postRepository.findAll(pageable);
        return posts.stream()
                .map(this::convertToMainResponse)
                .collect(Collectors.toList());
    }

    /**
     * 글 상세보기
     */
    public PostDetailResponse getPostDetail(Long postId) {
        Post post = getPostById(postId);
        return convertToPostDetailResponse(post);
    }


    /**
     * 끌어올리기
     */
    public void recentPost(Long postId){
        Post post = getPostById(postId);
        post.updatePostDateTime();
        postRepository.save(post);
    }


    /**
     * 좋아요 추가, 취소
     */
    public void addOrRemoveHeart(Long postId, Member member) {
        Post post = getPostById(postId);
        Heart alreadyHeart = null;
        // 이미 좋아요를 눌렀는지 확인
        for (Heart heart : post.getHearts()) {
            if (heart.getMember().equals(member)) {
                alreadyHeart = heart;
                break;
            }
        }
        if (alreadyHeart != null) {
            // 이미 좋아요를 눌렀다면 좋아요 취소
            heartRepository.delete(alreadyHeart);
        } else {
            // 아직 좋아요를 누르지 않았다면 좋아요 추가
            Heart heart = new Heart(member, post);
            heartRepository.save(heart);
        }
    }

    /**
     * 글 생성
     */
    public void createPost(CreatePostRequest createPostRequest, Member member, List<MultipartFile> images) throws IOException {

        // 이미지 파일이 없으면, imagePath에 저장하지 않는다.
        List<String> imagePaths = null;
        if (images != null && !images.isEmpty()) {
            imagePaths = new ArrayList<>();
            for (MultipartFile image : images) {
                String imagePath = fileUploadService.uploadImage(image);
                if (imagePath != null) {
                    imagePaths.add(imagePath);
                }
            }
        }

        Post post = Post.createPost(
                member,
                createPostRequest.getCategory(),
                createPostRequest.getPostTitle(),
                createPostRequest.getContent(),
                createPostRequest.getPrice(),
                imagePaths
        );
        postRepository.save(post);
    }

    /**
     * 글 수정
     */
    public void updatePost(Long postId, UpdatePostRequest updatePostRequest, List<MultipartFile> postImages) throws IOException {
        Post post = getPostById(postId);
        
        List<String> imagePaths = new ArrayList<>();
        if (postImages != null && !postImages.isEmpty()) {
            for (MultipartFile image : postImages) {
                String imagePath = fileUploadService.uploadImage(image);
                if (imagePath != null) {
                    imagePaths.add(imagePath);
                }
            }
        }

        post.updatePostInfo(
                updatePostRequest.getCategory(),
                updatePostRequest.getPostTitle(),
                updatePostRequest.getContent(),
                updatePostRequest.getPrice(),
                imagePaths);

        // 기존 이미지 삭제
        List<PostImage> oldPostImages = new ArrayList<>(post.getPostImages());
        for (PostImage oldPostImage : oldPostImages) {
            if (!imagePaths.contains(oldPostImage.getImagePath())) {
                fileUploadService.deleteImage(oldPostImage.getImagePath());
                post.getPostImages().remove(oldPostImage);
            }
        }

        postRepository.save(post);
    }

    /**
     * 상품 상태 수정 [SALE, RESERVATION, COMPLETE]
     */
    public void updateProductStatus(Long postId, ProductStatus productStatus) {
        Post post = getPostById(postId);

        post.updateProductStatus(productStatus);
        postRepository.save(post);
    }

    /**
     * 게시글 상태 수정 [BASIC, HIDE]
     */
    public void updatePostStatus(Long postId, PostStatus postStatus) {
        Post post = getPostById(postId);

        post.updatePostStatus(postStatus);
        postRepository.save(post);
    }


    /**
     * 글 삭제
     */
    public void deletePost(Long postId) {
        Post post = getPostById(postId);

        // 게시글에 연결된 이미지 삭제
        for (PostImage postImage : post.getPostImages()) {
            try {
                fileUploadService.deleteImage(postImage.getImagePath());
            } catch (IOException e) {
                throw new RuntimeException("Failed to delete image: " + postImage.getImagePath(), e);
            }
        }

        postRepository.deleteById(postId);
    }

    // 메인페이지
    private MainResponse convertToMainResponse(Post post){
        MainResponse response = new MainResponse();

        response.setId(post.getId());
        response.setMemberName(post.getMember().getName());
        response.setProfileImage(post.getMember().getProfileImage());
        response.setPostTitle(post.getPostTitle());
        response.setFirstImage(post.getPostImages().isEmpty() ? null : post.getPostImages().get(0).getImagePath());
        response.setPrice(post.getPrice());
        response.setPostDateTime(post.getPostDateTime());
        response.setAddress(post.getMember().getAddress());
        response.setHeartCount(post.getHearts().size());
        response.setCommentCount(post.getComments().size());
        return response;
    }


    // 상세페이지
    private PostDetailResponse convertToPostDetailResponse(Post post) {
        PostDetailResponse response = new PostDetailResponse();
        response.setId(post.getId());
        response.setPostTitle(post.getPostTitle());
        response.setContent(post.getContent());
        response.setMemberName(post.getMember().getName());
        response.setProfileImage(post.getMember().getProfileImage());
        response.setPostImages(post.getPostImages().stream().map(PostImage::getImagePath).collect(Collectors.toList()));
        response.setPrice(post.getPrice());
        response.setPostDateTime(post.getPostDateTime());
        response.setAddress(post.getMember().getAddress());
        response.setHeartCount(post.getHearts().size());
        response.setCommentCount(post.getComments().size());
        response.setComments(post.getComments().stream()
                .map(comment -> new CommentResponse(
                        comment.getId(),
                        comment.getMember().getName(),
                        comment.getContent(),
                        comment.getCmDateTime(),
                        comment.getMember().getProfileImage()))
                .collect(Collectors.toList()));
        return response;
    }

    // 게시물이 존재하지 않는 경우 예외처리
    public Post getPostById(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new PostException.PostNotFoundException("존재하지 않는 게시글입니다."));
    }


}
