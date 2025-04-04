package dev.markusssh.drawleservermanager.redis.repository;

import dev.markusssh.drawleservermanager.redis.entity.Player;
import org.springframework.data.repository.CrudRepository;

public interface PlayerRepository extends CrudRepository<Player, Long> {
}
