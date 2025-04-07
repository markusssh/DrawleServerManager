package dev.markusssh.drawleservermanager.dtos;

import lombok.Getter;

public record JwtValidationResult(
        @Getter
        boolean valid,
        String errorMessage,
        Long playerId,
        Long lobbyId,
        String playerName
) {
    public static JwtValidationResult valid(Long playerId, Long lobbyId, String playerName) {
        return new JwtValidationResult(true, null, playerId, lobbyId, playerName);
    }

    public static JwtValidationResult invalid(String errorMessage) {
        return new JwtValidationResult(false, errorMessage, null, null, null);
    }
}
