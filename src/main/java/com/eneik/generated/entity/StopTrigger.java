package com.eneik.generated.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "stop_trigger")
public class StopTrigger {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trigger_word", nullable = false, unique = true)
    private String triggerWord;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTriggerWord() {
        return triggerWord;
    }

    public void setTriggerWord(String triggerWord) {
        this.triggerWord = triggerWord;
    }
}
