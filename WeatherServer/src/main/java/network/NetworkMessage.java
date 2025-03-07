package network;

import java.io.Serializable;
import network.enums.*;

public class NetworkMessage implements Serializable {
    private String message;
    private Command command;

    public NetworkMessage(String message, Command command) {
        this.message = message;
        this.command = command;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Command getCommand() {
        return command;
    }

    public void setCommand(Command command) {
        this.command = command;
    }
}

