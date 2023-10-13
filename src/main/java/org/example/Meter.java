package org.example;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Random;

@Data
@NoArgsConstructor
public class Meter {
    private MeterType type;
    private String number;
    @JsonIgnore
    private int dayData;
    @JsonIgnore
    private int nightData;

    public Meter(MeterType type) {
        this.type = type;
        this.number = String.valueOf(new Random().nextInt(100,999));
    }

    @Override
    public String toString() {
        return type.toString() + "-" + number;
    }
}
