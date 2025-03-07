package network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import network.enums.Role;
import network.enums.Command;
import network.enums.WeatherType;
import network.packets.*;
import network.CommandBasedDeserializer;

import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Scanner;
import java.util.WeakHashMap;

public class Client {
    private final int PORT = 6543;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private volatile Boolean isRunning = true;
    private volatile Boolean isAuthenticated = false;
    private String username;
    private Role currentRole;

    public void start() throws IOException {
        this.socket = new Socket("localhost", 6543);
        this.out = new ObjectOutputStream(this.socket.getOutputStream());
        this.in = new ObjectInputStream(this.socket.getInputStream());

        /* Write to server thread */
        new Thread(() -> {
            while (this.isRunning) {
                if(!isAuthenticated) {
                    showAuthMenu();
                } else {
                    showMainMenu();
                }
            }
        }).start();

        /* Read from server thread */
        new Thread(() -> {
            try {
                while (this.isRunning) {
                    String fromServer = (String) this.in.readObject();
                    handleServerResponse(fromServer);
                }
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    private void showAuthMenu() {
        System.out.println("""
                1. Login;
                2. Register;""");
        Scanner scanner = new Scanner(System.in);
        String authOptions = scanner.nextLine().toLowerCase();

        switch (authOptions) {
            case "1", "login" -> loginMenu();
            case "2", "register" -> registerMenu();
        }
    }

    private void registerMenu() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Insert your username:");
        String username = scanner.nextLine();
        System.out.println("Insert your password:");
        String password = scanner.nextLine();

        UserPacket packet = UserPacket.builder()
                .username(username)
                .password(password)
                .command(Command.AUTH)
                .build();
        try {
            this.out.writeObject(new Gson().toJson(packet));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loginMenu() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Insert your username");
        String username = scanner.nextLine();
        System.out.println("Insert your password");
        String password = scanner.nextLine();

        UserPacket packet = UserPacket.builder()
                .username(username)
                .command(Command.LOGIN)
                .password(password)
                .build();

        try {
            this.out.writeObject(new Gson().toJson(packet));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void showMainMenu() {
        System.out.println("""
                1. Request Weather Data;
                2. Upload JSON Weather Data (ADMIN ONLY);
                """);
        Scanner scanner = new Scanner(System.in);
        String mainOptions = scanner.nextLine().toLowerCase();

        switch (mainOptions) {
            case "1", "request" -> requestWeatherData();
            case "2", "upload" -> uploadWeatherData();
        }
    }

    private void requestWeatherData() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the latitude:");
        Double latitude = scanner.nextDouble();
        System.out.println("Enter the longitude:");
        Double longitude = scanner.nextDouble();

        LocationPacket packet = LocationPacket.builder()
                .command(Command.REQUEST)
                .latitude(latitude)
                .longitude(longitude)
                .username(username)
                .build();
        try {
            this.out.writeObject(new Gson().toJson(packet));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private JsonObject readJsonFromFile(String filePath) {
        try (FileReader reader = new FileReader(filePath)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void uploadWeatherData() {
        if(currentRole.equals(Role.ADMIN)) {
            Path currentRelativePath = Paths.get("");
            String s = currentRelativePath.toAbsolutePath().toString();
            String filePath =  s + "\\WeatherClient\\src\\main\\data.json";
            JsonObject obj = readJsonFromFile(filePath);

            if(obj != null) {
                Double latitude = obj.get("latitude").getAsDouble();
                Double longitude = obj.get("longitude").getAsDouble();
                Double temperature = obj.get("temperature").getAsDouble();
                WeatherType weather = WeatherType.valueOf(obj.get("weather").getAsString());
                String jsonDate = obj.get("date").getAsString();
                LocalDate date = LocalDate.parse(jsonDate);

                WeatherPacket weatherPacket = WeatherPacket.builder()
                        .latitude(latitude)
                        .longitude(longitude)
                        .temperature(temperature)
                        .weatherType(weather)
                        .command(Command.UPLOAD)
                        .date(date)
                        .build();

                try {
                    Gson gson = new GsonBuilder()
                            .registerTypeAdapter(Packet.class, new CommandBasedDeserializer())
                            .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                            .create();
                    this.out.writeObject(gson.toJson(weatherPacket));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        else {
            System.out.println("Not eligible for this command!");
        }
    }

    private void handleServerResponse(String responseJson) {

        Gson gson = new GsonBuilder().registerTypeAdapter(Packet.class, new CommandBasedDeserializer()).create();
        Packet packet = gson.fromJson(responseJson, Packet.class);

        switch (packet.getCommand()) {
            case LOGIN, AUTH -> {
                UserPacket userPacket = (UserPacket) packet;
                this.currentRole = userPacket.getRole();
                this.isAuthenticated = true;
                this.username = userPacket.getUsername();
            }

            case WEATHER_RESPONSE -> {
                WeatherPacket weatherPacket = (WeatherPacket) packet;
                LocalDate date = weatherPacket.getDate();
                System.out.println(date);
                System.out.println("Today, it is " + weatherPacket.getTemperature() + " degrees in " + weatherPacket.getCityName());
            }

            case MESSAGE -> {
                StatusPacket statusPacket = (StatusPacket) packet;
                System.out.println(statusPacket.getMessage());
            }
        }

//        if(packet.getCommand().equals(Command.LOGIN) || packet.getCommand().equals(Command.AUTH)) {
//            this.isAuthenticated = true;
//            this.currentUsername = ((UserPacket) packet).getUsername();
//            this.currentRole = ((UserPacket) packet).getRole();
//        }
//
//        if(packet.getCommand().equals(Command.REQUEST)) {
//            System.out.println("Today, it is " + ((WeatherPacket) packet).getTemperature() + " degrees in " + ((WeatherPacket) packet).getCityName());
//        }
//
//        if(packet.getCommand().equals(Command.MESSAGE)) {
//            System.out.println(responseJson);
//        }
//
//        if(packet.getCommand().equals(Command.UPLOAD)) {
//            return;
//        }
    }
}
