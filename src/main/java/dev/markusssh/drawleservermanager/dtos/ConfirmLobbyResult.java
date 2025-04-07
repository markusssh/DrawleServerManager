package dev.markusssh.drawleservermanager.dtos;

public record ConfirmLobbyResult(
        Long lobbyId,
        Long creatorId,
        int playTime,
        int maxPlayers
) {
}
