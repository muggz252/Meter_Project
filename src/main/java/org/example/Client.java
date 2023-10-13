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
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
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
                        Service.listToJson(Service.CONTRACT_PATH, Service.contractList);
                    }
                    case 4 -> {
                        Contract c = getContract(Input.next("№ договора: "), client);
                        if (c != null) {
                            Service.contractList.remove(c);
                            List<Meter> meterList = c.getMeterList();
                            addMeter(meterChoice(), meterList);
                            Service.contractList.add(c);
                            Service.listToJson(Service.CONTRACT_PATH,Service.contractList);
                        } else System.out.println("Договора под таким номером нет");
                    }
                    case 5 -> {
                        if (Admin.getDayOfPay(LocalDate.now())){

                        } else System.out.println();
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
                System.out.println("Вы заключили договор № " + contract.getNumber() + " на учет водоснабжения");
            }
            case 2 -> {
                Meter meter = new Meter(MeterType.ELECTRO);
                contract.getMeterList().add(meter);
                System.out.println("Вы заключили договор № " + contract.getNumber() + " на учет электроснабжения");
            }
            case 3 -> System.out.println("Вы заключили договор № " + contract.getNumber());
        }
        c.getContractList().add(contract);
        contract.setDayOfPay(Admin.getDayOfPay(LocalDate.now()));
        Service.contractList.add(contract);
    }

    public static void addMeter(int meterchoice, List<Meter> meterList) {
        switch (meterchoice) {
            case 1 -> {
                Meter m = new Meter(MeterType.WATER);
                meterList.add(m);
                System.out.println("Прибор добавлен");
                Service.listToJson(Service.CLIENT_PATH, Service.clientList);
            }
            case 2 -> {
                Meter m = new Meter(MeterType.ELECTRO);
                meterList.add(m);
                System.out.println("Прибор добавлен");
                Service.listToJson(Service.CLIENT_PATH, Service.clientList);
            }
            case 3 -> System.out.println("Прибор не выбран");
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

}
