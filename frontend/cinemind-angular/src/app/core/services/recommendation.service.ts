import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Movie } from '../models/movie.model';
import {
  OrchestratorHealth,
  RecommendationRequest,
  RecommendationResult,
} from '../models/recommendation.model';
import { RagSearchResponse, ragResultToMovie } from '../models/rag.model';

export interface SimilarSource {
  id: number;
  title: string;
}

/** Recommendations + RAG-based similar movies, all via the API Gateway. */
@Injectable({ providedIn: 'root' })
export class RecommendationService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiBaseUrl;

  recommend(query: string, maxResults = 5): Observable<RecommendationResult> {
    const body: RecommendationRequest = { query, maxResults };
    return this.http.post<RecommendationResult>(
      `${this.baseUrl}/api/recommendations`,
      body,
    );
  }

  health(): Observable<OrchestratorHealth> {
    return this.http.get<OrchestratorHealth>(
      `${this.baseUrl}/api/recommendations/health`,
    );
  }

  /** Uses the real RAG Service search to find movies similar to a given one. */
  findSimilar(source: SimilarSource, limit = 4): Observable<Movie[]> {
    return this.http
      .post<RagSearchResponse>(`${this.baseUrl}/api/rag/search`, {
        query: `Movies similar to ${source.title}`,
        topK: limit + 1,
        minScore: 0,
        filters: {},
      })
      .pipe(
        map((response) =>
          (response.results ?? [])
            .filter((result) => result.movieId !== source.id)
            .slice(0, limit)
            .map(ragResultToMovie),
        ),
      );
  }
}
