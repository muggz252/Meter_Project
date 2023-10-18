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

import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;


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
            while (choice != 6) {
                choice = Input.nextInt("""
                             1. Создать файл с данными всех пользователей
                             2. Удалить учетную запись пользователя по номеру договора
                             3. Удалить счетчик по номеру договора и счетчика
                             4. Изменить учетный период договора
                             5. Поменять тарифный план счетчика
                             6. Выход
                        """);
                final Path path = Path.of("meter.csv");
                switch (choice) {
                    case 1 -> {
                        clientListToCSV(Service.clientList);
                        System.out.println("Файл готов!");
                    }
                    case 2 -> {
                        deleteClient(Input.next("№ договора: "));
                        Client.createCSV(path);
                        Service.listToJson(Service.CLIENT_PATH, Service.clientList);
                    }
                    case 3 -> {
                        deleteMeter(Input.next("№ договора: "));
                        Client.createCSV(path);
                        Service.listToJson(Service.CLIENT_PATH, Service.clientList);
                    }
                    case 4 -> {
                        changePeriod(Input.next("№ договора: "));
                        Service.listToJson(Service.CLIENT_PATH, Service.clientList);
                    }
                    case 5 -> {
                        String number = Input.next("№ договора: ");
                        changeTarif(number);
                        Service.listToJson(Service.CLIENT_PATH, Service.clientList);
                    }
                    case 6 -> System.out.println("*************");
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

    public static void deleteClient(String number) {
        Contract contract = null;
        try {
            contract = Service.clientList.stream().flatMap(t -> t.getContractList().stream())
                    .filter(t -> t.getNumber().equals(number)).findAny().orElseThrow();
        } catch (NoSuchElementException e) {
            System.out.println("Такого договора не найдено");
        }
        Client client = null;
        for (Client c : Service.clientList) {
            if (c.getContractList().contains(contract)) {
                client = c;
            }
        }
        Service.clientList.remove(client);
        System.out.println("Клиент " + client.getLogin() + " удален\n");
    }

    public static void deleteMeter(String number) {
        Contract contract = null;
        try {
            contract = Service.clientList.stream()
                    .flatMap(t -> t.getContractList().stream())
                    .filter(t -> t.getNumber().equals(number)).findAny().orElseThrow();
        } catch (NoSuchElementException e) {
            System.out.println("Такого договора нет");
        }
        String meterNumber = Input.next("№ счетчика: ");
        try {
            Meter meter = contract.getMeterList().stream()
                    .filter(t -> t.getNumber().equals(meterNumber)).findAny().orElseThrow();
            contract.getMeterList().remove(meter);
            System.out.println("Прибор " + meter + " удален\n");
        } catch (NoSuchElementException e) {
            System.out.println("Такого счетчика нет");
        }
    }

    public static void changePeriod(String number) {
        Contract contract = null;
        try {
            contract = Service.clientList.stream()
                    .flatMap(t -> t.getContractList().stream()
                            .filter(e -> e.getNumber().equals(number)))
                    .findAny().orElseThrow();
        } catch (NoSuchElementException e) {
            System.out.println("Договор не найден");
        }
        if (LocalDate.now().getDayOfMonth() <= 15 == contract.isDayOfPay()) {
            String answer = Input.next("Текущий период - c 1 по 15 число каждого месяца. " +
                    "Изменить его? (y/n): ");
            if (answer.equalsIgnoreCase("y")) {
                contract.setDayOfPay(!contract.isDayOfPay());
                System.out.println("Учетный период изменен. Новый учетный период - " +
                        "с 15 числа до конца месяца \n");
            } else System.out.println("Учетный период прежний");
        } else {
            String answer = Input.next("Текущий период - c 15 по конец месяца. " +
                    "Изменить его? (y/n): ");
            if (answer.equalsIgnoreCase("y")) {
                contract.setDayOfPay(!contract.isDayOfPay());
                System.out.println("Учетный период изменен. Новый учетный период - " +
                        "с 1 по 15 число месяца\n");
            } else System.out.println("Учетный период прежний");
        }
    }

    public static void changeTarif(String number) {
        String meterNumber = Input.next("№ Счетчика: ");
        try {
            Meter meter = Service.clientList.stream()
                    .flatMap(t -> t.getContractList().stream()
                            .filter(c -> c.getNumber().equals(number))
                            .flatMap(m -> m.getMeterList().stream()
                                    .filter(x -> x.getNumber().equals(meterNumber))))
                    .findAny().orElseThrow();
            System.out.println("Текущий тарифный план прибора: " + meter.getTarif().type);
            String answer = Input.next("Желаете поменять тариф(y/n)?: ");
            if (answer.equalsIgnoreCase("y")) {
                Tarif t;
                if (meter.getTarif().type == TarifType.DAYNIGHT) {
                    t = new Tarif(TarifType.SIMPLE);
                } else {
                    t = new Tarif(TarifType.DAYNIGHT);
                }
                meter.setTarif(t);
                System.out.println("Новый тариф - " + t.type + "\n");
            } else System.out.println("Тарифный план прежний\n");
        } catch (NoSuchElementException e) {
            System.out.println("Прибор не найден\n");
        }
    }

    @Override
    public String toString() {
        return login + ";" + password;
    }
}
