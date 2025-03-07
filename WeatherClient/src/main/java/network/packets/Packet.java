package network.packets;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import network.enums.Command;

@SuperBuilder
@Getter
public class Packet {
    protected Command command;
    protected String type;
}
