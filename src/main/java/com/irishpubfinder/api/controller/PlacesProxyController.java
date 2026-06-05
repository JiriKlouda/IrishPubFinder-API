package com.irishpubfinder.api.controller;

import com.irishpubfinder.api.service.GooglePlacesService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/places")
public class PlacesProxyController {

    private final GooglePlacesService placesService;

    public PlacesProxyController(GooglePlacesService placesService) {
        this.placesService = placesService;
    }

    @GetMapping(value = "/nearby", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> nearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam int radius,
            @RequestParam(required = false) String pageToken) {
        return ResponseEntity.ok(placesService.getNearbyPubs(lat, lng, radius, pageToken));
    }

    @GetMapping(value = "/details/{placeId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> details(
            @PathVariable String placeId,
            @RequestParam(required = false) String sessionToken) {
        return ResponseEntity.ok(placesService.getPlaceDetails(placeId, sessionToken));
    }

    @GetMapping(value = "/autocomplete", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> autocomplete(
            @RequestParam String input,
            @RequestParam(required = false) String sessionToken) {
        return ResponseEntity.ok(placesService.getAutocomplete(input, sessionToken));
    }
}
