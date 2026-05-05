package database;

/**
 * Represents a tariff plan for electricity pricing.
 */
public class Tariff {
    private int tariffId;
    private String name;
    private double ratePerKwh;
    private double fixedCharge;
    private String startDate;
    private String endDate;
    private boolean active;

    public Tariff() {}
    public Tariff(int tariffId, String name, double ratePerKwh, double fixedCharge, String startDate, boolean active) {
        this.tariffId = tariffId; this.name = name; this.ratePerKwh = ratePerKwh;
        this.fixedCharge = fixedCharge; this.startDate = startDate; this.active = active;
    }

    public int getTariffId() { return tariffId; }
    public void setTariffId(int tariffId) { this.tariffId = tariffId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getRatePerKwh() { return ratePerKwh; }
    public void setRatePerKwh(double ratePerKwh) { this.ratePerKwh = ratePerKwh; }
    public double getFixedCharge() { return fixedCharge; }
    public void setFixedCharge(double fixedCharge) { this.fixedCharge = fixedCharge; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() { return name + " ($" + ratePerKwh + "/kWh)"; }
}
