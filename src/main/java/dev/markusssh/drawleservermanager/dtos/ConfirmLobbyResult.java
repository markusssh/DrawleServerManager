package dev.markusssh.drawleservermanager.dtos;

public record ConfirmLobbyResult(
        Long lobbyId,
        int playTime,
        int maxPlayers
) {
}
