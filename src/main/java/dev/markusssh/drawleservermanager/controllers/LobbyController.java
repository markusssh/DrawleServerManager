package dev.markusssh.drawleservermanager.controllers;

import dev.markusssh.drawleservermanager.dtos.JoinLobbyRequest;
import dev.markusssh.drawleservermanager.dtos.RegisterLobbyRequest;
import dev.markusssh.drawleservermanager.services.LobbyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
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
    private ResponseEntity<?> registerLobby(
            @RequestBody RegisterLobbyRequest req,
            HttpServletRequest servletRequest) {
        String clientIp = getClientIp(servletRequest);
        try {
            var res = lobbyService.createLobby(req, clientIp);
            return ResponseEntity.ok(res);
        } catch (RuntimeException e) {
            log.warn("To many requests from {}", clientIp);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(e.getMessage());
        }
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

    //Первый ip = clientIp
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

}

