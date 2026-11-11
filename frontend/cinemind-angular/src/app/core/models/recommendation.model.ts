import { Movie } from './movie.model';

/** Mirrors the AI Orchestrator DTOs. Every value comes from the backend. */
export interface MoviePreferences {
  genres: string[];
  mood: string;
  similarTo: string[];
  releaseYearMin: number | null;
  maxRuntime: number | null;
  excludedGenres: string[];
}

export interface RecommendedMovie {
  movieId: number;
  title: string;
  releaseYear: number | null;
  matchScore: number;
  reason: string;
}

export interface AgentTrace {
  preferenceAgent: string;
  ragRetrieval: string;
  recommendationAgent: string;
  retrievedDocuments: number;
  candidatesAfterFilters: number;
}

export interface RecommendationResult {
  query: string;
  maxResults: number;
  retrievedDocuments: number;
  preferences: MoviePreferences;
  recommendations: RecommendedMovie[];
  agentTrace: AgentTrace;
}

export interface RecommendationRequest {
  query: string;
  maxResults: number;
}

/**
 * A recommendation paired with its real Movie Service metadata (genres,
 * rating, poster). The orchestrator only returns ids/titles/years, so the
 * frontend enriches them via GET /api/movies/{id}.
 */
export interface EnrichedRecommendation {
  recommendation: RecommendedMovie;
  movie: Movie;
}

export interface OrchestratorHealth {
  status: string;
  llmModel: string;
  geminiConfigured: boolean;
  maxGeminiCallsPerRequest: number;
}
