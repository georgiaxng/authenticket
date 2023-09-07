package com.authenticket.authenticket.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EqualsAndHashCode(callSuper = true)
@Table(name = "ticket", schema = "dev")
public class Ticket extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Integer ticketId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JsonIgnore
    @JoinColumns({
            @JoinColumn(name="event_id", referencedColumnName="event_id", nullable = false),
            @JoinColumn(name="category_id", referencedColumnName="category_id", nullable = false)
    })
    private EventTicketCategory eventTicketCategory;
}
