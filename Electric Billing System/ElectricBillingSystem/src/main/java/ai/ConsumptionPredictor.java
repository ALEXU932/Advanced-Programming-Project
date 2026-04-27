package ai;

import java.util.List;

/**
 * AI Consumption Predictor — works from day 1 with 0 readings.
 * Uses industry average baselines when historical data is insufficient.
 */
public class ConsumptionPredictor {

    // Industry average monthly consumption (kWh) for a typical household
    private static final double BASELINE_KWH = 150.0;

    /**
     * Predicts next month's consumption.
     * - 0 readings  → returns industry baseline
     * - 1 reading   → returns that reading (no trend yet)
     * - 2+ readings → linear regression
     */
    public static double predict(List<Double> history) {
        if (history == null || history.isEmpty()) return BASELINE_KWH;
        if (history.size() == 1) return history.get(0);

        int n = history.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++) {
            double x = i + 1, y = history.get(i);
            sumX += x; sumY += y; sumXY += x * y; sumX2 += x * x;
        }
        double denom = n * sumX2 - sumX * sumX;
        if (denom == 0) return sumY / n;
        double slope     = (n * sumXY - sumX * sumY) / denom;
        double intercept = (sumY - slope * sumX) / n;
        return Math.max(0, slope * (n + 1) + intercept);
    }

    /**
     * Confidence using R² (coefficient of determination) from linear regression.
     * - 0 readings → 40% (baseline)
     * - 1 reading  → 55%
     * - 2-5 readings → R² based but capped at 75% with warning
     * - 6+ readings → full R² × 100
     */
    public static double confidence(List<Double> history) {
        if (history == null || history.isEmpty()) return 40.0;
        if (history.size() == 1) return 55.0;

        int n = history.size();
        double sumX=0, sumY=0, sumXY=0, sumX2=0;
        for (int i=0; i<n; i++) {
            double x=i+1, y=history.get(i);
            sumX+=x; sumY+=y; sumXY+=x*y; sumX2+=x*x;
        }
        double denom = n*sumX2 - sumX*sumX;
        if (denom == 0) return 50.0;
        double slope     = (n*sumXY - sumX*sumY) / denom;
        double intercept = (sumY - slope*sumX) / n;
        double meanY     = sumY / n;

        // R² = 1 - SS_res / SS_tot
        double ssTot = 0, ssRes = 0;
        for (int i=0; i<n; i++) {
            double predicted = slope*(i+1) + intercept;
            ssTot += Math.pow(history.get(i) - meanY, 2);
            ssRes += Math.pow(history.get(i) - predicted, 2);
        }
        if (ssTot == 0) return 50.0;
        double r2 = Math.max(0, Math.min(1, 1 - ssRes/ssTot));
        double conf = r2 * 100;

        // Cap at 75% if fewer than 6 readings
        if (n < 6) conf = Math.min(conf, 75.0);
        return Math.round(conf * 10.0) / 10.0;
    }

    /**
     * Returns minimum readings needed for full confidence.
     */
    public static int getMinReadingsForFullConfidence() { return 6; }

    /**
     * Returns a message about data quality for display.
     */
    public static String getConfidenceMessage(List<Double> history) {
        if (history == null || history.isEmpty())
            return "No data — using industry baseline (40% confidence)";
        int n = history.size();
        if (n < 6) {
            int needed = 6 - n;
            return "Need " + needed + " more month" + (needed>1?"s":"") + " for accurate prediction (currently " + n + "/6)";
        }
        double conf = confidence(history);
        if (conf >= 80) return "High confidence (" + conf + "%) — based on " + n + " months of data";
        if (conf >= 60) return "Good confidence (" + conf + "%) — based on " + n + " months of data";
        return "Fair confidence (" + conf + "%) — consumption pattern is variable";
    }

    /**
     * Returns a contextual recommendation based on available data.
     * Works from 0 readings — provides onboarding tips when no data exists.
     */
    public static String getRecommendation(List<Double> history) {
        if (history == null || history.isEmpty()) {
            return "Welcome! No readings yet.\n\n" +
                   "AI Tip: Based on industry averages, a typical household uses ~150 kWh/month.\n" +
                   "Add your first meter reading to start personalized AI analysis.\n\n" +
                   "Energy Saving Tips to start with:\n" +
                   "• Switch to LED lighting (saves up to 75%)\n" +
                   "• Set AC to 24-26°C for optimal efficiency\n" +
                   "• Unplug devices when not in use";
        }
        if (history.size() == 1) {
            double val = history.get(0);
            String comparison = val > BASELINE_KWH
                ? String.format("%.0f%% above", ((val - BASELINE_KWH) / BASELINE_KWH) * 100)
                : String.format("%.0f%% below", ((BASELINE_KWH - val) / BASELINE_KWH) * 100);
            return "First reading recorded: " + String.format("%.2f kWh", val) + "\n\n" +
                   "This is " + comparison + " the industry average (" + BASELINE_KWH + " kWh).\n\n" +
                   "Add more readings each month to unlock trend analysis and accurate predictions.";
        }

        double last      = history.get(history.size() - 1);
        double predicted = predict(history);
        double change    = ((predicted - last) / last) * 100;

        if (change > 15)  return "Warning: Usage trending UP by " + String.format("%.1f", change) + "%. Consider reducing AC/heating and switching to LED lighting.";
        if (change > 5)   return "Slight increase expected. Monitor high-consumption appliances like AC, water heaters and refrigerators.";
        if (change < -10) return "Great! Usage trending DOWN. Keep up the energy-saving habits.";
        return "Consumption is stable. Maintain current usage patterns for optimal efficiency.";
    }

    /**
     * Returns a display label for the data quality level.
     */
    public static String getDataQualityLabel(List<Double> history) {
        if (history == null || history.isEmpty()) return "No Data — Using Baseline";
        if (history.size() == 1)  return "1 Reading — Limited Analysis";
        if (history.size() < 4)   return history.size() + " Readings — Basic Analysis";
        if (history.size() < 8)   return history.size() + " Readings — Good Analysis";
        return history.size() + " Readings — Full Analysis";
    }
}
