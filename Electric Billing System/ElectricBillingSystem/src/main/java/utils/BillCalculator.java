package utils;

import models.Tariff;
import java.util.Calendar;

public class BillCalculator {

    public static double calculateAmount(double consumptionKwh, Tariff tariff) {
        return consumptionKwh * tariff.getRatePerKwh();
    }

    public static double calculateTotal(double consumptionKwh, Tariff tariff) {
        double base = calculateAmount(consumptionKwh, tariff) + tariff.getFixedCharge();
        double tax  = SystemSettings.getTaxPercent();
        return tax > 0 ? base * (1 + tax / 100.0) : base;
    }

    // Tiered billing: first 100 kWh at base rate, next 200 at 1.2x, above 300 at 1.5x
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
        // Apply minimum bill
        double minBill = SystemSettings.getMinBillAmount();
        if (total < minBill) total = minBill;
        // Apply tax
        double tax = SystemSettings.getTaxPercent();
        if (tax > 0) total = total * (1 + tax / 100.0);
        return total;
    }

    /** Calculate due date based on configured bill_due_days setting. */
    public static java.sql.Date calculateDueDate() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, SystemSettings.getBillDueDays());
        return new java.sql.Date(cal.getTimeInMillis());
    }
}
