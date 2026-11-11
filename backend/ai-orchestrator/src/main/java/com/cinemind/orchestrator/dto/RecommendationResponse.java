package com.cinemind.orchestrator.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Structured output of the Recommendation Specialist (Agent 2).
 * The orchestrator validates every movieId against the retrieved context and
 * replaces title/releaseYear with the real retrieved metadata, so anything the
 * model invents is discarded.
 */
public class RecommendationResponse {

    private List<RecommendationItem> recommendations = new ArrayList<>();

    public List<RecommendationItem> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<RecommendationItem> recommendations) {
        this.recommendations = recommendations == null ? new ArrayList<>() : recommendations;
    }

    public static class RecommendationItem {

        private Long movieId;
        private String title;
        private Integer releaseYear;
        private Integer matchScore;
        private String reason;

        public Long getMovieId() {
            return movieId;
        }

        public void setMovieId(Long movieId) {
            this.movieId = movieId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Integer getReleaseYear() {
            return releaseYear;
        }

        public void setReleaseYear(Integer releaseYear) {
            this.releaseYear = releaseYear;
        }

        public Integer getMatchScore() {
            return matchScore;
        }

        public void setMatchScore(Integer matchScore) {
            this.matchScore = matchScore;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
