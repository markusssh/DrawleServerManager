package dev.markusssh.drawleservermanager.controllers;

import dev.markusssh.drawleservermanager.dtos.JoinLobbyRequest;
import dev.markusssh.drawleservermanager.dtos.RegisterLobbyRequest;
import dev.markusssh.drawleservermanager.dtos.ServerVerification;
import dev.markusssh.drawleservermanager.services.LobbyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("lobby")
public class LobbyController {

    private final LobbyService lobbyService;

    @Value("${game.server.token}")
    private String token;

    @Autowired
    public LobbyController(
            LobbyService lobbyService) {
        this.lobbyService = lobbyService;
    }

    @PostMapping("new-lobby")
    private ResponseEntity<?> registerLobby(@RequestBody RegisterLobbyRequest req) {
        var res = lobbyService.createLobby(req);
        return ResponseEntity.ok(res);
    }

    @PostMapping("join-lobby")
    private ResponseEntity<?> joinLobby(@RequestBody JoinLobbyRequest req) {
        try {
            var res = lobbyService.joinLobby(req);
            return ResponseEntity.ok(res);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @GetMapping
    private ResponseEntity<?> getLobby(@RequestParam long lobbyId, @RequestBody ServerVerification verification) {
        if (verificationFailed(verification)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        var resOptional = lobbyService.getLobbyById(lobbyId);
        if (resOptional.isEmpty()) { return ResponseEntity.notFound().build(); }
        var res = resOptional.get();
        return ResponseEntity.ok(res);
    }

    @DeleteMapping
    private ResponseEntity<?> deleteLobby(@RequestParam long lobbyId, @RequestBody ServerVerification verification) {
        if (verificationFailed(verification)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        lobbyService.deleteLobbyById(lobbyId);
        return ResponseEntity.ok().build();
    }

    private boolean verificationFailed(ServerVerification verification) {
        return !verification.token().equals(token);
    }
}

