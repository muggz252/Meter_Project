package org.example;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Random;

@Data
@NoArgsConstructor
public class Meter {
    private MeterType type;
    private String number;
    private Tarif tarif;
    private int dayData;
    private int nightData;

    public Meter(MeterType type) {
        this.type = type;
        this.number = String.valueOf(new Random().nextInt(100,999));
    }

    @Override
    public String toString() {
        return number + "-" + type;
    }
}
