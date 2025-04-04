package dev.markusssh.drawleservermanager.services;

import dev.markusssh.drawleservermanager.dtos.ServerConnectionData;
import org.springframework.stereotype.Service;

//PLACEHOLDER SERVICE
@Service
public class ServerManagerService {

    public static ServerConnectionData getConnectionData() {
        return new ServerConnectionData("127.0.0.1", 8081);
    }

}
