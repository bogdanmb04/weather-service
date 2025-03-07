package database.entities;

import javax.persistence.*;

import database.enums.WeatherType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "city_weather", schema = "public")
public class CityWeather {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "temperature", nullable = false)
    private Double temperature;

    @Enumerated(EnumType.STRING)
    @Column(name = "weather", nullable = false)
    private WeatherType weather;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CityWeather that = (CityWeather) o;
        return Objects.equals(id, that.id) || (Objects.equals(city, that.city) && Objects.equals(date, that.date));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, city, date);
    }
}