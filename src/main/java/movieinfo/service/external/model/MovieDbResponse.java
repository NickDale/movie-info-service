package movieinfo.service.external.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;

import java.time.LocalDate;
import java.util.List;

public class MovieDbResponse {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SearchResponse(List<MovieResult> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MovieResult(long id, String title,
                              @JsonProperty("release_date")
                              @JsonDeserialize(using = LocalDateDeserializer.class)
                              @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate releaseDate) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DetailsResponse(List<CrewMember> crew) implements Details {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CrewMember(String name, String job) {
    }
}
