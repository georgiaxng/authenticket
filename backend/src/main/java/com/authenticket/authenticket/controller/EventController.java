package com.authenticket.authenticket.controller;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.authenticket.authenticket.controller.response.GeneralApiResponse;
import com.authenticket.authenticket.dto.event.*;
import com.authenticket.authenticket.exception.ApiRequestException;
import com.authenticket.authenticket.dto.section.SectionTicketDetailsDto;
import com.authenticket.authenticket.exception.NonExistentException;
import com.authenticket.authenticket.model.*;
import com.authenticket.authenticket.repository.*;
import com.authenticket.authenticket.service.*;
import com.authenticket.authenticket.service.impl.EventServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@RestController
@CrossOrigin(
        origins = {
                "${authenticket.frontend-production-url}",
                "${authenticket.frontend-dev-url}",
                "${authenticket.loadbalancer-url}"
        },
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT},
        allowedHeaders = {"Authorization", "Cache-Control", "Content-Type"},
        allowCredentials = "true"
)
@RequestMapping("/api/v2")
public class EventController extends Utility {
    private final EventServiceImpl eventService;

    private final PresaleService presaleService;

    private final AmazonS3Service amazonS3Service;

    private final EventRepository eventRepository;

    private final UserRepository userRepository;

    private final AdminRepository adminRepository;

    private final ArtistRepository artistRepository;

    private final VenueRepository venueRepository;

    private final EventTypeRepository eventTypeRepository;

    private final EventDtoMapper eventDtoMapper;

    private final TaskScheduler taskScheduler;

    private final TicketService ticketService;

    private final QueueService queueService;

    private static final int PRESALE_HOURS = 24;

    @Autowired
    public EventController(EventServiceImpl eventService,
                           AmazonS3Service amazonS3Service,
                           EventRepository eventRepository,
                           EventOrganiserRepository eventOrganiserRepository,
                           AdminRepository adminRepository,
                           ArtistRepository artistRepository,
                           VenueRepository venueRepository,
                           EventTypeRepository eventTypeRepository,
                           EventDtoMapper eventDtoMapper,
                           PresaleService presaleService,
                           UserRepository userRepository,
                           TaskScheduler taskScheduler,
                           TicketService ticketService,
                           QueueService queueService) {
        this.eventService = eventService;
        this.amazonS3Service = amazonS3Service;
        this.eventRepository = eventRepository;
        this.adminRepository = adminRepository;
        this.artistRepository = artistRepository;
        this.venueRepository = venueRepository;
        this.eventTypeRepository = eventTypeRepository;
        this.eventDtoMapper = eventDtoMapper;
        this.presaleService = presaleService;
        this.userRepository = userRepository;
        this.taskScheduler = taskScheduler;
        this.ticketService = ticketService;
        this.queueService = queueService;
    }

    @GetMapping("/public/event/test")
    public String test() {
        return "test successful";
    }

    @GetMapping("/public/event")
    public ResponseEntity<GeneralApiResponse<Object>> findAllPublicEvent(Pageable pageable) {
        try {
            List<EventHomeDto> eventList = eventService.findAllPublicEvent(pageable);
            if (eventList.isEmpty()) {
                return ResponseEntity.ok(generateApiResponse(eventList, "No events found."));
            } else {
                return ResponseEntity.ok(generateApiResponse(eventList, "Events successfully returned."));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(generateApiResponse(null, "Error getting the events."));
        }
    }

    @GetMapping("/public/event/{eventId}")
    public ResponseEntity<GeneralApiResponse<Object>> findEventById(@PathVariable("eventId") Integer eventId) {
        OverallEventDto overallEventDto = eventService.findEventById(eventId);
        if (overallEventDto == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(generateApiResponse(null, String.format("Event with id %d not found", eventId)));
        }
        return ResponseEntity.ok(generateApiResponse(overallEventDto, String.format("Event %d successfully returned.", eventId)));

    }

    @GetMapping("/public/event/featured")
    public ResponseEntity<GeneralApiResponse<Object>> findFeaturedEvents(Pageable pageable) {
        List<FeaturedEventDto> eventList = eventService.findFeaturedEvents(pageable);
        if (eventList == null || eventList.isEmpty()) {
            return ResponseEntity.ok(generateApiResponse(null, "No featured events found"));
        }
        return ResponseEntity.ok(generateApiResponse(eventList, "Featured events successfully returned."));

    }

    @GetMapping("/public/event/recently-added")
    public ResponseEntity<GeneralApiResponse<Object>> findRecentlyAddedEvents(Pageable pageable) {
        List<EventHomeDto> eventList = eventService.findRecentlyAddedEvents(pageable);
        if (eventList == null || eventList.isEmpty()) {
            return ResponseEntity.ok(generateApiResponse(null, "No recently added events found"));

        }
        return ResponseEntity.ok(generateApiResponse(eventList, "Recently added events successfully returned."));

    }


    @GetMapping("/public/event/bestseller")
    public ResponseEntity<GeneralApiResponse<Object>> findBestSellerEvents() {
        List<EventHomeDto> eventList = eventService.findBestSellerEvents();
        if (eventList == null || eventList.isEmpty()) {
            return ResponseEntity.ok(generateApiResponse(null, "No bestseller events found"));

        }
        return ResponseEntity.ok(generateApiResponse(eventList, "Bestseller events successfully returned."));

    }

    @GetMapping("/public/event/upcoming")
    public ResponseEntity<GeneralApiResponse<Object>> findUpcomingEvents(Pageable pageable) {
        List<EventHomeDto> eventList = eventService.findUpcomingEventsByTicketSalesDate(pageable);
        if (eventList == null || eventList.isEmpty()) {
            return ResponseEntity.ok(generateApiResponse(null, "No upcoming events found"));
        }
        return ResponseEntity.ok(generateApiResponse(eventList, "Upcoming events successfully returned."));

    }

    @GetMapping("/public/event/current")
    public ResponseEntity<GeneralApiResponse<Object>> findCurrentEventsByEventDate(Pageable pageable) {
        List<EventHomeDto> eventList = eventService.findCurrentEventsByEventDate(pageable);
        if (eventList == null || eventList.isEmpty()) {
            return ResponseEntity.ok(generateApiResponse(null, "No current events found"));
        }
        return ResponseEntity.ok(generateApiResponse(eventList, "Current events successfully returned."));

    }

    @GetMapping("/public/event/past")
    public ResponseEntity<GeneralApiResponse<Object>> findPastEventsByEventDate(Pageable pageable) {
        List<EventHomeDto> eventList = eventService.findPastEventsByEventDate(pageable);
        if (eventList == null || eventList.isEmpty()) {
            return ResponseEntity.ok(generateApiResponse(null, "No past events found"));
        }
        return ResponseEntity.ok(generateApiResponse(eventList, "Past events successfully returned."));

    }

    @GetMapping("/public/event/venue/{venueId}")
    public ResponseEntity<GeneralApiResponse<Object>> findEventsByVenue(Pageable pageable, @PathVariable("venueId") Integer venueId) {
        List<EventHomeDto> eventList = eventService.findEventsByVenue(venueId, pageable);
        if (eventList == null || eventList.isEmpty()) {
            return ResponseEntity.ok(generateApiResponse(null, "No events found for venue"));
        }
        return ResponseEntity.ok(generateApiResponse(eventList, "Events for venue successfully returned."));
    }    
@GetMapping("/public/event/venue/past/{venueId}")

    public ResponseEntity<GeneralApiResponse<Object>> findPastEventsByVenue(Pageable pageable, @PathVariable("venueId") Integer venueId) {
        List<EventHomeDto> eventList = eventService.findPastEventsByVenue(venueId, pageable);
        if (eventList == null || eventList.isEmpty()) {
            return ResponseEntity.ok(generateApiResponse(null, "No events found for venue"));
        }
        return ResponseEntity.ok(generateApiResponse(eventList, "Past events for venue successfully returned."));

    }    
@GetMapping("/public/event/venue/upcoming/{venueId}")

    public ResponseEntity<GeneralApiResponse<Object>> findUpcomingEventsByVenue(Pageable pageable, @PathVariable("venueId") Integer venueId) {
        List<EventHomeDto> eventList = eventService.findEventsByVenue(venueId, pageable);
        if (eventList == null || eventList.isEmpty()) {
            return ResponseEntity.ok(generateApiResponse(null, "No events found for venue"));
        }
        return ResponseEntity.ok(generateApiResponse(eventList, "Upcoming events for venue successfully returned."));

    }

    //get method for admin
    @GetMapping("/event")
    public ResponseEntity<GeneralApiResponse<Object>> findAllEvent() {
        try {
            List<EventAdminDisplayDto> eventList = eventService.findAllEvent();
            if (eventList.isEmpty()) {
                return ResponseEntity.ok(generateApiResponse(eventList, "No events found."));
            } else {
                return ResponseEntity.ok(generateApiResponse(eventList, "Events successfully returned."));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(generateApiResponse(null, e.getMessage()));
        }
    }

    @GetMapping("/event/enhanced")
    public ResponseEntity<GeneralApiResponse<Object>> findAllEnhancedEventForOrg(@NonNull HttpServletRequest request) {
        EventOrganiser organiser = retrieveOrganiserFromRequest(request);
        Integer organiserId = organiser.getOrganiserId();

        try {
            List<EventHomeDto> eventList = eventService.findEventsByOrganiserAndEnhancedStatus(organiserId, Boolean.TRUE);
            if (eventList.isEmpty()) {
                return ResponseEntity.ok(generateApiResponse(eventList, "No events found."));
            } else {
                return ResponseEntity.ok(generateApiResponse(eventList, "Enhanced events successfully returned."));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(generateApiResponse(null, e.getMessage()));
        }
    }

    @GetMapping("/event/not-enhanced")
    public ResponseEntity<GeneralApiResponse<Object>> findAllNotEnhancedEventForOrg(@NonNull HttpServletRequest request) {
        EventOrganiser organiser = retrieveOrganiserFromRequest(request);
        Integer organiserId = organiser.getOrganiserId();

        try {
            List<EventHomeDto> eventList = eventService.findEventsByOrganiserAndEnhancedStatus(organiserId, Boolean.FALSE);
            if (eventList.isEmpty()) {
                return ResponseEntity.ok(generateApiResponse(eventList, "No events found."));
            } else {
                return ResponseEntity.ok(generateApiResponse(eventList, "Not enhanced events successfully returned."));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(generateApiResponse(null, e.getMessage()));
        }
    }



    @PostMapping("/event")
    public ResponseEntity<GeneralApiResponse<Object>> saveEvent(@RequestParam("file") MultipartFile file,
                                                                @RequestParam("eventName") String eventName,
                                                                @RequestParam("eventDescription") String eventDescription,
                                                                @RequestParam("eventDate") LocalDateTime eventDate,
                                                                @RequestParam("otherEventInfo") String otherEventInfo,
                                                                @RequestParam("ticketSaleDate") LocalDateTime ticketSaleDate,
//                                                                @RequestParam("organiserId") Integer organiserId,
                                                                @RequestParam("venueId") Integer venueId,
                                                                @RequestParam("typeId") Integer typeId,
                                                                //comma separated string
                                                                @RequestParam("artistId") String artistIdString,
                                                                //comma separated string
                                                                @RequestParam("ticketPrices") String ticketPricesString,
                                                                @RequestParam("hasPresale") Boolean hasPresale,
                                                                @RequestParam("isEnhanced") Boolean isEnhanced,
                                                                @NonNull HttpServletRequest request) {
        String imageName;
        Event savedEvent;
        //Getting the Respective Objects for Organiser, Venue and Type and checking if it exists
        EventOrganiser eventOrganiser = retrieveOrganiserFromRequest(request);
        Venue venue = venueRepository.findById(venueId).orElse(null);
        EventType eventType = eventTypeRepository.findById(typeId).orElse(null);

        //artistIdString to artistId List
        List<Integer> artistIdList = Arrays.stream(artistIdString.split(","))
                .map(Integer::parseInt)
                .toList();
        //check that all artist is valid first
        for (Integer artistId : artistIdList) {
            if (artistRepository.findById(artistId).isEmpty()) {
                throw new NonExistentException(String.format("Artist with id %d does not exist, please try again", artistId));
            }
        }

        if (eventOrganiser == null) {
            throw new NonExistentException("Event Organiser does not exist");
        } else if (venue == null) {
            throw new NonExistentException("Venue does not exist");
        } else if (eventType == null) {
            throw new NonExistentException("Event Type does not exist");
        }

        //save event first to get the event id
        try {
            //save event first without image name to get the event id
            Event newEvent = new Event(null, eventName, eventDescription, eventDate, otherEventInfo, null,
                    ticketSaleDate, null, "pending", null, isEnhanced, hasPresale, false, eventOrganiser, venue, null, eventType, new HashSet<TicketPricing>(), new HashSet<Order>());
            savedEvent = eventService.saveEvent(newEvent);

            //generating the file name with the extension
            String fileExtension = getFileExtension(file.getContentType());
            imageName = savedEvent.getEventId() + fileExtension;

            //update event with image name and save to db again, IMAGE HAS NOT BEEN UPLOADED HERE
            savedEvent.setEventImage(imageName);
            eventService.saveEvent(savedEvent);

        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body(generateApiResponse(null, "DataIntegrityViolationException: Ticket sale date is earlier than event created date."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(generateApiResponse(null, e.getMessage()));
        }

        //uploading event image to s3 server
        try {
            amazonS3Service.uploadFile(file, imageName, "event_images");
            // delete event from db if got error saving image
        } catch (AmazonS3Exception e) {
            eventService.deleteEvent(savedEvent.getEventId());

            String errorCode = e.getErrorCode();
            if ("AccessDenied".equals(errorCode)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(generateApiResponse(null, "Access Denied to Amazon."));
            } else if ("NoSuchBucket".equals(errorCode)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(generateApiResponse(null, "S3 bucket not found."));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(generateApiResponse(null, "An error occurred during S3 interaction."));
            }
        } catch (Exception e) {
            eventService.deleteEvent(savedEvent.getEventId());
            return ResponseEntity.badRequest().body(generateApiResponse(null, e.getMessage()));
        }

        Integer eventId = savedEvent.getEventId();


        for (Integer artistId : artistIdList) {
            eventService.addArtistToEvent(artistId, eventId);
        }

        //adding ticket pricing for each cat
        List<Double> ticketPrices = Arrays.stream(ticketPricesString.split(","))
                .map(Double::parseDouble)
                .toList();

        if (ticketPrices.size() != 5) {
            throw new IllegalArgumentException("Ticket Prices should have 5 values");
        }

        eventService.addTicketCategory(1, eventId, ticketPrices.get(0));
        eventService.addTicketCategory(2, eventId, ticketPrices.get(1));
        eventService.addTicketCategory(3, eventId, ticketPrices.get(2));
        eventService.addTicketCategory(4, eventId, ticketPrices.get(3));
        eventService.addTicketCategory(5, eventId, ticketPrices.get(4));

        //setting all the fields for event
        OverallEventDto overallEventDto = eventDtoMapper.applyOverallEventDto(savedEvent);

        // Set presale to run 1 day before ticket sale date
        if (hasPresale) {
            LocalDateTime scheduledCheckTime = ticketSaleDate.minusHours(PRESALE_HOURS);
            taskScheduler.schedule(() -> presaleService.selectPresaleUsersForEvent(savedEvent),
                    Date.from(scheduledCheckTime.atZone(ZoneId.systemDefault()).toInstant()));
        }

        return ResponseEntity.status(201).body(generateApiResponse(overallEventDto, "Event created successfully."));
    }

    //without review, so basically targeted towards organiser
    @PutMapping("/event")
    public ResponseEntity<GeneralApiResponse<Object>> updateEvent(@RequestParam(value = "file", required = false) MultipartFile eventImageFile,
                                                                  @RequestParam(value = "eventId") Integer eventId,
                                                                  @RequestParam(value = "eventName", required = false) String eventName,
                                                                  @RequestParam(value = "eventDescription", required = false) String eventDescription,
                                                                  @RequestParam(value = "eventDate", required = false) LocalDateTime eventDate,
                                                                  @RequestParam(value = "eventLocation", required = false) String eventLocation,
                                                                  @RequestParam(value = "otherEventInfo", required = false) String otherEventInfo,
                                                                  @RequestParam(value = "ticketSaleDate", required = false) LocalDateTime ticketSaleDate,
                                                                  @RequestParam(value = "venueId", required = false) Integer venueId,
                                                                  @RequestParam(value = "typeId", required = false) Integer typeId,
                                                                  @RequestParam(value = "ticketPrices", required = false) String ticketPricesString,
                                                                  @NonNull HttpServletRequest request) {
        EventOrganiser organiser = retrieveOrganiserFromRequest(request);
        if (!eventRepository.existsEventByEventIdAndOrganiser(eventId, organiser)) {
            throw new IllegalArgumentException("Organiser is not allowed to update events created by other organisers.");
        }

        Venue venue = null;
        if (venueId != null) {
            Optional<Venue> venueOptional = venueRepository.findById(venueId);
            if (venueOptional.isPresent()) {
                venue = venueOptional.get();
            } else {
                throw new NonExistentException("Venue does not exist");
            }
        }

        EventType eventType = null;
        if (typeId != null) {
            Optional<EventType> eventTypeOptional = eventTypeRepository.findById(typeId);
            if (eventTypeOptional.isPresent()) {
                eventType = eventTypeOptional.get();
            } else {
                throw new NonExistentException("Event type does not exist");
            }
        }

        if (ticketPricesString != null) {
            List<Double> ticketPrices = Arrays.stream(ticketPricesString.split(","))
                    .map(Double::parseDouble)
                    .toList();

            if (ticketPrices.size() != 5) {
                throw new IllegalArgumentException("Ticket Prices should have 5 values");
            }
            eventService.updateTicketPricing(1, eventId, ticketPrices.get(0));
            eventService.updateTicketPricing(2, eventId, ticketPrices.get(1));
            eventService.updateTicketPricing(3, eventId, ticketPrices.get(2));
            eventService.updateTicketPricing(4, eventId, ticketPrices.get(3));
            eventService.updateTicketPricing(5, eventId, ticketPrices.get(4));
        }

        EventUpdateDto eventUpdateDto = new EventUpdateDto(eventId, eventName, eventDescription, eventDate, eventLocation, otherEventInfo, ticketSaleDate, venue, eventType, null, null, null);
        Event event = eventService.updateEvent(eventUpdateDto);
        //update event image if not null
        if (eventImageFile != null) {
            try {
                System.out.println(eventImageFile.getName());
                amazonS3Service.uploadFile(eventImageFile, event.getEventImage(), "event_images");
                // delete event from db if got error saving image
            } catch (AmazonS3Exception e) {
                String errorCode = e.getErrorCode();
                if ("AccessDenied".equals(errorCode)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(generateApiResponse(null, "Access Denied to Amazon."));
                } else if ("NoSuchBucket".equals(errorCode)) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(generateApiResponse(null, "S3 bucket not found."));
                } else {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(generateApiResponse(null, "An error occurred during S3 interaction: " + e.getMessage()));
                }
            }
        }
        return ResponseEntity.ok(generateApiResponse(event, "Event updated successfully."));
    }

    @PutMapping("/event/delete")
    public ResponseEntity<GeneralApiResponse<Object>> deleteEvent(@RequestParam("eventId") String eventIdString,
                                                                  @NonNull HttpServletRequest request) {
        // Check if deleteEvent is called by admin or event Organiser
        boolean isAdmin = isAdminRequest(request);
        EventOrganiser organiser = null;

        if (!isAdmin) {
            organiser = retrieveOrganiserFromRequest(request);
        }

        try {
            List<Integer> eventIdList = Arrays.stream(eventIdString.split(","))
                    .map(Integer::parseInt)
                    .toList();

            //check if all events exist first
            for (Integer eventId : eventIdList) {
                if (eventRepository.findById(eventId).isEmpty()) {
                    throw new NonExistentException(String.format("Event %d does not exist, deletion halted", eventId));
                }
                if (!isAdmin && !eventRepository.existsEventByEventIdAndOrganiser(eventId, organiser)) {
                    throw new IllegalArgumentException("No such event listed under organiser, deletion halted");
                }
            }

            StringBuilder results = new StringBuilder();

            for (Integer eventId : eventIdList) {
                results.append(eventService.deleteEvent(eventId)).append(" ");
            }

            return ResponseEntity.ok(generateApiResponse(null, results.toString()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(generateApiResponse(null, e.getMessage()));
        }
    }

    @PutMapping("/event/update-artist")
    public ResponseEntity<GeneralApiResponse> updateEventArtist(@RequestParam("artistIdString") String artistIdString,
                                                                @RequestParam("eventId") Integer eventId,
                                                                @NonNull HttpServletRequest request) {
        EventOrganiser eventOrganiser = retrieveOrganiserFromRequest(request);
        if (!eventRepository.existsEventByEventIdAndOrganiser(eventId, eventOrganiser)) {
            throw new IllegalArgumentException("Event organiser does not have an event with id " + eventId);
        }

        List<Integer> artistIdList = Arrays.stream(artistIdString.split(","))
                .map(Integer::parseInt)
                .toList();

        //check that all artist is valid first
        for (Integer artistId : artistIdList) {
            if (artistRepository.findById(artistId).isEmpty()) {
                throw new NonExistentException(String.format("Artist with id %d does not exist, please try again", artistId));
            }
        }

        eventService.removeAllArtistFromEvent(eventId);

        for (Integer artistId : artistIdList) {
            eventService.addArtistToEvent(artistId, eventId);
        }

        return ResponseEntity.ok(generateApiResponse(eventService.findArtistForEvent(eventId), String.format("Artist successfully assigned to event %d", eventId)));
    }

    @PostMapping("/event/featured")
    public ResponseEntity<GeneralApiResponse<Object>> saveFeaturedEvents(@RequestParam("eventId") Integer eventId,
                                                                         @RequestParam("startDate") LocalDateTime startDate,
                                                                         @RequestParam("endDate") LocalDateTime endDate,
                                                                         @NonNull HttpServletRequest request) {
        Event event = eventRepository.findById(eventId).orElse(null);
        Admin admin = retrieveAdminFromRequest(request);

        if (event == null) {
            throw new NonExistentException(String.format("No event of id %d found", eventId));
        } else if (admin == null) {
            throw new NonExistentException("Admin not found");
        } else if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("end date is earlier than start date");
        }
        try {
            FeaturedEvent featuredEvent = new FeaturedEvent(null, event, startDate, endDate, admin);
            return ResponseEntity.ok(generateApiResponse(eventService.saveFeaturedEvent(featuredEvent), "Featured Event Successfully Saved"));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body(generateApiResponse(null, "Featured event with event id could already exists"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(generateApiResponse(null, e.getMessage()));
        }

    }

    @GetMapping("/public/event/section-ticket-details/{eventId}")
    public ResponseEntity<GeneralApiResponse<Object>> findAllSectionsByEvent(
            @PathVariable("eventId") Integer eventId) {
        Event event = eventRepository.findById(eventId).orElse(null);

        checkIfEventExistsAndIsApprovedAndNotDeleted(eventId);

        List<SectionTicketDetailsDto> sectionDetailsForEvent = eventService.findAllSectionDetailsForEvent(event);

        return ResponseEntity.ok(generateApiResponse(sectionDetailsForEvent, String.format("Success returning all section ticket details for event %d", eventId)));
    }

    @GetMapping("/event/available")
    public ResponseEntity<GeneralApiResponse<Object>> eventHasTickets(@RequestParam("eventId") Integer eventId) {
        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null) {
            throw new NonExistentException("Event", eventId);
        }

        return ResponseEntity.ok(generateApiResponse(ticketService.getEventHasTickets(event), String.format("Success returning tickets available for event %d", eventId)));
    }

    @PutMapping("/event/interest")
    public ResponseEntity<GeneralApiResponse<Object>> userIndicateInterest(@RequestParam("eventId") Integer eventId,
                                                                           @NonNull HttpServletRequest request) {

        Optional<Event> eventOptional = eventRepository.findById(eventId);
        if (eventOptional.isEmpty()) {
            throw new NonExistentException("Event", eventId);
        }
        Event event = eventOptional.get();

        if (LocalDateTime.now().isAfter(event.getTicketSaleDate().minusHours(PRESALE_HOURS))) {
            throw new ApiRequestException("The presale interest indication period for event '" + event.getEventName() + "' has ended.");
        }

        User user = retrieveUserFromRequest(request);

        presaleService.setPresaleInterest(user, event, false, false);
        return ResponseEntity.status(201).body(generateApiResponse(null, "Presale interest recorded"));
    }

    @GetMapping("/event/presale-event")
    public ResponseEntity<GeneralApiResponse<Object>> isPresaleEvent(@RequestParam("eventId") Integer eventId) {
        Optional<Event> eventOptional = eventRepository.findById(eventId);
        if (eventOptional.isEmpty()) {
            throw new NonExistentException("Event", eventId);
        }

        return ResponseEntity.ok(generateApiResponse(eventOptional.get().getHasPresale(), "Returned presale status for event " + eventId));
    }


    @GetMapping("/event/presale-status")
    public ResponseEntity<GeneralApiResponse<Object>> checkPresaleStatus(@RequestParam("eventId") Integer eventId,
                                                                         @RequestParam("userId") Integer userId) {
        Optional<Event> eventOptional = eventRepository.findById(eventId);
        if (eventOptional.isEmpty()) {
            throw new NonExistentException("Event", eventId);
        }
        Event event = eventOptional.get();
        if (!event.getHasPresale()) {
            throw new IllegalArgumentException("Event '" + event.getEventName() + "' does not have a presale period");
        }

        Optional<User> userOptional = userRepository.findUserByUserId(userId);
        if (userOptional.isEmpty()) {
            throw new NonExistentException("User", userId);
        }
        User user = userOptional.get();

        return ResponseEntity.ok(generateApiResponse(presaleService.existsById(new EventUserId(user, event)), "Returned presale status for event id " + eventId + ", user id " + userId));
    }

    @GetMapping("/event/user-selected")
    public ResponseEntity<GeneralApiResponse<Object>> checkIfUserSelected(@RequestParam("eventId") Integer eventId,
//                                                                          @RequestParam("userId") Integer userId,
                                                                          @NonNull HttpServletRequest request) {
        Optional<Event> eventOptional = eventRepository.findById(eventId);
        if (eventOptional.isEmpty()) {
            throw new NonExistentException("Event", eventId);
        }
        Event event = eventOptional.get();
        if (!event.getHasPresale()) {
            throw new IllegalArgumentException("Event '" + event.getEventName() + "' does not have a presale period");
        }

        User user = retrieveUserFromRequest(request);

        Optional<PresaleInterest> presaleInterestOptional = presaleService.findPresaleInterestByID(new EventUserId(user, event));
        if (presaleInterestOptional.isPresent() && presaleInterestOptional.get().getIsSelected()) {
            return ResponseEntity.ok(generateApiResponse(true, "User " + user.getUserId() + " has been selected"));
        }

        return ResponseEntity.ok(generateApiResponse(false, "User " + user.getUserId() + " has not been selected"));
    }

    @GetMapping("/event/selected-users")
    public ResponseEntity<GeneralApiResponse<Object>> getEventSelectedUsers(@RequestParam("eventId") Integer eventId) {
        Optional<Event> eventOptional = eventRepository.findById(eventId);
        if (eventOptional.isEmpty()) {
            throw new NonExistentException("Event", eventId);
        }
        Event event = eventOptional.get();
        if (!event.getHasPresale()) {
            throw new IllegalArgumentException("Event '" + event.getEventName() + "' does not have a presale period");
        }
        if (!event.getHasPresaleUsers()) {
            throw new IllegalStateException("Users have yet to be selected");
        }

        return ResponseEntity.ok(generateApiResponse(presaleService.findUsersSelectedForEvent(event, true), "Returned list of users allowed in presale"));
    }

    @GetMapping("/event/purchaseable-tickets")
    public ResponseEntity<GeneralApiResponse<Object>> getNumberOfPurchaseableTickets(@RequestParam("eventId") Integer eventId,
                                                                                     @NonNull HttpServletRequest request) {
        User user = retrieveUserFromRequest(request);

        Optional<Event> eventOptional = eventRepository.findById(eventId);
        if (eventOptional.isEmpty()) {
            throw new NonExistentException("Event", eventId);
        }
        Event event = eventOptional.get();

        return ResponseEntity.ok(generateApiResponse(ticketService.getNumberOfTicketsPurchaseable(event, user), "Returned number of tickets user can purchase"));
    }

    @GetMapping("/event/queue-position")
    public ResponseEntity<GeneralApiResponse<Object>> getQueuePosition(
            @RequestParam("eventId") Integer eventId,
            @RequestParam("userId") Integer userId,
            @NonNull HttpServletRequest request) {
        User user = retrieveUserFromRequest(request);

        Optional<Event> eventOptional = eventRepository.findById(eventId);
        if (eventOptional.isEmpty()) {
            throw new NonExistentException("Event", eventId);
        }
        Event event = eventOptional.get();

        // can be removed after testing as userId will be derived
        if (userId != null) {
            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                throw new NonExistentException("Event", eventId);
            }

            return ResponseEntity.ok(generateApiResponse(queueService.getPosition(userOptional.get(), event), "Returned queue number"));
        }

        return ResponseEntity.ok(generateApiResponse(queueService.getPosition(user, event), "Returned queue number"));
    }

    @GetMapping("/event/queue-total")
    public ResponseEntity<GeneralApiResponse<Object>> getQueuePosition(@RequestParam("eventId") Integer eventId) {

        Optional<Event> eventOptional = eventRepository.findById(eventId);
        if (eventOptional.isEmpty()) {
            throw new NonExistentException("Event", eventId);
        }
        Event event = eventOptional.get();

        return ResponseEntity.ok(generateApiResponse(queueService.getTotalInQueue(event), "Returned number of users in queue"));
    }

    @PutMapping("/event/enter-queue")
    public ResponseEntity<GeneralApiResponse<Object>> enterQueue(
            @RequestParam("eventId") Integer eventId,
            @RequestParam("userId") Integer userId,
            @NonNull HttpServletRequest request) {
        User user = retrieveUserFromRequest(request);

        Optional<Event> eventOptional = eventRepository.findById(eventId);
        if (eventOptional.isEmpty()) {
            throw new NonExistentException("Event", eventId);
        }
        Event event = eventOptional.get();

        // can be removed after testing as userId will be derived
        if (userId != null) {
            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                throw new NonExistentException("Event", eventId);
            }

            queueService.addToQueue(userOptional.get(), event);
            return ResponseEntity.status(201).body(generateApiResponse(queueService.getPosition(userOptional.get(), event), "Added to queue and returned queue number"));
        }

        queueService.addToQueue(user, event);
        return ResponseEntity.status(201).body(generateApiResponse(queueService.getPosition(user, event), "Added to queue and returned queue number"));
    }

    @PutMapping("/event/leave-queue")
    public ResponseEntity<GeneralApiResponse<Object>> leaveQueue(
            @RequestParam("eventId") Integer eventId,
            @RequestParam("userId") Integer userId,
            @NonNull HttpServletRequest request) {
        User user = retrieveUserFromRequest(request);

        Optional<Event> eventOptional = eventRepository.findById(eventId);
        if (eventOptional.isEmpty()) {
            throw new NonExistentException("Event", eventId);
        }
        Event event = eventOptional.get();

        // can be removed after testing as userId will be derived
        if (userId != null) {
            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                throw new NonExistentException("Event", eventId);
            }

            queueService.removeFromQueue(userOptional.get(), event);
            return ResponseEntity.ok(generateApiResponse(null, "Removed from queue"));
        }

        queueService.removeFromQueue(user, event);
        return ResponseEntity.ok(generateApiResponse(null, "Removed from queue"));
    }
}
