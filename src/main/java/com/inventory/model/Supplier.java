package com.inventory.model;

public class Supplier {
    private int supplierId;
    private String name;
    private String contactEmail;
    private String phone;

    public Supplier() {
    }

    public Supplier(int supplierId, String name, String contactEmail, String phone) {
        this.supplierId = supplierId;
        this.name = name;
        this.contactEmail = contactEmail;
        this.phone = phone;
    }

    public int getSupplierId() { return supplierId; }
    public void setSupplierId(int supplierId) { this.supplierId = supplierId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    @Override
    public String toString() { return name; }
}
