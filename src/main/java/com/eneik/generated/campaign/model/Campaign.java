package com.eneik.generated.campaign.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "campaigns")
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Lead> leads = new ArrayList<>();

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SpintaxTemplate> spintaxTemplates = new ArrayList<>();

    public Campaign() {}

    public Campaign(String name, String description, String status) {
        this.name = name;
        this.description = description;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<Lead> getLeads() {
        return leads;
    }

    public void setLeads(List<Lead> leads) {
        this.leads = leads;
    }

    public void addLead(Lead lead) {
        leads.add(lead);
        lead.setCampaign(this);
    }

    public void removeLead(Lead lead) {
        leads.remove(lead);
        lead.setCampaign(null);
    }

    public List<SpintaxTemplate> getSpintaxTemplates() {
        return spintaxTemplates;
    }

    public void setSpintaxTemplates(List<SpintaxTemplate> spintaxTemplates) {
        this.spintaxTemplates = spintaxTemplates;
    }

    public void addSpintaxTemplate(SpintaxTemplate template) {
        spintaxTemplates.add(template);
        template.setCampaign(this);
    }

    public void removeSpintaxTemplate(SpintaxTemplate template) {
        spintaxTemplates.remove(template);
        template.setCampaign(null);
    }
}
