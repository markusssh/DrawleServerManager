package dev.markusssh.drawleservermanager.services;

import dev.markusssh.drawleservermanager.dtos.*;
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
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class LobbyService {

    private static final String LOBBY_ID_SEQUENCE = "lobby:id:sequence";

    @Value("${jwt.secret}")
    private String jwtSecret;
    private final SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${lobby.pending.ttl:300}")
    private long pendingLobbyTtl;

    @Value("${lobby.active.ttl:7200}")
    private long activeLobbyTtl;

    @Value("${lobby.max-per-ip:5}")
    private int maxPendingLobbiesPerIp;

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

    public JoinLobbyResponse createLobby(RegisterLobbyRequest req, String clientIp) {
        String pendingLobbiesKey = "pending:lobbies:" + clientIp;
        Long pendingCount = redisTemplate.opsForValue().increment(pendingLobbiesKey, 1);

        if (pendingCount != null && pendingCount > maxPendingLobbiesPerIp) {
            redisTemplate.opsForValue().decrement(pendingLobbiesKey);
            throw new RuntimeException("Too many lobbies. Please try again later.");
        }

        redisTemplate.expire(pendingLobbiesKey, pendingLobbyTtl, TimeUnit.SECONDS);

        Long lobbyId = redisTemplate.opsForValue().increment(LOBBY_ID_SEQUENCE);

        Player player = playerService.createPlayer(req.creatorName(), lobbyId);
        Long playerId = player.getId();

        Lobby lobby = new Lobby();
        lobby.setId(lobbyId);
        lobby.setPlayTime(validatePlayTime(req.playTime()));
        lobby.setMaxPlayers(validateMaxPlayers(req.maxPlayers()));
        lobby.setCreatorId(playerId);
        lobby.setPlayerCount(1);
        lobby.setStatus(Lobby.Status.PENDING);
        lobby.setTtl(pendingLobbyTtl);
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

        if (lobby.getStatus() != Lobby.Status.ACTIVE) {
            throw new IllegalArgumentException("Lobby is not active yet");
        }

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

    public ConfirmLobbyResult confirmLobby(Long lobbyId, Long playerId) {
        var lobbyOptional = lobbyRepository.findById(lobbyId);
        if (lobbyOptional.isEmpty()) { return null; }

        var lobby = lobbyOptional.get();
        if (lobby.getCreatorId().equals(playerId)) {
            lobby.setStatus(Lobby.Status.ACTIVE);
            lobby.setTtl(activeLobbyTtl);
            lobbyRepository.save(lobby);

            return new ConfirmLobbyResult(
                    lobby.getId(),
                    lobby.getCreatorId(),
                    lobby.getPlayTime(),
                    lobby.getMaxPlayers());
        }

        return null;
    }

    public void closeLobby(Long lobbyId) {
        var lobbyOptional = lobbyRepository.findById(lobbyId);
        if (lobbyOptional.isPresent()) {
            var lobby = lobbyOptional.get();
            lobby.setStatus(Lobby.Status.CLOSED);
            lobby.setTtl(60L);
            lobbyRepository.save(lobby);
        }
    }

    public JwtValidationResult validateJwtToken(String token) {
        try {
            Long playerId = parseJwtToken(token);
            if (playerId == null) { return JwtValidationResult.invalid("PlayerId is null"); }

            var playerOptional = playerService.getPlayerById(playerId);
            if (playerOptional.isEmpty()) { return JwtValidationResult.invalid("Player not found"); }
            var player = playerOptional.get();

            var lobbyOptional = getLobbyById(player.getLobbyId());
            if (lobbyOptional.isEmpty()) { return JwtValidationResult.invalid("Lobby not found"); }
            var lobbyId = lobbyOptional.get().getId();

            return JwtValidationResult.valid(playerId, lobbyId, player.getName());
        } catch (Exception e) {
            return JwtValidationResult.invalid(e.getMessage());
        }
    }

    public Optional<Lobby> getLobbyById(Long lobbyId) {
        return lobbyRepository.findById(lobbyId);
    }
}
