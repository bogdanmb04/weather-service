package network.packets;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter @Setter
public class LocationPacket extends Packet{
    private Double latitude;
    private Double longitude;
    private String username;
}
