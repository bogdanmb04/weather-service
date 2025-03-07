package network;

import com.google.gson.*;
import network.enums.Command;
import network.packets.Packet;
import network.packets.StatusPacket;
import network.packets.UserPacket;
import network.packets.WeatherPacket;

import java.lang.reflect.Type;

public class CommandBasedDeserializer implements JsonDeserializer<Packet> {
    @Override
    public Packet deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        String commandStr = jsonObject.get("command").getAsString();
        Command command = Command.valueOf(commandStr);

        switch (command) {
            case LOGIN, AUTH:
                return new Gson().fromJson(jsonObject, UserPacket.class);
            case REQUEST:
                return new Gson().fromJson(jsonObject, WeatherPacket.class);
            case MESSAGE:
                return new Gson().fromJson(jsonObject, StatusPacket.class);
            default:
                throw new JsonParseException("Unknown command: " + commandStr);
        }
    }
}
