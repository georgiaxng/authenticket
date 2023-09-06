package com.authenticket.authenticket.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "event", schema = "dev")
@EqualsAndHashCode(callSuper = true)
public class Event extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Integer eventId;

    @Column(name = "event_name", nullable = false)
    private String eventName;

    @Column(name = "event_description")
    private String eventDescription;

    @Column(name = "event_date")
    private LocalDateTime eventDate;

    @Column(name = "event_location")
    private String eventLocation;

    @Column(name = "other_event_info")
    private String otherEventInfo;

    @Column(name = "event_image")
    private String eventImage;

    @Column(name = "ticket_sale_date")
    private LocalDateTime ticketSaleDate;

    @Column(name = "total_tickets")
    private Integer totalTickets;

    @Column(name = "total_tickets_sold")
    private Integer totalTicketsSold = 0;

    @Column(name = "approved_by")
    private Integer approvedBy;

    @ManyToOne(fetch = FetchType.EAGER)
    @JsonIgnore
    @JoinColumn(name = "organiser_id", nullable = false)
    private EventOrganiser organiser;

    @ManyToOne(fetch = FetchType.EAGER)
    @JsonIgnore
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

//    @Getter
    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(
            schema = "dev",
            name = "artist_event",
            joinColumns = {@JoinColumn(name = "event_id")},
            inverseJoinColumns = {@JoinColumn(name = "artist_id")})
    private Set<Artist> artists;

//    public void setArtists(Set<Artist> artist){
//        this.artists = artist;
//    }

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="type_id",nullable = false)
    private EventType eventType;

    @OneToMany
    @JoinColumn(name = "event_id", referencedColumnName = "event_id")
    Set<EventTicketCategory> eventTicketCategorySet = new HashSet<>();

//    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
//    @JoinTable(name = "event_ticket_category", joinColumns = {@JoinColumn(table = "event",
//            name = "event_id",
//            referencedColumnName = "event_id"),
//            @JoinColumn(table = "ticket_categories",
//                    name = "category_id",
//                    referencedColumnName = "category_id")},
//            inverseJoinColumns = {@JoinColumn(table = "event",
//                    name="event_id",
//                    referencedColumnName = "event_id")})
//    Set<EventTicketCategory> eventTicketCategorySet = new HashSet<>();

    //    @ManyToOne(fetch = FetchType.EAGER)
//    @JsonIgnore
//    @JoinColumn(name = "venue_id")
//    private Venue venue;

//    //https://www.baeldung.com/jpa-many-to-many
//    @ManyToMany
//    private ArrayList<Artist> artistList;
//
//
//    public Event(Integer eventId, String eventName, String eventDescription, LocalDateTime eventDate, String eventLocation, String otherEventInfo, String eventImage, LocalDateTime ticketSaleDate, EventOrganiser eventOrganiser) {
//    }
}

