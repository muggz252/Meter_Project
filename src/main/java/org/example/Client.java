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
        String from = "maximfedorovykh@gmail.com";
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
                    }
                });
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
            while (number != 6) {
                number = Input.nextInt("""
                        1. Посмотреть список договоров
                        2. Посмотреть наличие приборов по договору
                        3. Заключить новый договор
                        4. Зарегистрировать счетчик по номеру договора
                        5. Передать показания счетчика
                        6. Выход
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
                                    System.out.println("Оплатите долг во избежание расторжения договора\n");
                                }
                                try {
                                    Meter meter = contract.getMeterList().stream()
                                            .filter(t -> t.getNumber().equals(Input.next("№ счетчика: ")))
                                            .findAny().orElseThrow();
                                    if (meter.getTarif() != null) dataMenu(contract, meter);
                                    else System.out.println("Тарифный план прибора еще не выбран\n");
                                } catch (NoSuchElementException e) {
                                    System.out.println("Такого прибора не найдено");
                                }
                            } else System.out.println("Прием показаний в данное время недоступен.\n" +
                                    "Дождитесь учетного периода или обратитесь к аминистратору \n" +
                                    "для изменения графика учетного периода.\n");
                        } else System.out.println("Договора с таким номером нет\n");
                    }
                    case 6 -> System.out.println("*************");
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
        switch (meterChoice) {
            case 1 -> {
                Meter meter = new Meter(MeterType.WATER);
                contract.getMeterList().add(meter);
                addTarif(meter);
                System.out.println("Вы заключили договор № " + contract.getNumber() + "\n");
            }
            case 2 -> {
                Meter meter = new Meter(MeterType.ELECTRO);
                contract.getMeterList().add(meter);
                addTarif(meter);
                System.out.println("Вы заключили договор № " + contract.getNumber() + "\n");
            }
            case 3 -> System.out.println("Вы заключили договор № " + contract.getNumber() + "\n");
        }
        c.getContractList().add(contract);
    }

    public static void addMeter(int meterchoice, List<Meter> meterList) {
        switch (meterchoice) {
            case 1 -> {
                Meter m = new Meter(MeterType.WATER);
                meterList.add(m);
                addTarif(m);
                System.out.println("Прибор № " + m.getNumber() + " добавлен" + "\n");
                Service.listToJson(Service.CLIENT_PATH, Service.clientList);
            }
            case 2 -> {
                Meter m = new Meter(MeterType.ELECTRO);
                meterList.add(m);
                addTarif(m);
                System.out.println("Прибор № " + m.getNumber() + " добавлен" + "\n");
                Service.listToJson(Service.CLIENT_PATH, Service.clientList);
            }
            case 3 -> System.out.println("Прибор не выбран");
        }
    }

    public static void addTarif(Meter meter) {
        int choice = Input.nextInt("""
                Выбор тарифного плана прибора: 
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
        int data1;
        int data2 = 0;
        if (m.getTarif().getType().equals(TarifType.SIMPLE)) {
            data1 = Input.nextInt("Введите показания счетчика: ");
        } else {
            data1 = Input.nextInt("Введите дневные показания счетчика: ");
            data2 = Input.nextInt("Введите ночные показания счетчика: ");
        }
        c.setBalance(c.getBalance() - m.getTarif().action(m.getDayData(), data1, m.getNightData(), data2));
        m.setDayData(data1);
        m.setNightData(data2);
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
                builder.append("Счетчик" + ";").append("день" + ";").append("ночь" + ";");
            }
            for (Client client : Service.clientList) {
                for (Contract contract : client.contractList) {
                    builder.append("\nДоговор " + contract.getNumber() + ",");
                    for (Meter meter : contract.getMeterList()) {
                        builder.append(meter + ";").append(meter.getDayData() + ";")
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
        String[] data = null;
        try {
            List<String> strings = Files.readAllLines(path);
            for (int i = 1; i < strings.size(); i++) {
                data = strings.get(i).split(",");
                for (int j = 1; j < data.length; j++) {
                    if (data[j].startsWith(m.getNumber())) {
                        i = strings.size();
                        break;
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return data;
    }

    public static void dataMenu(Contract contract, Meter meter) {
        int choice = Input.nextInt("""
                1.Ввести в консоли
                2.Заполнить файл
                3.Выход
                """);
        if (choice < 3) {
            switch (choice) {
                case 1 -> {
                    addMeterData(contract, meter);
                    Service.listToJson(Service.CLIENT_PATH, Service.clientList);
                }
                case 2 -> {
                    System.out.println(Arrays.toString(parseCSV(meter, Path.of("meter.csv"))));
                }
            }
        }
    }
}
