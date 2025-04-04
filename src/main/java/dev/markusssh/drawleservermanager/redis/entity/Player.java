package dev.markusssh.drawleservermanager.redis.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

@RedisHash("player")
@Getter
@Setter
public class Player {
    @Id
    private Long id;
    private String name;
    @Indexed
    private Long lobbyId;
}
