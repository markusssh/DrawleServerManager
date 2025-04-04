package dev.markusssh.drawleservermanager.dtos;

public record RegisterLobbyRequest(
        String creatorName,
        int maxPlayers,
        int playTime
) {
}
