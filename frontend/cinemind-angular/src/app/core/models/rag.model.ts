import { Movie } from './movie.model';

/** Mirrors the RAG Service RetrievedMovie record. */
export interface RagResult {
  movieId: number;
  title: string;
  overview: string | null;
  genres: string[];
  releaseYear: number | null;
  runtimeMinutes: number | null;
  rating: number | null;
  posterUrl: string | null;
  documentContent: string;
  similarityScore: number;
  modelName: string;
}

export interface RagSearchResponse {
  query: string;
  topK: number;
  candidateCount: number;
  retrievedCount: number;
  results: RagResult[];
}

export function ragResultToMovie(result: RagResult): Movie {
  return {
    id: result.movieId,
    title: result.title,
    overview: result.overview,
    genres: result.genres ?? [],
    releaseYear: result.releaseYear,
    runtimeMinutes: result.runtimeMinutes,
    keywords: [],
    rating: result.rating,
    posterUrl: result.posterUrl,
  };
}
