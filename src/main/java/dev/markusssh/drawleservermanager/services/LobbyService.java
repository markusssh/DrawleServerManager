package dev.markusssh.drawleservermanager.services;

import dev.markusssh.drawleservermanager.dtos.JoinLobbyRequest;
import dev.markusssh.drawleservermanager.dtos.JoinLobbyResponse;
import dev.markusssh.drawleservermanager.dtos.RegisterLobbyRequest;
import dev.markusssh.drawleservermanager.redis.entity.Lobby;
import dev.markusssh.drawleservermanager.redis.entity.Player;
import dev.markusssh.drawleservermanager.redis.repository.LobbyRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class LobbyService {

    private static final String LOBBY_ID_SEQUENCE = "lobby:id:sequence";

    @Value("${jwt.secret}")
    private String jwtSecret;
    private final SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    private final PlayerService playerService;
    private final LobbyRepository lobbyRepository;
    private final RedisTemplate<String, Object> redisTemplate;


    @Autowired
    public LobbyService(
            PlayerService playerService,
            LobbyRepository lobbyRepository,
            RedisTemplate<String, Object> redisTemplate) {
        this.playerService = playerService;
        this.lobbyRepository = lobbyRepository;
        this.redisTemplate = redisTemplate;
    }

    private String generateJwtToken(Long playerId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("playerId", playerId);

        return Jwts.builder()
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(key)
                .compact();
    }

    public JoinLobbyResponse createLobby(RegisterLobbyRequest req) {
        Long lobbyId = redisTemplate.opsForValue().increment(LOBBY_ID_SEQUENCE);

        Player player = playerService.createPlayer(req.creatorName(), lobbyId);
        Long playerId = player.getId();

        Lobby lobby = new Lobby();
        lobby.setId(lobbyId);
        lobby.setPlayTime(validatePlayTime(req.playTime()));
        lobby.setMaxPlayers(validateMaxPlayers(req.maxPlayers()));
        lobby.setCreatorId(playerId);
        lobby.setPlayerCount(1);
        lobbyRepository.save(lobby);

        var serverConData = ServerManagerService.getConnectionData();
        String jwt = generateJwtToken(playerId);

        return new JoinLobbyResponse(jwt, serverConData);
    }

    private int validatePlayTime(int time) {
        return Math.clamp(time, Lobby.MIN_PLAY_TIME, Lobby.MAX_PLAY_TIME);
    }

    private int validateMaxPlayers(int time) {
        return Math.clamp(time, Lobby.MIN_PLAYERS, Lobby.MAX_PLAYERS);
    }

    public JoinLobbyResponse joinLobby(JoinLobbyRequest req) throws RuntimeException {
        Long lobbyId = req.lobbyId();

        var lobbyOptional = lobbyRepository.findById(lobbyId);
        if (lobbyOptional.isEmpty()) {
            throw new IllegalArgumentException("Invalid lobby id");
        }
        var lobby = lobbyOptional.get();

        int playerCount = lobby.getPlayerCount();
        if (playerCount < lobby.getMaxPlayers()) {
            lobby.setPlayerCount(lobby.getPlayerCount() + 1);
        } else {
            throw new RuntimeException("Lobby is full");
        }

        //Такая логика не поддерживает переподключения :(
        var player = playerService.createPlayer(req.playerName(), lobbyId);

        var serverConData = ServerManagerService.getConnectionData();
        String jwt = generateJwtToken(player.getId());

        return new JoinLobbyResponse(jwt, serverConData);
    }

    private Long parseJwtToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return ((Number) claims.get("playerId")).longValue();
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }

    public boolean validateJwtToken(String token) {
        try {
            Long playerId = parseJwtToken(token);
            if (playerId == null) { return false; }

            var playerOptional = playerService.getPlayerById(playerId);
            if (playerOptional.isEmpty()) { return false; }
            var player = playerOptional.get();

            var lobbyOptional = getLobbyById(player.getLobbyId());
            return lobbyOptional.isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    public Optional<Lobby> getLobbyById(Long lobbyId) {
        return lobbyRepository.findById(lobbyId);
    }

    public void deleteLobbyById(Long lobbyId) {
        lobbyRepository.deleteById(lobbyId);
    }
}
