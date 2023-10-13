package org.example;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import java.util.stream.Collectors;


@NoArgsConstructor
@Data
@AllArgsConstructor
public class Admin {

    private String login;
    private String password;

    public static Admin of(String login) {
        Admin admin = null;
        while (!login.matches("[a-zA-Z0-9]{6,}")) {
            login = Input.next("Логин не отвечает правилам. Попробуйте еще: ");
        }
        String finalLogin = login;
        if (Service.adminList.stream().noneMatch(t -> t.getLogin().equals(finalLogin))) {
            String password = "";
            while (!password.matches("^(?=.*[A-Z])(?=.*\\d).+")) {
                password = Input.next("Придумайте пароль. Не менее шести символов " +
                        "латинских букв и цифр. Наличие одной заглавной буквы обязательно: ");
            }
            admin = new Admin(login, password);
        }
        return admin;
    }

    public static void add(Admin admin) {
        if (admin != null) {
            String login = admin.getLogin();
            Service.adminList.add(admin);
            Service.listToJson(Service.ADMIN_PATH, Service.adminList);
            System.out.println("Новый админ " + login + " добавлен.");
        } else System.out.println("Этот админ уже зарегистрирован");
    }

    public static void getAdminMenu(String login) {
        Admin admin = getAdmin(login);
        if (admin != null) {
            int choice = 0;
            while (choice != 5) {
                choice = Input.nextInt("""
                        1. Создать файл с данными всех пользователей
                        2. Удалить учетную запись пользователя по номеру договора
                        3. Удалить счетчик по номеру договора и счетчика
                        4. Изменить учетный период договора
                        5. Выход
                        """);
                switch (choice) {
                    case 1 -> {
                        clientListToCSV(Service.clientList);
                        System.out.println("Файл готов!");
                    }
                    case 4 -> {
                        Contract c = getContract(Input.next("№ договора: "));
                        if (c!=null){
                            Service.contractList.remove(c);
                            c.setDayOfPay(!getDayOfPay(LocalDate.now()));
                            Service.contractList.add(c);
                            Service.listToJson(Service.CONTRACT_PATH,Service.contractList);
                            System.out.println("Учетный период договора изменен");
                        } else System.out.println("Договора под таким номером нет");
                    }

                    case 5 -> System.out.println("*************");
                }
            }
        } else System.out.println("Админа под таким логином нет");
    }

    public static Admin getAdmin(String login) {
        Admin admin = null;
        for (Admin a : Service.adminList) {
            if (a.getLogin().equals(login)) {
                admin = a;
            }
        }
        return admin;
    }

    public static boolean getDayOfPay(LocalDate date) {
        return date.getDayOfMonth() <= 15;
    }

    public static void clientListToCSV(List<Client> clientList) {
        try (FileWriter writer = new FileWriter("clients.csv")) {
            StringBuilder builder = new StringBuilder();
            builder.append("e-mail").append(";").append("password").append(";").append("contracts").append("\n");
            for (Client c : clientList) {
                builder.append(c.getLogin()).append(";").append("******" + ";")
                        .append(c.getContractList().stream().map(t -> t.getNumber() +
                                        " balance: " + t.getBalance())
                                .collect(Collectors.joining(";")))
                        .append("\n");
            }
            writer.write(builder.toString());
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static Contract getContract(String number) {
        Contract contract = null;
        for (Contract c : Service.contractList) {
            if (c.getNumber().equals(number)) {
                contract = c;
            }
        }
        return contract;
    }

    public static List<Admin> fromJsonToList(Path path) {
        ObjectMapper mapper = new ObjectMapper();
        List<Admin> list;
        try (var reader = new FileReader(path.toString())) {
            list = mapper.readValue(reader,
                    new TypeReference<>() {
                    });
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        return list;
    }

    @Override
    public String toString() {
        return login + ";" + password;
    }
}
