package dev.markusssh.drawleservermanager.redis.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@RedisHash("lobby")
@Getter
@Setter
public class Lobby {
    public static final int MIN_PLAY_TIME = 10;
    public static final int MAX_PLAY_TIME = 300;
    public static final int MIN_PLAYERS = 3;
    public static final int MAX_PLAYERS = 10;

    @Id
    private Long id;
    private int maxPlayers;
    private int playTime;
    private Long creatorId;
}
