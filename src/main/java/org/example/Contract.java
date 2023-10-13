package org.example;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Data
public class Contract {
    private String number;
    private int balance;
    @JsonIgnore
    private boolean isDayOfPay;
    private List<Meter> meterList;

    public Contract() {
        this.number = String.valueOf(new Random().nextInt(10000, 99999));
        this.meterList = new ArrayList<>();
    }

    @Override
    public String toString() {
        return "№ " + number + ", balance:" + balance + ", " + getMeterList();
    }
}
