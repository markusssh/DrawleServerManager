package dev.markusssh.drawleservermanager.dtos;

public record JoinLobbyResponse(
        String jwt,
        ServerConnectionData serverConnectionData
) {
}
