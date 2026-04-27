package models;

public class Customer {
    private int customerId;
    private int userId;
    private String name;
    private String email;
    private String phone;
    private String address;
    // Convenience field — fetched via JOIN with meters table
    private String meterNumber;

    public Customer() {}
    public Customer(int customerId, String name, String email,
                    String phone, String address, String meterNumber) {
        this.customerId = customerId; this.name = name; this.email = email;
        this.phone = phone; this.address = address; this.meterNumber = meterNumber;
    }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int id) { this.customerId = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getMeterNumber() { return meterNumber; }
    public void setMeterNumber(String meterNumber) { this.meterNumber = meterNumber; }

    @Override
    public String toString() { return name + (meterNumber != null ? " (" + meterNumber + ")" : ""); }
}
