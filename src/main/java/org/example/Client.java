package org.example;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.*;


@AllArgsConstructor
@NoArgsConstructor
@Data
public class Client {
    private String login;
    private String password;
    private List<Contract> contractList;

    public static Client of(String to) {
        Client client = null;
        String code = confirmEmail(to);
        if (code != null) {
            sendMail(to, code);
            String password = createPassword(code, Input.next("Код для проверки почты из письма: "));
            client = new Client(to, password, new ArrayList<>());
        }
        return client;
    }

    public static void add(Client client) {
        if (client != null) {
            String login = client.getLogin();
            Service.clientList.add(client);
            Service.listToJson(Service.CLIENT_PATH, Service.clientList);
            System.out.println("Новый клиент " + login + " зарегистрирован.");
        } else System.out.println("Этот клиент уже зарегистрирован");
    }

    public static void sendMail(String mail, String code) {
        String from = "maximfedorovykh@gmail.com"; //в поле from вставляем наш e-mail - только gmail!
        String to = mail;
        String host = "smtp.gmail.com";
        String port = "465";

        Properties properties = new Properties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", port);
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.auth", "true");

        Session session = Session.getInstance(
                properties, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(from, "mqpukzhovmfdeimp");
                    }   // вторым параметром в конструктор PasswordAuthentication передаем пароль,
                });    // который генерируем по пути -> Управление аккаунтом - Безопасность - Двухэтапная аутентификация - Пароли приложений
        try {
            MimeMessage mimeMessage = new MimeMessage(session);
            mimeMessage.setFrom(new InternetAddress(from));
            mimeMessage.addRecipients(Message.RecipientType.TO, to);
            mimeMessage.setSubject("Регистрация");
            mimeMessage.setText(code);
            Transport.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static String confirmEmail(String eMail) {
        Random rnd = new Random();
        String code = String.valueOf(rnd.nextInt(1000, 9999));
        while (!eMail.matches(".+@.+\\.\\w+")) {
            eMail = Input.next("Это не e-mail, попробуйте еще раз: ");
        }
        String finalEMail = eMail;
        if (Service.clientList.stream().anyMatch(t -> t.getLogin().equals(finalEMail))) {
            return null;
        }
        return code;
    }

    public static String createPassword(String code, String input) {
        String password = input;
        while (!password.equals(code)) {
            password = Input.next("Неверный код. Попробуйте еще раз: ");
        }
        while (!password.matches("^(?=.*[A-Z])(?=.*\\d).+")) {
            password = Input.next("Придумайте пароль. Не менее шести символов " +
                    "латинских букв и цифр. Наличие одной заглавной буквы обязательно: ");
        }
        return password;
    }

    public static List<Client> fromJsonToList(Path path) {
        ObjectMapper mapper = new ObjectMapper();
        List<Client> list;
        try (FileReader reader = new FileReader(path.toString())) {
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
        return login + "-" + contractList;
    }

    public static void getClientMenu(String login) {
        int number = 0;
        Client client = getClient(login);
        if (getClient(login) != null) {
            while (number != 7) {
                number = Input.nextInt("""
                        1. Посмотреть список договоров
                        2. Посмотреть наличие приборов по договору
                        3. Заключить новый договор
                        4. Зарегистрировать счетчик по номеру договора
                        5. Передать показания счетчика
                        6. Пополнить баланс договора
                        7. Выход
                        """);
                switch (number) {
                    case 1 -> System.out.println(client.getContractList());
                    case 2 -> {
                        Contract c = getContract(Input.next("№ договора: "), client);
                        if (c != null) System.out.println(c.getMeterList());
                        else System.out.println("Договора под таким номером нет");
                    }
                    case 3 -> {
                        addContract(meterChoice(), getClient(login));
                        Service.listToJson(Service.CLIENT_PATH, Service.clientList);
                    }
                    case 4 -> {
                        Contract c = getContract(Input.next("№ договора: "), client);
                        if (c != null) {
                            List<Meter> meterList = c.getMeterList();
                            addMeter(meterChoice(), meterList);
                        } else System.out.println("Договора под таким номером нет");
                    }
                    case 5 -> {
                        Contract contract = getContract(Input.next("№ договора: "), client);
                        if (contract != null) {
                            if (contract.isDayOfPay()) {
                                if (contract.getBalance() < 0) {
                                    System.out.println("Оплатите долг на балансе договора\n");
                                }
                                try {
                                    String meterNumber = Input.next("№ счетчика: ");
                                    Meter meter = contract.getMeterList().stream()
                                            .filter(t -> t.getNumber().equals(meterNumber))
                                            .findAny().orElseThrow();
                                    if (meter.getTarif() != null) {
                                        dataMenu(contract, meter);
                                    } else System.out.println("Тарифный план прибора еще не выбран\n");
                                } catch (NoSuchElementException e) {
                                    System.out.println("Такого прибора не найдено\n");
                                }
                            } else System.out.println("""
                                    Прием показаний в данное время недоступен.
                                    Дождитесь окончания учетного периода или обратитесь к аминистратору\s
                                    для изменения графика учетного периода.
                                    """);
                        } else System.out.println("Договора с таким номером нет\n");
                    }
                    case 7 -> System.out.println("*************");
                }
            }
        } else System.out.println("Такого клиента нет\n");
    }

    public static Client getClient(String login) {
        Client client = null;
        for (Client c : Service.clientList) {
            if (c.getLogin().equals(login)) {
                client = c;
            }
        }
        return client;
    }

    public static void addContract(int meterChoice, Client c) {
        Contract contract = new Contract();
        Meter meter = null;
        switch (meterChoice) {
            case 1 -> meter = new Meter(MeterType.WATER);
            case 2 -> meter = new Meter(MeterType.ELECTRO);
        }
        contract.getMeterList().add(meter);
        addTarif(meter);
        System.out.println("Вы заключили договор № " + contract.getNumber() + "\n");
        c.getContractList().add(contract);
        createCSV(Path.of("meter.csv"));
    }

    public static void addMeter(int meterchoice, List<Meter> meterList) {
        Meter m = null;
        switch (meterchoice) {
            case 1 -> {
                m = new Meter(MeterType.WATER);
                System.out.println("Прибор № " + m.getNumber() + " добавлен" + "\n");
            }
            case 2 -> {
                m = new Meter(MeterType.ELECTRO);
                System.out.println("Прибор № " + m.getNumber() + " добавлен" + "\n");
            }
            case 3 -> System.out.println("Прибор не выбран");
        }
        meterList.add(m);
        addTarif(m);
        Service.listToJson(Service.CLIENT_PATH, Service.clientList);
        createCSV(Path.of("meter.csv"));
    }

    public static void addTarif(Meter meter) {
        int choice = Input.nextInt("""
                Выбор тарифного плана прибора:\s
                1. Расчет по общему объему потребления
                2. Расчет дневного и ночного потребления отдельно
                3. Пока отказаться от выбора тарифного плана
                """);
        if (choice != 3) {
            switch (choice) {
                case 1 -> {
                    Tarif t = new Tarif(TarifType.SIMPLE);
                    meter.setTarif(t);
                    System.out.println("Выбран общий тариф");
                    System.out.println("Для смены тарифа обратитесь к администратору\n");
                }
                case 2 -> {
                    Tarif t = new Tarif(TarifType.DAYNIGHT);
                    meter.setTarif(t);
                    System.out.println("Выбран тариф с льготным потреблением с 23.00 до 8.00 часов");
                    System.out.println("Для смены тарифа обратитесь к администратору\n");
                }
            }
        }
    }

    public static int meterChoice() {
        int meterChoice = Input.nextInt("""
                Нажми 1 - выбор прибора показаний водоснабжения
                Нажми 2 - выбор прибора показаний электроснабжения
                Нажми 3 - пока отказаться от выбора прибора
                """);
        return meterChoice;
    }

    public static Contract getContract(String number, Client client) {
        Contract contract = null;
        for (Contract c : client.getContractList()) {
            if (c.getNumber().equals(number)) {
                contract = c;
            }
        }
        return contract;
    }

    public static void addMeterData(Contract c, Meter m) {
        int dayData = 0;
        int nightData = 0;
        if (m.getTarif().getType().equals(TarifType.SIMPLE)) {
            dayData = Input.nextInt("Введите показания счетчика: ");
        } else if (m.getTarif().getType().equals(TarifType.DAYNIGHT)) {
            dayData = Input.nextInt("Введите дневные показания счетчика: ");
            nightData = Input.nextInt("Введите ночные показания счетчика: ");
        } else {
            System.out.println("Прибор не подключен к тарифному плану");
        }
        if (dayData > 0 && nightData >= 0) {
            c.setBalance(c.getBalance() - m.getTarif().action(m.getDayData(), dayData, m.getNightData(), nightData));
            m.setDayData(dayData);
            m.setNightData(nightData);
            System.out.println("Показания переданы успешно\n");
        }
    }

    public static void addMeterData(Contract c, Meter m, String[] data) {
        int dayData = Integer.parseInt(data[0]) > m.getDayData() ? Integer.parseInt(data[0]) : 0;
        int nightData = Integer.parseInt(data[1])> m.getNightData()? Integer.parseInt(data[1]) : 0;
        if (m.getTarif() != null) {
            nightData = m.getTarif().type == TarifType.SIMPLE ? 0 : nightData;
            if (dayData == 0) {
                System.out.println("Данные некорректны. Показания меньше предыдущих или заполнены лишние поля \n" +
                        "По счетчику с общим тарифом принимаются показания только из поля 'день'");
            } else {
                c.setBalance(c.getBalance() - m.getTarif().action(m.getDayData(), dayData, m.getNightData(), nightData));
                m.setDayData(dayData);
                m.setNightData(nightData);
                System.out.println("Показания переданы успешно\n");
            }
        } else System.out.println("Прибор не подключен к тарифному плану");
    }

    public static void createCSV(Path path) {
        try (FileWriter writer = new FileWriter(path.toString())) {
            StringBuilder builder = new StringBuilder();
            long count = 0;
            for (Client client : Service.clientList) {
                for (Contract contract : client.contractList) {
                    if (contract.getMeterList().size() > count) {
                        count = contract.getMeterList().size();
                    }
                }
            }
            builder.append("Договор" + ";");
            for (long i = 0; i < count; i++) {
                builder.append("Счетчик" + ",").append("день" + ",").append("ночь" + ",");
            }
            for (Client client : Service.clientList) {
                for (Contract contract : client.contractList) {
                    builder.append("\nДоговор " + contract.getNumber() + ",");
                    for (Meter meter : contract.getMeterList()) {
                        builder.append(meter + ",").append(meter.getDayData() + ",")
                                .append(meter.getNightData() + ",");
                    }
                }
            }
            writer.write(builder.toString());
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static String[] parseCSV(Meter m, Path path) {
        String[] data;
        StringBuilder builder = new StringBuilder();
        try {
            List<String> strings = Files.readAllLines(path);
            for (int i = 1; i < strings.size(); i++) {
                data = strings.get(i).split(",");
                for (int j = 1; j < data.length; j++) {
                    if (data[j].startsWith(m.getNumber())) {
                        builder.append(data[j + 1]).append(",").append(data[j + 2]);
                        i = strings.size();
                        break;
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return builder.toString().split(",");
    }

    public static void dataMenu(Contract contract, Meter meter) {
        int choice = Input.nextInt("""
                1.Ввести в консоли
                2.Взять из файла
                3.Выход
                """);
        if (choice != 3) {
            switch (choice) {
                case 1 -> {
                    addMeterData(contract, meter);
                    createCSV(Path.of("meter.csv"));
                    System.out.println("Баланс договора " + contract.getBalance() + "\n");
                    Service.listToJson(Service.CLIENT_PATH, Service.clientList);
                }
                case 2 -> {
                    addMeterData(contract, meter, parseCSV(meter, Path.of("meter.csv")));
                    createCSV(Path.of("meter.csv"));
                    System.out.println("Баланс договора " + contract.getBalance() + "\n");
                    Service.listToJson(Service.CLIENT_PATH, Service.clientList);
                }
            }
        }
    }
}
