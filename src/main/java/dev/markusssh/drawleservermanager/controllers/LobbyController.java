package dev.markusssh.drawleservermanager.controllers;

import dev.markusssh.drawleservermanager.dtos.JoinLobbyRequest;
import dev.markusssh.drawleservermanager.dtos.RegisterLobbyRequest;
import dev.markusssh.drawleservermanager.dtos.ServerVerification;
import dev.markusssh.drawleservermanager.services.LobbyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("lobby")
public class LobbyController {

    private final LobbyService lobbyService;

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
        var res = lobbyService.joinLobby(req);
        if (res == null) { return ResponseEntity.badRequest().build(); }
        return ResponseEntity.ok(res);
    }

    @GetMapping
    private ResponseEntity<?> getLobby(@RequestParam long lobbyId, @RequestBody ServerVerification verification) {
        if (verificationFailed(verification)) return ResponseEntity.badRequest().build();
        var res = lobbyService.getLobbyById(lobbyId);
        if (res == null) { return ResponseEntity.notFound().build(); }
        return ResponseEntity.ok(res);
    }

    @DeleteMapping
    private ResponseEntity<?> deleteLobby(@RequestParam long lobbyId, @RequestBody ServerVerification verification) {
        if (verificationFailed(verification)) return ResponseEntity.badRequest().build();
        lobbyService.deleteLobbyById(lobbyId);
        return ResponseEntity.ok().build();
    }

    private boolean verificationFailed(ServerVerification verification) {
        //PLACEHOLDER
        return !verification.token().equals("1234");
    }
}

