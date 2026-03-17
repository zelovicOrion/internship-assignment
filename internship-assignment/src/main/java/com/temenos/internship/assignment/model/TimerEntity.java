package com.temenos.internship.assignment.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@Table("timer")
public class TimerEntity implements Persistable<UUID> {
    @Id
    @Column("timer_id")
    private UUID timerId;

    @Column("created")
    private Long created;

    @Column("delay")
    private Integer delay;

    @Transient
    private boolean isNew = true;

    @Override
    public UUID getId() {
        return timerId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
}
