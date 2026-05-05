package database;

/**
 * Represents an electricity meter associated with a customer.
 */
public class Meter {
    private int meterId;
    private String meterNumber;
    private int customerId;
    private String customerName;
    private String meterType;
    private String status;
    private String location;
    private String installedAt;

    public Meter() {}

    public int getMeterId() { return meterId; }
    public void setMeterId(int meterId) { this.meterId = meterId; }
    public String getMeterNumber() { return meterNumber; }
    public void setMeterNumber(String meterNumber) { this.meterNumber = meterNumber; }
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getMeterType() { return meterType; }
    public void setMeterType(String meterType) { this.meterType = meterType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getInstalledAt() { return installedAt; }
    public void setInstalledAt(String installedAt) { this.installedAt = installedAt; }

    @Override
    public String toString() { return meterNumber + " (" + meterType + ")"; }
}
