package dev.markusssh.drawleservermanager.dtos;

public record JoinLobbyResponse(
        //TODO: финальный вариант -> String jwt (шифровать)
        JwtBody jwt,
        ServerConnectionData serverConnectionData
) {
}
