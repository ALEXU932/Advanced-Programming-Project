package models;

import java.util.Date;

public class MeterReading {
    private int readingId;
    private int customerId;
    private String customerName;
    private Date readingDate;
    private double consumptionKwh;
    private double previousReading;
    private double currentReading;

    public MeterReading() {}

    public int getReadingId() { return readingId; }
    public void setReadingId(int readingId) { this.readingId = readingId; }
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public Date getReadingDate() { return readingDate; }
    public void setReadingDate(Date readingDate) { this.readingDate = readingDate; }
    public double getConsumptionKwh() { return consumptionKwh; }
    public void setConsumptionKwh(double consumptionKwh) { this.consumptionKwh = consumptionKwh; }
    public double getPreviousReading() { return previousReading; }
    public void setPreviousReading(double previousReading) { this.previousReading = previousReading; }
    public double getCurrentReading() { return currentReading; }
    public void setCurrentReading(double currentReading) { this.currentReading = currentReading; }
}
