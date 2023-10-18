package org.example;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Meter meter = (Meter) o;
        return type == meter.type && Objects.equals(number, meter.number) && Objects.equals(tarif, meter.tarif);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, number, tarif);
    }

    @Override
    public String toString() {
        return number + "-" + type;
    }
}
