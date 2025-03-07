package network.packets;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import database.enums.*;

@SuperBuilder
@Getter @Setter
public class UserPacket  extends Packet {
    private String username;
    private String password;
    private Role role;
}
