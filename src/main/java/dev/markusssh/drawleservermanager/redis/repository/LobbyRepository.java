package dev.markusssh.drawleservermanager.redis.repository;

import dev.markusssh.drawleservermanager.redis.entity.Lobby;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LobbyRepository extends CrudRepository<Lobby, Long> {
}
