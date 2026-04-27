package models;

import java.util.Date;

public class Bill {
    private int billId;
    private int customerId;
    private String customerName;
    private int tariffId;
    private String billingMonth;
    private double consumptionKwh;
    private double amount;
    private double fixedCharge;
    private double totalAmount;
    private String status;
    private Date dueDate;
    private Date paidDate;
    private Date generatedAt;

    public Bill() {}

    public int getBillId() { return billId; }
    public void setBillId(int billId) { this.billId = billId; }
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public int getTariffId() { return tariffId; }
    public void setTariffId(int tariffId) { this.tariffId = tariffId; }
    public String getBillingMonth() { return billingMonth; }
    public void setBillingMonth(String billingMonth) { this.billingMonth = billingMonth; }
    public double getConsumptionKwh() { return consumptionKwh; }
    public void setConsumptionKwh(double consumptionKwh) { this.consumptionKwh = consumptionKwh; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public double getFixedCharge() { return fixedCharge; }
    public void setFixedCharge(double fixedCharge) { this.fixedCharge = fixedCharge; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getDueDate() { return dueDate; }
    public void setDueDate(Date dueDate) { this.dueDate = dueDate; }
    public Date getPaidDate() { return paidDate; }
    public void setPaidDate(Date paidDate) { this.paidDate = paidDate; }
    public Date getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Date generatedAt) { this.generatedAt = generatedAt; }
}
