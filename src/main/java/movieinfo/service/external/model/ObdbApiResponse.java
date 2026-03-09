package movieinfo.service.external.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ObdbApiResponse {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SearchResponse(@JsonProperty("Search") List<SearchResult> search,
                                 @JsonProperty("Response") String response) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SearchResult(
            @JsonProperty("imdbID") String imdbId,
            @JsonProperty("Title") String title,
            @JsonProperty("Year") String year) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DetailResponse(
            @JsonProperty("Title") String title,
            @JsonProperty("Year") String year,
            @JsonProperty("Director") String director,
            @JsonProperty("Response") String response
    ) implements Details {
    }
}
