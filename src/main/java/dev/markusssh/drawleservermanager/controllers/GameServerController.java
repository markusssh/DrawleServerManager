package dev.markusssh.drawleservermanager.controllers;

import dev.markusssh.drawleservermanager.dtos.ConfirmLobbyRequest;
import dev.markusssh.drawleservermanager.dtos.JwtValidationRequest;
import dev.markusssh.drawleservermanager.services.LobbyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("game-server")
public class GameServerController {

    private final LobbyService lobbyService;

    @Value("${game.server.token}")
    private String token;

    @Autowired
    public GameServerController(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
    }

    @PostMapping("auth")
    public ResponseEntity<?> authenticate(
            @RequestBody JwtValidationRequest req,
            @RequestHeader("Authorization") String authHeader) {
        if (invalidAuth(authHeader)) { return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); }

        var res = lobbyService.validateJwtToken(req.jwt());
        if (res.isValid()) {
            return ResponseEntity.status(HttpStatus.OK).body(res);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Lobby or player not found");
        }
    }

    @PostMapping("confirm-lobby")
    public ResponseEntity<?> confirmLobby(
            @RequestBody ConfirmLobbyRequest req,
            @RequestHeader("Authorization") String authHeader) {
        if (invalidAuth(authHeader)) { return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); }

        var res = lobbyService.confirmLobby(req.lobbyId(), req.playerId());
        if (res != null) {
            return ResponseEntity.status(HttpStatus.OK).body(res);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Lobby or player not found");
        }
    }

    @PostMapping("close-lobby")
    public ResponseEntity<?> closeLobby(
            @RequestParam Long lobbyId,
            @RequestHeader("Authorization") String authHeader) {
        if (invalidAuth(authHeader)) { return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); }

        lobbyService.closeLobby(lobbyId);
        return ResponseEntity.ok().build();
    }

    private boolean invalidAuth(String auth) {
        if (!auth.startsWith("Bearer ")) return true;
        String token = auth.substring(7);
        return !token.equals(this.token);
    }

}
