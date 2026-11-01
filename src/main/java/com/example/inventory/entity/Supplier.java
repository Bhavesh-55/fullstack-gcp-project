package com.example.inventory.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "suppliers")
public class Supplier extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 200)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(nullable = false)
    private boolean active;

    public Supplier() {}

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public boolean getActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

}
