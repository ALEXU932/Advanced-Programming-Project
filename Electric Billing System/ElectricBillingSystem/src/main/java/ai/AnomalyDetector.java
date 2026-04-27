package ai;

import java.util.List;

/**
 * Z-score based anomaly detection for electricity consumption.
 * Flags readings that deviate more than 2 standard deviations from the mean.
 */
public class AnomalyDetector {

    private static final double Z_THRESHOLD = 2.0;

    public static boolean isAnomaly(double value, List<Double> historicalData) {
        if (historicalData == null || historicalData.size() < 3) return false;
        double mean = historicalData.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = historicalData.stream().mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(0);
        double stdDev = Math.sqrt(variance);
        if (stdDev == 0) return false;
        double zScore = Math.abs((value - mean) / stdDev);
        return zScore > Z_THRESHOLD;
    }

    public static double getZScore(double value, List<Double> historicalData) {
        if (historicalData == null || historicalData.size() < 2) return 0;
        double mean = historicalData.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = historicalData.stream().mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(0);
        double stdDev = Math.sqrt(variance);
        if (stdDev == 0) return 0;
        return (value - mean) / stdDev;
    }

    public static String getSeverity(double zScore) {
        double abs = Math.abs(zScore);
        if (abs > 3.5) return "HIGH";
        if (abs > 2.5) return "MEDIUM";
        return "LOW";
    }

    public static String getDescription(double value, double mean, double zScore) {
        String direction = value > mean ? "above" : "below";
        return String.format("Consumption %.2f kWh is %.1f standard deviations %s average (%.2f kWh). Possible meter fault or unusual activity.",
            value, Math.abs(zScore), direction, mean);
    }
}
