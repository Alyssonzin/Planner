package com.rocketseat.planner.trip;

import com.rocketseat.planner.activity.ActivityData;
import com.rocketseat.planner.activity.ActivityRequestPayload;
import com.rocketseat.planner.activity.ActivityResponse;
import com.rocketseat.planner.activity.ActivityService;
import com.rocketseat.planner.link.*;
import com.rocketseat.planner.participant.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/trips")
public class TripController {

    @Autowired
    private ParticipantService participantService;

    @Autowired
    ActivityService activityService;

    @Autowired
    LinkService linkService;

    @Autowired
    private TripRepository repository;

    @PostMapping //indica que o método abaixo é POST, e sua rota por padrão será "/trips"
    public ResponseEntity<TripCreateResponse> createTrip(@RequestBody TripRequestPayload payload) {
        Trip newTrip = new Trip(payload);

        this.repository.save(newTrip);
        this.participantService.registerParticipantsToEvent(payload.emailsToInvite(), newTrip);

        return ResponseEntity.ok(new TripCreateResponse(newTrip.getId()));
    }

    @GetMapping("/{id}") //retorna uma viagem pelo id
    public ResponseEntity<Trip> getTripDetails(@PathVariable UUID id){
        Optional<Trip> trip = this.repository.findById(id);

        return trip.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}") //Atualiza uma viagem pelo id. Seus parâmetros são o id na rota e uma trip no body"
    public ResponseEntity<Trip> updateTrip(@PathVariable UUID id, @RequestBody TripRequestPayload payload){
        Optional<Trip> trip = this.repository.findById(id);

        //Se existe uma viagem com o id especificado
        if(trip.isPresent()) {
            Trip rawTrip = trip.get();

            rawTrip.setDestination(payload.destination());
            rawTrip.setStartsAt(LocalDateTime.parse(payload.startsAt(), DateTimeFormatter.ISO_DATE_TIME));
            rawTrip.setEndsAt(LocalDateTime.parse(payload.endsAt(), DateTimeFormatter.ISO_DATE_TIME));

            this.repository.save(rawTrip);

            return ResponseEntity.ok(rawTrip);
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/confirm")
    public ResponseEntity<Trip> confirmTrip(@PathVariable UUID id) {
        Optional<Trip> trip = this.repository.findById(id);

        if(trip.isPresent()) {
            Trip rawTrip = trip.get();

            rawTrip.setIsConfirmed(true);
            this.repository.save(rawTrip);
            this.participantService.triggerConfirmationEmailtoParticipants(id);

            return ResponseEntity.ok(rawTrip);
        }

        return ResponseEntity.notFound().build();
    }

    // =================== PARTICIPANTS =================
    @PostMapping("/{id}/invite")
    public ResponseEntity<ParticipantCreateResponse> inviteParticipant(@PathVariable UUID id, @RequestBody ParticipantRequestPayload payload) {
        Optional<Trip> trip = this.repository.findById(id);

        if(trip.isPresent()) {
            Trip rawTrip = trip.get();

            List<String> participantToInvite = new ArrayList<>();
            participantToInvite.add(payload.email());

            //Cria novo participante
            ParticipantCreateResponse participantResponse = this.participantService.registerParticipantToEvent(payload.email(), rawTrip);

            //Se a viagem está confirmada
            if(rawTrip.getIsConfirmed()){
                this.participantService.triggerConfirmationEmailtoParticipant(payload.email());
            }

            return ResponseEntity.ok(participantResponse);
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/participants")
    public ResponseEntity<List<ParticipantData>> getAllParticipants(@PathVariable UUID id) {
        List<ParticipantData> participantList = this.participantService.getAllParticipantsFromEvent(id);

        return ResponseEntity.ok(participantList);
    }

    // =================== ACTIVITIES ===================

    @PostMapping("/{id}/activities")
    public ResponseEntity<ActivityResponse> registerActivity(@PathVariable UUID id, @RequestBody ActivityRequestPayload payload) {
        Optional<Trip> trip = this.repository.findById(id);

        if(trip.isPresent()) {
            Trip rawTrip = trip.get();

            ActivityResponse activityResponse = activityService.registerActivity(payload, rawTrip);
            return ResponseEntity.ok(activityResponse);
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/activities")
    public ResponseEntity<List<ActivityData>> getAllActivities(@PathVariable UUID id) {
        List<ActivityData> activitiesDataList = this.activityService.getAllActivitiesFromId(id);

        return ResponseEntity.ok(activitiesDataList);
    }

    // =================== LINKS =========================

    @PostMapping("/{id}/links")
    public ResponseEntity<LinkResponse> registerLink(@PathVariable UUID id, @RequestBody LinkRequestPayload payload) {
        Optional<Trip> trip = this.repository.findById(id);

        if(trip.isPresent()) {
            Trip rawTrip = trip.get();

            LinkResponse linkResponse = linkService.registerLink(payload, rawTrip);
            return ResponseEntity.ok(linkResponse);
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/links")
    public ResponseEntity<List<LinkData>> getAllLinks(@PathVariable UUID id) {
        List<LinkData> LinkDataList = this.linkService.getAllLinksFromTripId(id);

        return ResponseEntity.ok(LinkDataList);
    }

}
