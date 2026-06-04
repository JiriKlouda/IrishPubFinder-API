package com.irishpubfinder.api.service;

import com.irishpubfinder.api.dto.BadgeDto;
import com.irishpubfinder.api.repository.FriendshipRepository;
import com.irishpubfinder.api.repository.GuinnessReviewRepository;
import com.irishpubfinder.api.repository.VisitRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BadgeService {

    private record BadgeDef(String id, String name, String description, String icon, String color, String category, int target) {}

    private static final List<BadgeDef> BADGE_DEFS = List.of(
        // Pub Explorer
        new BadgeDef("first_pint",   "First Pint",    "Visit your first Irish pub",       "map-marker-alt", "#27ae60", "visits", 1),
        new BadgeDef("pub_crawler",  "Pub Crawler",   "Visit 5 Irish pubs",               "route",          "#f59e0b", "visits", 5),
        new BadgeDef("the_regular",  "The Regular",   "Visit 10 Irish pubs",              "home",           "#f59e0b", "visits", 10),
        new BadgeDef("dedicated",    "Dedicated",     "Visit 25 Irish pubs",              "fire",           "#ef4444", "visits", 25),
        new BadgeDef("legend",       "Legend",        "Visit 50 Irish pubs",              "crown",          "#f59e0b", "visits", 50),
        new BadgeDef("immortal",     "Immortal",      "Visit 100 Irish pubs",             "star",           "#8b5cf6", "visits", 100),
        // World Explorer
        new BadgeDef("on_the_map",    "On the Map",   "Visit a pub in your first country",  "globe",         "#3b82f6", "explorer", 1),
        new BadgeDef("explorer",      "Explorer",     "Visit pubs in 3 different countries","compass",       "#3b82f6", "explorer", 3),
        new BadgeDef("globetrotter",  "Globetrotter", "Visit pubs in 5 different countries","plane",         "#06b6d4", "explorer", 5),
        new BadgeDef("world_tour",      "World Tour",     "Visit pubs in 10 different countries", "rocket",        "#8b5cf6", "explorer", 10),
        new BadgeDef("quarter_century","Quarter Century","Visit pubs in 25 different countries", "globe-americas","#06b6d4", "explorer", 25),
        new BadgeDef("half_century",   "Half Century",   "Visit pubs in 50 different countries", "satellite",     "#ec4899", "explorer", 50),
        // Guinness Connoisseur
        new BadgeDef("first_drop",    "First Drop",   "Rate your first Guinness",           "star",          "#f1c40f", "connoisseur", 1),
        new BadgeDef("taster",        "Taster",       "Rate 5 Guinness pints",              "award",         "#f1c40f", "connoisseur", 5),
        new BadgeDef("connoisseur",   "Connoisseur",  "Rate 25 Guinness pints",             "medal",         "#f59e0b", "connoisseur", 25),
        new BadgeDef("master_taster", "Master Taster","Rate 50 Guinness pints",             "crown",         "#ef4444", "connoisseur", 50),
        // Social Butterfly
        new BadgeDef("social_animal", "Social Animal","Add your first friend",              "user-plus",     "#a78bfa", "social", 1),
        new BadgeDef("pub_gang",      "Pub Gang",     "Have 5 friends on IrishPubFinder",   "users",         "#a78bfa", "social", 5),
        new BadgeDef("the_crew",      "The Crew",     "Have 10 friends on IrishPubFinder",  "user-friends",  "#ec4899", "social", 10)
    );

    private final VisitRepository visitRepository;
    private final GuinnessReviewRepository guinnessReviewRepository;
    private final FriendshipRepository friendshipRepository;

    public BadgeService(VisitRepository visitRepository,
                        GuinnessReviewRepository guinnessReviewRepository,
                        FriendshipRepository friendshipRepository) {
        this.visitRepository = visitRepository;
        this.guinnessReviewRepository = guinnessReviewRepository;
        this.friendshipRepository = friendshipRepository;
    }

    public List<BadgeDto> getBadges(String userId) {
        int visitCount    = (int) visitRepository.countByUserId(userId);
        int countryCount  = (int) visitRepository.countDistinctCountriesByUserId(userId);
        int guinnessCount = (int) guinnessReviewRepository.countByUserId(userId);
        int friendCount   = friendshipRepository.findAcceptedFriendships(userId).size();

        return BADGE_DEFS.stream().map(def -> {
            int progress = switch (def.category()) {
                case "visits"      -> visitCount;
                case "explorer"    -> countryCount;
                case "connoisseur" -> guinnessCount;
                case "social"      -> friendCount;
                default            -> 0;
            };
            return new BadgeDto(
                def.id(), def.name(), def.description(),
                def.icon(), def.color(), def.category(),
                progress >= def.target(),
                Math.min(progress, def.target()),
                def.target()
            );
        }).collect(Collectors.toList());
    }
}
