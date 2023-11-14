package subak.backend.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import subak.backend.domain.enumType.PostStatus;
import subak.backend.domain.enumType.ProductStatus;

import javax.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static javax.persistence.FetchType.*;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {

    @Id @GeneratedValue
    @Column(name = "post_id")
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    private String category;

    @Column(name = "post_title")
    private String postTitle; //글 제목

    private int price;

    private int likes; // 즐겨찾기

    @Column(name = "post_date_time")
    private LocalDateTime postDateTime; // 글 게시 시간

    private int views; // 조회수

    @Column(name = "post_image")
    private String postImage;

    @Enumerated(EnumType.STRING)
    private ProductStatus productStatus; // 상품 상태 [SALE, RESERVATION, COMPLETE]

    @Enumerated(EnumType.STRING)
    private PostStatus postStatus; // 게시글 상태 [BASIC, HIDE]

    @OneToMany(mappedBy = "member")
    private List<Heart> hearts = new ArrayList<>();

    @OneToMany(mappedBy = "member")
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "member")
    private List<Review> reviews = new ArrayList<>();






}
