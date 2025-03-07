package network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import database.dao.AppUserDao;
import database.dao.CityDao;
import database.dao.WeatherDao;
import database.entities.AppUser;
import javafx.util.Pair;
import network.packets.Packet;
import network.packets.UserPacket;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import network.enums.*;
import network.packets.*;
import database.entities.*;
import database.enums.*;

public class ClientThread extends Thread {
    private final Socket socket;
    static private final Integer ACCEPTABLE_DISTANCE = 200;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    public ClientThread(Socket acceptedClient) {
        try {
            this.socket = acceptedClient;
            this.out = new ObjectOutputStream(this.socket.getOutputStream());
            this.in = new ObjectInputStream(this.socket.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        try {
            while(true) {
                String networkJson = (String) this.in.readObject();
                Packet packet = new Gson().fromJson(networkJson, Packet.class);
                switch (packet.getCommand()) {
                    case AUTH -> authScenario(networkJson);
                    case LOGIN -> loginScenario(networkJson);
                    case REQUEST -> requestScenario(networkJson);
                    case UPLOAD -> uploadScenario(networkJson);
                    default -> System.out.println("Unknown command");
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    //https://community.fabric.microsoft.com/t5/Desktop/How-to-calculate-lat-long-distance/td-p/1488227
    private static Double getDistance(Pair<Double, Double> source, Pair<Double, Double> target) {
        final int EARTH_RADIUS = 6371;
        Double latFirst = source.getKey();
        Double longFirst = source.getValue();
        Double latSecond = target.getKey();
        Double longSecond = target.getValue();

        return Math.acos(Math.sin(Math.toRadians(latFirst)) * Math.sin(Math.toRadians(latSecond))
                + Math.cos(Math.toRadians(latFirst)) * Math.cos(Math.toRadians(latSecond)) * Math.cos(Math.toRadians(longSecond)
                - Math.toRadians(longFirst))) * EARTH_RADIUS;
    }

    private void authScenario(String networkJson) {
        UserPacket userPacket = new Gson().fromJson(networkJson, UserPacket.class);
        AppUserDao appUserDao = new AppUserDao();
        Optional<AppUser> user = appUserDao.find(userPacket.getUsername());
        if (user.isPresent()) {
            StatusPacket response = StatusPacket.builder().message("User already exists. Please login if this is you.").command(Command.MESSAGE).build();
            try {
                this.out.writeObject(new Gson().toJson(response));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            UserPacket response;
            AppUser appUser = new AppUser();
            appUser.setUsername(userPacket.getUsername());
            appUser.setPassword(userPacket.getPassword());
            appUser.setRole(Role.USER);
            appUserDao.create(appUser);
            userPacket.setRole(Role.USER);
            response = UserPacket.builder().command(Command.AUTH).build();
            try {
                this.out.writeObject(new Gson().toJson(response));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void loginScenario(String networkJson) {
        UserPacket networkUser = new Gson().fromJson(networkJson, UserPacket.class);
        AppUserDao appUserDao = new AppUserDao();
        Optional<AppUser> user = appUserDao.find(networkUser.getUsername());
        if(user.isPresent() && user.get().getPassword().equals(networkUser.getPassword())) {
            networkUser.setRole(user.get().getRole());
            try {
                this.out.writeObject(new Gson().toJson(networkUser));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            StatusPacket statusPacket = StatusPacket.builder()
                    .message("Incorrect credentials")
                    .command(Command.MESSAGE)
                    .build();
            try {
                this.out.writeObject(new Gson().toJson(statusPacket));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void requestScenario(String networkJson) {
        ZoneId zonedId = ZoneId.of( "Europe/Paris" ); //https://www.geeksforgeeks.org/java-time-zoneid-class-in-java/
        LocalDate today = LocalDate.now( zonedId );
        LocationPacket location = new Gson().fromJson(networkJson, LocationPacket.class);
        WeatherDao weatherDao = new WeatherDao();
        AppUserDao appUserDao = new AppUserDao();
        Optional<AppUser> appUserOpt = appUserDao.find(location.getUsername());

        if(appUserOpt.isPresent()) {
            AppUser appUser = appUserOpt.get();
            appUser.setLatitude(location.getLatitude());
            appUser.setLongitude(location.getLongitude());
            appUserDao.update(appUser);
        }
        Stream<CityWeather> weathers = weatherDao.getAllWeathers();
        //User sent in location name as well as coords
        AtomicReference<Double> minDistance = new AtomicReference<>(10000000.0);
        AtomicReference<Long> cityID = new AtomicReference<>(null);
        Pair<Double, Double> userCoords = new Pair<Double, Double>(location.getLatitude(), location.getLongitude());
        weathers.forEach(weather -> {
            Pair<Double, Double> cityCoords = new Pair<>(weather.getCity().getLatitude(), weather.getCity().getLongitude());
            Double distance = getDistance(cityCoords, userCoords);
            if(distance < minDistance.get()) {
                minDistance.set(distance);
                cityID.set(weather.getCity().getId());
            }
        });

        if(minDistance.get() <= ACCEPTABLE_DISTANCE) {
            CityDao cityDao = new CityDao();
            Optional<City> city = cityDao.getCityByID(cityID.get());
            if(city.isPresent()) {
                Stream<CityWeather> cityWeathers = weatherDao.getAllWeathers();
                Optional<CityWeather> weatherOpt = cityWeathers.filter(weather -> {
                    return weather.getCity().getId().equals(city.get().getId()) && weather.getDate().equals(today);
                }).findFirst();
                if(weatherOpt.isPresent()) {
                    Double temp = weatherOpt.get().getTemperature();
                    WeatherType type = weatherOpt.get().getWeather();
                    WeatherPacket response = WeatherPacket.builder()
                            .cityName(city.get().getName())
                            .latitude(city.get().getLatitude())
                            .longitude(city.get().getLongitude())
                            .temperature(temp)
                            .weatherType(type)
                            .date(today)
                            .command(Command.WEATHER_RESPONSE)
                            .build();
                    String typeOfWeather = type.toString();
                    StatusPacket messageResponse = StatusPacket.builder()
                            .message(today.toString() + " there are " + temp + " degrees in " + city.get().getName() + ". The weather is : " + typeOfWeather)
                            .command(Command.MESSAGE)
                            .build();
                    try {
                        this.out.writeObject(new Gson().toJson(messageResponse));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                else {
                    StatusPacket packet = StatusPacket.builder()
                            .message("Cannot retrieve any weather for " + city.get().getName())
                            .build();
                    try {
                        this.out.writeObject(new Gson().toJson(packet));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        } else {
            StatusPacket statusPacket = StatusPacket.builder()
                    .message("Cannot return accurate weather results")
                    .command(Command.MESSAGE)
                    .build();
            try {
                this.out.writeObject(new Gson().toJson(statusPacket));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void uploadScenario(String networkJson) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .create();
        WeatherPacket weatherPacket = gson.fromJson(networkJson, WeatherPacket.class);
        WeatherDao weatherDao = new WeatherDao();
        CityDao cityDao = new CityDao();
        Stream<City> cities = cityDao.getAllCities();
        Optional<City> cityOpt = cities.filter(city -> {
            return city.getLongitude().equals(weatherPacket.getLongitude()) && city.getLatitude().equals(weatherPacket.getLatitude());
        }).findFirst();
        if(cityOpt.isPresent()) {
            CityWeather weather = new CityWeather();

            City city = cityOpt.get();
            weather.setCity(city);
            weather.setWeather(weatherPacket.getWeatherType());
            weather.setDate(weatherPacket.getDate());
            weather.setTemperature(weatherPacket.getTemperature());
            weatherDao.create(weather);

            StatusPacket statusPacket = StatusPacket.builder().command(Command.MESSAGE).message("Successfully uploaded given data!").build();
            try {
                this.out.writeObject(new Gson().toJson(statusPacket));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            StatusPacket statusPacket = StatusPacket.builder().command(Command.MESSAGE).message("Cannot upload weather data for a non-existent city").build();
            try {
                this.out.writeObject(new Gson().toJson(statusPacket));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
