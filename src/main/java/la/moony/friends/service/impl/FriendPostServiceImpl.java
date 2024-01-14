package la.moony.friends.service.impl;


import la.moony.friends.extension.Friend;
import la.moony.friends.extension.FriendPost;
import la.moony.friends.finders.FriendFinder;
import la.moony.friends.service.FriendPostService;
import la.moony.friends.util.RSSParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.content.Post;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Component
@Slf4j
public class FriendPostServiceImpl implements FriendPostService {


    private final ReactiveExtensionClient  client;

    private final FriendFinder friendFinder;


    private final int pageSize = 20;

    public FriendPostServiceImpl(FriendFinder friendFinder , ReactiveExtensionClient client) {
        this.client = client;
        this.friendFinder = friendFinder;
    }


    @Override
    public void synchronizationFriend() {
        RSSParser rssParser = new RSSParser();

        Predicate<Friend> paramPredicate = post -> true;
        Mono<ListResult<Friend>> listResult = client.list(Friend.class, paramPredicate, null, 1, pageSize);
        listResult.subscribe(friends -> {
            //分页导出数据
            //分页获取并处理
            for (int i = 1; i <= friends.getTotalPages(); i++) {
                Mono<ListResult<Friend>> friendsPage = client.list(Friend.class, paramPredicate, null, i, pageSize);
                friendsPage.subscribe(friend -> {
                    friend.getItems().forEach(f->{
                        try {
                            Map<String, Object> data = rssParser.data(f.getSpec().getRssUrl());
                            String author = (String)data.get("author");
                            String channelLink = (String)data.get("channelLink");
                            String channelDescription = (String) data.get("channelDescription");
                            f.getSpec().setLink(channelLink);
                            f.getSpec().setDescription(channelDescription);
                            f.getSpec().setDisplayName(author);
                            //保存帖子数据
                            List<FriendPost> friendPostList = (List<FriendPost>) data.get("friendPostList");
                            if (friendPostList.size()>0){
                                //删除之前数据
                                client.list(FriendPost.class,
                                        friendPost -> StringUtils.equals(friendPost.getSpec().getUrl(), channelLink), null)
                                    .flatMap(client::delete).subscribe();
                                friendPostList.forEach(post -> {
                                    FriendPost.Spec spec = post.getSpec();
                                    // 设置元数据才能保存
                                    FriendPost friendPost = new FriendPost();
                                    friendPost.setMetadata(new Metadata());
                                    friendPost.getMetadata().setGenerateName("friendPost-");
                                    spec.setLogo(f.getSpec().getLogo());
                                    friendPost.setSpec(spec);
                                    client.create(friendPost).subscribe();
                                });
                                f.getSpec().setStatus(1);
                                f.getSpec().setPullTime(new Date());
                                client.update(f).subscribe();
                            }

                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
                });
            }
        });



    }
}
