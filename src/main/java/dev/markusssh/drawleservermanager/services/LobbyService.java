package dev.markusssh.drawleservermanager.services;

import dev.markusssh.drawleservermanager.dtos.JoinLobbyRequest;
import dev.markusssh.drawleservermanager.dtos.JoinLobbyResponse;
import dev.markusssh.drawleservermanager.dtos.JwtBody;
import dev.markusssh.drawleservermanager.dtos.RegisterLobbyRequest;
import dev.markusssh.drawleservermanager.redis.entity.Lobby;
import dev.markusssh.drawleservermanager.redis.entity.Player;
import dev.markusssh.drawleservermanager.redis.repository.LobbyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class LobbyService {

    private static final String LOBBY_ID_SEQUENCE = "lobby:id:sequence";

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

    public JoinLobbyResponse createLobby(RegisterLobbyRequest req) {
        Long lobbyId = redisTemplate.opsForValue().increment(LOBBY_ID_SEQUENCE);

        Player player = playerService.createPlayer(req.creatorName(), lobbyId);
        Long playerId = player.getId();

        Lobby lobby = new Lobby();
        lobby.setId(lobbyId);
        lobby.setPlayTime(validatePlayTime(req.playTime()));
        lobby.setMaxPlayers(validateMaxPlayers(req.maxPlayers()));
        lobby.setCreatorId(playerId);
        lobbyRepository.save(lobby);

        var serverConData = ServerManagerService.getConnectionData();
        var jwt = new JwtBody(playerId, lobbyId);

        return new JoinLobbyResponse(jwt, serverConData);
    }

    private int validatePlayTime(int time) {
        return Math.clamp(time, Lobby.MIN_PLAY_TIME, Lobby.MAX_PLAY_TIME);
    }

    private int validateMaxPlayers(int time) {
        return Math.clamp(time, Lobby.MIN_PLAYERS, Lobby.MAX_PLAYERS);
    }

    public JoinLobbyResponse joinLobby(JoinLobbyRequest req) {
        Long lobbyId = req.lobbyId();

        var lobbyOptional = lobbyRepository.findById(lobbyId);
        if (lobbyOptional.isEmpty()) {
            return null;
        }
        var lobby = lobbyOptional.get();

        //Такая логика не поддерживает переподключения :(
        var player = playerService.createPlayer(req.playerName(), lobbyId);

        var serverConData = ServerManagerService.getConnectionData();
        var jwt = new JwtBody(player.getId(), lobby.getId());
        return new JoinLobbyResponse(jwt, serverConData);
    }

    public Lobby getLobbyById(Long lobbyId) {
        return lobbyRepository.findById(lobbyId).orElse(null);
    }

    public void deleteLobbyById(Long lobbyId) {
        lobbyRepository.deleteById(lobbyId);
    }
}
