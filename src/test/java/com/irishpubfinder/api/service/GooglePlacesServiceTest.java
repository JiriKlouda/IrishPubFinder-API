package com.irishpubfinder.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the pure pagination helpers of GooglePlacesService (no Google / DB).
 * Lives in the service package to reach the package-private helpers.
 */
class GooglePlacesServiceTest {

    // Repos/metrics are unused by the helpers under test, so null dependencies are fine.
    private final GooglePlacesService service = new GooglePlacesService(null, null, null);
    private final ObjectMapper mapper = new ObjectMapper();

    /** Builds a Google-style page JSON with the given place ids and an optional nextPageToken. */
    private static String page(String token, String... ids) {
        StringBuilder sb = new StringBuilder("{\"places\":[");
        for (int i = 0; i < ids.length; i++) {
            if (i > 0) sb.append(',');
            sb.append("{\"id\":\"").append(ids[i]).append("\"}");
        }
        sb.append(']');
        if (token != null) sb.append(",\"nextPageToken\":\"").append(token).append('"');
        sb.append('}');
        return sb.toString();
    }

    /** Builds a combined cached set of {@code n} places: ids p0..p(n-1), no token. */
    private String combined(int n) {
        String[] ids = new String[n];
        for (int i = 0; i < n; i++) ids[i] = "p" + i;
        return service.mergePages(List.of(page(null, ids)));
    }

    @Test
    void mergePages_concatenatesPlaces_andDropsToken() throws Exception {
        String merged = service.mergePages(List.of(
                page("tok1", "a", "b"),
                page("tok2", "c", "d"),
                page(null, "e")));

        JsonNode root = mapper.readTree(merged);
        assertThat(root.get("places")).hasSize(5);
        assertThat(root.has("nextPageToken")).isFalse();
        assertThat(root.get("places").get(0).get("id").asText()).isEqualTo("a");
        assertThat(root.get("places").get(4).get("id").asText()).isEqualTo("e");
    }

    @Test
    void mergePages_skipsUnparseablePages() throws Exception {
        String merged = service.mergePages(List.of(page(null, "a"), "not-json", page(null, "b")));
        assertThat(mapper.readTree(merged).get("places")).hasSize(2);
    }

    @Test
    void sliceResponse_firstPage_returns20WithNextToken() throws Exception {
        JsonNode out = mapper.readTree(service.sliceResponse(combined(45), "v3:1,2,80467", 0));

        assertThat(out.get("places")).hasSize(20);
        assertThat(out.get("places").get(0).get("id").asText()).isEqualTo("p0");
        assertThat(out.has("nextPageToken")).isTrue();
        assertThat(GooglePlacesService.parseSyntheticToken(out.get("nextPageToken").asText()).offset())
                .isEqualTo(20);
    }

    @Test
    void sliceResponse_middlePage_returns20WithNextToken() throws Exception {
        JsonNode out = mapper.readTree(service.sliceResponse(combined(45), "v3:1,2,80467", 20));

        assertThat(out.get("places")).hasSize(20);
        assertThat(out.get("places").get(0).get("id").asText()).isEqualTo("p20");
        assertThat(GooglePlacesService.parseSyntheticToken(out.get("nextPageToken").asText()).offset())
                .isEqualTo(40);
    }

    @Test
    void sliceResponse_lastPartialPage_hasNoToken() throws Exception {
        JsonNode out = mapper.readTree(service.sliceResponse(combined(45), "v3:1,2,80467", 40));

        assertThat(out.get("places")).hasSize(5); // 40..44
        assertThat(out.has("nextPageToken")).isFalse();
    }

    @Test
    void sliceResponse_offsetBeyondEnd_returnsEmptyNoToken() throws Exception {
        JsonNode out = mapper.readTree(service.sliceResponse(combined(45), "v3:1,2,80467", 60));

        assertThat(out.get("places")).isEmpty();
        assertThat(out.has("nextPageToken")).isFalse();
    }

    @Test
    void sliceResponse_exactlyFull_doesNotAdvertiseEmptyNextPage() throws Exception {
        JsonNode out = mapper.readTree(service.sliceResponse(combined(40), "v3:1,2,80467", 20));

        assertThat(out.get("places")).hasSize(20); // 20..39
        assertThat(out.has("nextPageToken")).isFalse();
    }

    @Test
    void syntheticToken_roundTrips_withSpecialCharsInCellKey() {
        String cellKey = "v3:-12,34,80467";
        String token = GooglePlacesService.buildSyntheticToken(cellKey, 40);

        GooglePlacesService.SyntheticToken parsed = GooglePlacesService.parseSyntheticToken(token);
        assertThat(parsed).isNotNull();
        assertThat(parsed.cellKey()).isEqualTo(cellKey);
        assertThat(parsed.offset()).isEqualTo(40);
    }

    @Test
    void parseSyntheticToken_rejectsRealGoogleToken() {
        // Sample of the opaque token shape Google returns — must not be treated as synthetic.
        String googleToken = "AeJbb3eQ-rNotARealTokenButLooksLikeOne_1234567890abcXYZ";
        assertThat(GooglePlacesService.parseSyntheticToken(googleToken)).isNull();
    }

    @Test
    void parseSyntheticToken_rejectsMalformedSyntheticTokens() {
        assertThat(GooglePlacesService.parseSyntheticToken(null)).isNull();
        assertThat(GooglePlacesService.parseSyntheticToken("nb1.onlytwoparts")).isNull();
        assertThat(GooglePlacesService.parseSyntheticToken("nb1.dmFsaWQ.notanumber")).isNull();
        assertThat(GooglePlacesService.parseSyntheticToken("nb1.dmFsaWQ.-5")).isNull();
    }
}
