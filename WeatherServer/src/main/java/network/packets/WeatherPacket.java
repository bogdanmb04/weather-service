package network.packets;

import database.enums.WeatherType;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@SuperBuilder
@Getter @Setter
public class WeatherPacket extends Packet {
    private String cityName;
    private WeatherType weatherType;
    private Double temperature;
    private Double latitude;
    private Double longitude;
    private LocalDate date;
}
