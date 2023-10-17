package org.example;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Tarif {
    public TarifType type;

    public int action(int meterDayData, int dayData, int meterNightData, int nightData) {
        return (dayData - meterDayData) + (nightData - meterNightData) / 2;
    }
}
