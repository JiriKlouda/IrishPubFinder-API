package com.irishpubfinder.api.service;

import com.irishpubfinder.api.dto.BadgeDto;
import com.irishpubfinder.api.model.BadgeEvent;
import com.irishpubfinder.api.repository.BadgeEventRepository;
import com.irishpubfinder.api.repository.FriendshipRepository;
import com.irishpubfinder.api.repository.GuinnessReviewRepository;
import com.irishpubfinder.api.repository.VisitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class BadgeService {

    private record BadgeDef(String id, String name, String description, String icon, String color, String category, int target, boolean pro) {}

    private static final List<BadgeDef> BADGE_DEFS = List.of(
        // Pub Explorer
        new BadgeDef("first_pint",      "First Pint",       "Visit your first Irish pub",                     "map-marker-alt", "#27ae60", "visits",     1,   false),
        new BadgeDef("pub_crawler",     "Pub Crawler",      "Visit 5 Irish pubs",                             "route",          "#f59e0b", "visits",     5,   false),
        new BadgeDef("the_regular",     "The Regular",      "Visit 10 Irish pubs",                            "home",           "#f59e0b", "visits",     10,  false),
        new BadgeDef("dedicated",       "Dedicated",        "Visit 25 Irish pubs",                            "fire",           "#ef4444", "visits",     25,  false),
        new BadgeDef("legend",          "Legend",           "Visit 50 Irish pubs",                            "crown",          "#f59e0b", "visits",     50,  false),
        new BadgeDef("immortal",        "Immortal",         "Visit 100 Irish pubs",                           "star",           "#8b5cf6", "visits",     100, false),
        // World Explorer
        new BadgeDef("on_the_map",      "On the Map",       "Visit a pub in your first country",              "globe",          "#3b82f6", "explorer",   1,   false),
        new BadgeDef("explorer",        "Explorer",         "Visit pubs in 3 different countries",            "compass",        "#3b82f6", "explorer",   3,   false),
        new BadgeDef("globetrotter",    "Globetrotter",     "Visit pubs in 5 different countries",            "plane",          "#06b6d4", "explorer",   5,   false),
        new BadgeDef("world_tour",      "World Tour",       "Visit pubs in 10 different countries",           "rocket",         "#8b5cf6", "explorer",   10,  false),
        new BadgeDef("quarter_century", "Quarter Century",  "Visit pubs in 25 different countries",           "globe-americas", "#06b6d4", "explorer",   25,  false),
        new BadgeDef("half_century",    "Half Century",     "Visit pubs in 50 different countries",           "satellite",      "#ec4899", "explorer",   50,  false),
        // Guinness Connoisseur
        new BadgeDef("first_drop",      "First Drop",       "Rate your first Guinness",                       "star",           "#f1c40f", "connoisseur", 1,  false),
        new BadgeDef("taster",          "Taster",           "Rate 5 Guinness pints",                          "award",          "#f1c40f", "connoisseur", 5,  false),
        new BadgeDef("connoisseur",     "Connoisseur",      "Rate 25 Guinness pints",                         "medal",          "#f59e0b", "connoisseur", 25, false),
        new BadgeDef("master_taster",   "Master Taster",    "Rate 50 Guinness pints",                         "crown",          "#ef4444", "connoisseur", 50, false),
        // Social Butterfly
        new BadgeDef("social_animal",   "Social Animal",    "Add your first friend",                          "user-plus",      "#a78bfa", "social",     1,   false),
        new BadgeDef("pub_gang",        "Pub Gang",         "Have 5 friends on IrishPubFinder",               "users",          "#a78bfa", "social",     5,   false),
        new BadgeDef("the_crew",        "The Crew",         "Have 10 friends on IrishPubFinder",              "user-friends",   "#ec4899", "social",     10,  false),
        // Irish Counties
        new BadgeDef("county_crawler",  "County Crawler",   "Visit a pub in your first Irish county",         "map",            "#34d399", "counties",   1,   false),
        new BadgeDef("county_regular",  "County Regular",   "Visit pubs in 5 different Irish counties",       "road",           "#10b981", "counties",   5,   false),
        new BadgeDef("county_champion", "County Champion",  "Visit pubs in 26 counties (Republic of Ireland)","flag",           "#059669", "counties",   26,  false),
        new BadgeDef("all_32",          "All 32",           "Visit pubs in all 32 Irish counties",            "medal",          "#047857", "counties",   32,  false),
        // Continent Explorer
        new BadgeDef("local_explorer",   "Local Explorer",   "Visit pubs on your first continent",            "globe",          "#818cf8", "continents", 1,   false),
        new BadgeDef("continent_hopper", "Continent Hopper", "Visit pubs on 3 different continents",          "globe-americas", "#6366f1", "continents", 3,   false),
        new BadgeDef("world_wanderer",   "World Wanderer",   "Visit pubs on all 7 continents",                "satellite",      "#4f46e5", "continents", 7,   false),
        // American Trailblazer (Pro)
        new BadgeDef("state_visitor",   "State Visitor",    "Visit a pub in your first US state",             "flag-usa",       "#f87171", "us_states",  1,   true),
        new BadgeDef("road_tripper",    "Road Tripper",     "Visit pubs in 10 different US states",           "route",          "#ef4444", "us_states",  10,  true),
        new BadgeDef("coastie",         "Coastie",          "Visit pubs in 25 different US states",           "car",            "#dc2626", "us_states",  25,  true),
        new BadgeDef("all_american",    "All American",     "Visit pubs in all 50 US states",                 "star",           "#b91c1c", "us_states",  50,  true),
        // City Hopper (Pro)
        new BadgeDef("city_slicker",    "City Slicker",     "Visit a pub in your first city",                 "city",           "#fbbf24", "cities",     1,   true),
        new BadgeDef("city_explorer",   "City Explorer",    "Visit pubs in 10 different cities",              "store",          "#f59e0b", "cities",     10,  true),
        new BadgeDef("metropolitan",    "Metropolitan",     "Visit pubs in 25 different cities",              "landmark",       "#d97706", "cities",     25,  true),
        new BadgeDef("cosmopolitan",    "Cosmopolitan",     "Visit pubs in 50 different cities",              "map-pin",        "#b45309", "cities",     50,  true)
    );

    private static final Map<String, String> COUNTRY_TO_CONTINENT;
    static {
        COUNTRY_TO_CONTINENT = new HashMap<>();
        for (String c : new String[]{"IE","GB","FR","DE","ES","IT","NL","BE","PT","AT","CH","SE","NO","DK","FI","PL","CZ","HU","RO","BG","HR","SI","SK","EE","LV","LT","GR","CY","MT","LU","IS","ME","RS","BA","MK","AL","UA","MD","BY","RU","GE","AM","AZ","TR","AD","MC","SM","LI","VA","XK"})
            COUNTRY_TO_CONTINENT.put(c, "EU");
        for (String c : new String[]{"US","CA","MX","GT","BZ","HN","SV","NI","CR","PA","CU","JM","HT","DO","TT","BB","BS","PR","TC","KY","VG","VI","AW","CW","SX","DM","LC","VC","GD","AG","KN"})
            COUNTRY_TO_CONTINENT.put(c, "NA");
        for (String c : new String[]{"BR","AR","CL","CO","PE","VE","EC","BO","PY","UY","GY","SR","GF","FK"})
            COUNTRY_TO_CONTINENT.put(c, "SA");
        for (String c : new String[]{"CN","JP","KR","IN","TH","VN","SG","MY","ID","PH","HK","TW","AE","SA","IL","JO","LB","IQ","IR","PK","AF","BD","LK","NP","BT","MV","KW","QA","BH","OM","YE","KZ","UZ","TM","TJ","KG","MN","MM","KH","LA","SY","PS"})
            COUNTRY_TO_CONTINENT.put(c, "AS");
        for (String c : new String[]{"NG","ZA","EG","ET","GH","KE","TZ","MA","DZ","TN","LY","CM","CI","SN","ZM","ZW","UG","RW","MZ","AO","MG","MW","ML","MR","MU","BJ","BF","BI","CV","CF","TD","CG","CD","DJ","GA","GN","GW","LS","LR","NE","ST","SC","SL","SO","SS","SD","SZ","TG","ER"})
            COUNTRY_TO_CONTINENT.put(c, "AF");
        for (String c : new String[]{"AU","NZ","FJ","PG","SB","VU","WS","TO","KI","FM","MH","PW","NR","TV","CK"})
            COUNTRY_TO_CONTINENT.put(c, "OC");
        COUNTRY_TO_CONTINENT.put("AQ", "AN");
    }

    private final VisitRepository visitRepository;
    private final GuinnessReviewRepository guinnessReviewRepository;
    private final FriendshipRepository friendshipRepository;
    private final BadgeEventRepository badgeEventRepository;

    public BadgeService(VisitRepository visitRepository,
                        GuinnessReviewRepository guinnessReviewRepository,
                        FriendshipRepository friendshipRepository,
                        BadgeEventRepository badgeEventRepository) {
        this.visitRepository = visitRepository;
        this.guinnessReviewRepository = guinnessReviewRepository;
        this.friendshipRepository = friendshipRepository;
        this.badgeEventRepository = badgeEventRepository;
    }

    private int[] computeProgressCounts(String userId) {
        int visitCount    = (int) visitRepository.countByUserId(userId);
        int countryCount  = (int) visitRepository.countDistinctCountriesByUserId(userId);
        int guinnessCount = (int) guinnessReviewRepository.countByUserId(userId);
        int friendCount   = friendshipRepository.findAcceptedFriendships(userId).size();
        int continentCount = (int) visitRepository.findDistinctCountryCodesByUserId(userId).stream()
            .map(code -> COUNTRY_TO_CONTINENT.get(code.toUpperCase()))
            .filter(Objects::nonNull)
            .distinct()
            .count();
        // County, state, city counts require geo-enrichment (not yet stored in visits table)
        return new int[]{visitCount, countryCount, guinnessCount, friendCount, continentCount, 0, 0, 0};
    }

    private int progressForDef(BadgeDef def, int[] counts) {
        return switch (def.category()) {
            case "visits"      -> counts[0];
            case "explorer"    -> counts[1];
            case "connoisseur" -> counts[2];
            case "social"      -> counts[3];
            case "continents"  -> counts[4];
            case "counties"    -> counts[5];
            case "us_states"   -> counts[6];
            case "cities"      -> counts[7];
            default            -> 0;
        };
    }

    @Transactional
    public void checkAndRecordNewBadges(String userId) {
        int[] counts = computeProgressCounts(userId);
        for (BadgeDef def : BADGE_DEFS) {
            int progress = progressForDef(def, counts);
            if (progress >= def.target() && !badgeEventRepository.existsByUserIdAndBadgeId(userId, def.id())) {
                badgeEventRepository.save(BadgeEvent.builder()
                    .userId(userId)
                    .badgeId(def.id())
                    .badgeName(def.name())
                    .badgeDescription(def.description())
                    .badgeIcon(def.icon())
                    .badgeColor(def.color())
                    .badgeCategory(def.category())
                    .build());
            }
        }
    }

    public List<BadgeDto> getBadges(String userId) {
        int[] counts = computeProgressCounts(userId);

        // Backfill any earned badges not yet recorded (e.g. before feature was deployed)
        for (BadgeDef def : BADGE_DEFS) {
            int progress = progressForDef(def, counts);
            if (progress >= def.target() && !badgeEventRepository.existsByUserIdAndBadgeId(userId, def.id())) {
                badgeEventRepository.save(BadgeEvent.builder()
                    .userId(userId)
                    .badgeId(def.id())
                    .badgeName(def.name())
                    .badgeDescription(def.description())
                    .badgeIcon(def.icon())
                    .badgeColor(def.color())
                    .badgeCategory(def.category())
                    .build());
            }
        }

        return BADGE_DEFS.stream().map(def -> {
            int progress = progressForDef(def, counts);
            return new BadgeDto(
                def.id(), def.name(), def.description(),
                def.icon(), def.color(), def.category(),
                progress >= def.target(),
                Math.min(progress, def.target()),
                def.target(),
                def.pro()
            );
        }).collect(Collectors.toList());
    }
}
