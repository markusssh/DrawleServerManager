package dev.markusssh.drawleservermanager.dtos;

public record JoinLobbyRequest(
        String playerName,
        Long lobbyId
) {
}
