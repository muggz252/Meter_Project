package org.example;

public enum Tarif {
    SIMPLE {
        public int action(int startTerm, int endTerm) {
            return endTerm - startTerm;
        }
    },
    DAYNIGHT {
        public int action(int startDayMeter, int endDayMeter, int startNightMeter, int endNightMeter) {
            return (endDayMeter - startDayMeter) + (startNightMeter - endNightMeter) / 2;
        }
    }
}
