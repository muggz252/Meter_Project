package org.example;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

@Data
public class Contract {
    private String number;
    private int balance;
    private boolean isDayOfPay;
    private List<Meter> meterList;

    public Contract() {
        this.number = String.valueOf(new Random().nextInt(10000, 99999));
        this.meterList = new ArrayList<>();
        this.isDayOfPay = getDayOfPay(LocalDate.now());
    }

    public boolean getDayOfPay(LocalDate date) {
        return date.getDayOfMonth() <= 15;
    }

    public void setDayOfPay(boolean b) {
        this.isDayOfPay = b;
    }

    @Override
    public String toString() {
        return "№ " + number + ", balance:" + balance + ", " + getMeterList();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Contract contract = (Contract) o;
        return Objects.equals(number, contract.number);
    }

    @Override
    public int hashCode() {
        return Objects.hash(number);
    }
}
