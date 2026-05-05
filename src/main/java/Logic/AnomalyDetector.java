package Logic;

import java.util.List;

/**
 * Z-score based anomaly detection for electricity consumption.
 * Flags readings that deviate more than 2 standard deviations from the mean.
 */
public class AnomalyDetector {

    /**
     * Checks if a consumption value is an anomaly based on historical data.
     * Uses Z-score threshold of 2.0 standard deviations.
     * @param value the consumption value to check
     * @param historicalData list of previous consumption values
     * @return true if the value is an anomaly, false otherwise
     */
    public static boolean isAnomaly(double value, List<Double> historicalData) {
        if (historicalData == null || historicalData.size() < 3) return false;
        double mean = historicalData.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = historicalData.stream().mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(0);
        double stdDev = Math.sqrt(variance);
        if (stdDev == 0) return false;
        double zScore = Math.abs((value - mean) / stdDev);
        return zScore > Z_THRESHOLD;
    }

    /**
     * Calculates the Z-score for a value relative to historical data.
     * @param value the consumption value
     * @param historicalData list of previous consumption values
     * @return the Z-score (standard deviations from mean)
     */
    public static double getZScore(double value, List<Double> historicalData) {
        if (historicalData == null || historicalData.size() < 2) return 0;
        double mean = historicalData.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = historicalData.stream().mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(0);
        double stdDev = Math.sqrt(variance);
        if (stdDev == 0) return 0;
        return (value - mean) / stdDev;
    }

    /**
     * Determines the severity level of an anomaly based on Z-score.
     * @param zScore the Z-score value
     * @return "HIGH", "MEDIUM", or "LOW" severity
     */
    public static String getSeverity(double zScore) {
        double abs = Math.abs(zScore);
        if (abs > 3.5) return "HIGH";
        if (abs > 2.5) return "MEDIUM";
        return "LOW";
    }

    /**
     * Generates a descriptive message for an anomaly.
     * @param value the anomalous consumption value
     * @param mean the average consumption
     * @param zScore the Z-score
     * @return a formatted description string
     */
    public static String getDescription(double value, double mean, double zScore) {
        String direction = value > mean ? "above" : "below";
        return String.format("Consumption %.2f kWh is %.1f standard deviations %s average (%.2f kWh). Possible meter fault or unusual activity.",
            value, Math.abs(zScore), direction, mean);
    }
}
