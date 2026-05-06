package Logic;

import database.Tariff;
import java.util.Calendar;

/**
 * Utility class for calculating electricity bill amounts.
 * Supports both flat-rate and tiered billing calculations.
 */
public class BillCalculator {

    /**
     * Calculates the base amount for consumption using flat rate.
     * @param consumptionKwh the consumption in kWh
     * @param tariff the tariff containing the rate
     * @return the calculated amount before tax and fixed charges
     */
    public static double calculateAmount(double consumptionKwh, Tariff tariff) {
        return consumptionKwh * tariff.getRatePerKwh();
    }

    /**
     * Calculates the total bill amount including tax and fixed charges.
     * @param consumptionKwh the consumption in kWh
     * @param tariff the tariff containing rates and fixed charge
     * @return the total bill amount
     */
    public static double calculateTotal(double consumptionKwh, Tariff tariff) {
        double base = calculateAmount(consumptionKwh, tariff) + tariff.getFixedCharge();
        double tax  = SystemSettings.getTaxPercent();
        return tax > 0 ? base * (1 + tax / 100.0) : base;
    }

    /**
     * Calculates the total bill amount using tiered pricing.
     * First 100 kWh at base rate, next 200 at 1.2x, above 300 at 1.5x.
     * Includes tax, fixed charges, and minimum bill amount.
     * @param consumptionKwh the consumption in kWh
     * @param tariff the tariff containing base rate and fixed charge
     * @return the total bill amount
     */
    public static double calculateTieredAmount(double consumptionKwh, Tariff tariff) {
        double rate = tariff.getRatePerKwh();
        double amount;
        if (consumptionKwh <= 100) {
            amount = consumptionKwh * rate;
        } else if (consumptionKwh <= 300) {
            amount = 100 * rate + (consumptionKwh - 100) * rate * 1.2;
        } else {
            amount = 100 * rate + 200 * rate * 1.2 + (consumptionKwh - 300) * rate * 1.5;
        }
        double total = amount + tariff.getFixedCharge();
        double minBill = SystemSettings.getMinBillAmount();
        if (total < minBill) total = minBill;
        double tax = SystemSettings.getTaxPercent();
        if (tax > 0) total = total * (1 + tax / 100.0);
        return total;
    }

    /**
     * Calculates the due date for a bill based on system settings.
     * @return the due date as a SQL Date
     */
    public static java.sql.Date calculateDueDate() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, SystemSettings.getBillDueDays());
        return new java.sql.Date(cal.getTimeInMillis());
    }
}
