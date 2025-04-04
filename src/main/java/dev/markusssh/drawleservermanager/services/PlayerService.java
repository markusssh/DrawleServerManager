package dev.markusssh.drawleservermanager.services;

import dev.markusssh.drawleservermanager.redis.entity.Player;
import dev.markusssh.drawleservermanager.redis.repository.PlayerRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PlayerService {
    private static final String PLAYER_ID_SEQUENCE = "player:id:sequence";

    private final PlayerRepository playerRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public PlayerService(PlayerRepository playerRepository, RedisTemplate<String, Object> redisTemplate) {
        this.playerRepository = playerRepository;
        this.redisTemplate = redisTemplate;
    }

    public Player createPlayer(String name, Long lobbyId) {
        Player player = new Player();

        Long playerId = redisTemplate.opsForValue().increment(PLAYER_ID_SEQUENCE);
        player.setId(playerId);
        player.setName(name);
        player.setLobbyId(lobbyId);
        return playerRepository.save(player);
    }

    public Optional<Player> getPlayerById(Long playerId) {
        return playerRepository.findById(playerId);
    }
}
