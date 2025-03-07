package network.packets;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter @Setter
public class StatusPacket extends Packet {
    private String message;
}