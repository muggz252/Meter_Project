package org.example;


import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.List;

public class Service {
    public final static Path ADMIN_PATH = Path.of("admins.json");
    public final static Path CLIENT_PATH = Path.of("clients.json");

    public static List<Client> clientList;
    public static List<Admin> adminList;


    public Service() {
        int choice = 0;
        while (choice != 5) {
            choice = Input.nextInt("""
                       Добро пожаловать!
                    1. Регистрация админа
                    2. Регистрация нового клиента
                    3. Войти от имени клиента/Заключить договор/Зарегистрировать счетчик
                    4. Войти от имени администратора
                    5. Выход
                    """);
            switch (choice) {
                case 1 -> {
                    adminList = !Files.exists(ADMIN_PATH) ?
                            new ArrayList<>() : Admin.fromJsonToList(ADMIN_PATH);
                    Admin.add(Admin.of(Input.next("Login: ")));
                }
                case 2 -> {
                    clientList = !Files.exists(CLIENT_PATH) ?
                            new ArrayList<>() : Client.fromJsonToList(CLIENT_PATH);
                    Client.add(Client.of(Input.next("E-mail: ")));
                }
                case 3 -> {
                    clientList = !Files.exists(CLIENT_PATH) ?
                            new ArrayList<>() : Client.fromJsonToList(CLIENT_PATH);
                    Client.createCSV(Path.of("meter.csv"));
                    System.out.println(clientList);
                    Client.getClientMenu(Input.next("E-mail: "));
                }
                case 4 -> {
                    adminList = !Files.exists(ADMIN_PATH) ?
                            new ArrayList<>() : Admin.fromJsonToList(ADMIN_PATH);
                    clientList = !Files.exists(CLIENT_PATH) ?
                            new ArrayList<>() : Client.fromJsonToList(CLIENT_PATH);
                    Admin.getAdminMenu(Input.next("Login: "));
                }
            }
        }
    }

    public static <T> void listToJson(Path path, List<T> list) {
        try (FileWriter writer = new FileWriter(path.toString())) {
            String json = new ObjectMapper().writerWithDefaultPrettyPrinter()
                    .writeValueAsString(list);
            writer.write(json);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

}
