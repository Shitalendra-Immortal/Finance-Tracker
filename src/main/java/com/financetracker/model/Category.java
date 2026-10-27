package com.financetracker.model;

/** A transaction category. toString() returns the name so ComboBoxes show it. */
public class Category {
    private int id;
    private String name;
    private String type; // "INCOME" or "EXPENSE"

    public Category() {
    }

    public Category(int id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    @Override
    public String toString() { return name; }
}
