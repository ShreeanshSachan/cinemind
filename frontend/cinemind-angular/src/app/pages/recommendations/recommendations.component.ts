import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { catchError, forkJoin, map, of } from 'rxjs';

import { toUserMessage } from '../../core/api-error';
import { Movie } from '../../core/models/movie.model';
import {
  EnrichedRecommendation,
  RecommendationResult,
  RecommendedMovie,
} from '../../core/models/recommendation.model';
import { MovieService } from '../../core/services/movie.service';
import { RecommendationService } from '../../core/services/recommendation.service';
import { RecommendationStateService } from '../../core/services/recommendation-state.service';
import { AgentTraceComponent } from '../../shared/agent-trace.component';
import { ErrorComponent } from '../../shared/error.component';
import { LoadingComponent } from '../../shared/loading.component';
import { MovieCardComponent } from '../../shared/movie-card.component';
import { RecommendationListComponent } from '../../shared/recommendation-list.component';

@Component({
  selector: 'app-recommendations',
  imports: [
    RouterLink,
    LoadingComponent,
    ErrorComponent,
    MovieCardComponent,
    RecommendationListComponent,
    AgentTraceComponent,
  ],
  templateUrl: './recommendations.component.html',
  styleUrl: './recommendations.component.css',
})
export class RecommendationsComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly recommendationService = inject(RecommendationService);
  private readonly movieService = inject(MovieService);
  private readonly state = inject(RecommendationStateService);

  readonly result = signal<RecommendationResult | null>(null);
  readonly items = signal<EnrichedRecommendation[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly similar = signal<Movie[]>([]);
  readonly similarTitle = signal('');
  readonly similarLoading = signal(false);
  readonly similarError = signal<string | null>(null);

  private query = '';
  private maxResults = 5;

  constructor() {
    this.route.queryParamMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => {
        const query = params.get('query');
        const requested = Number(params.get('maxResults'));
        this.maxResults = Number.isFinite(requested) && requested > 0
          ? Math.min(Math.max(requested, 1), 5)
          : 5;

        if (!query || !query.trim()) {
          this.query = '';
          this.result.set(null);
          this.error.set('No search query was provided. Describe a movie on the home page first.');
          return;
        }
        this.query = query.trim();
        this.run();
      });
  }

  run(): void {
    if (!this.query) {
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.result.set(null);
    this.items.set([]);
    this.similar.set([]);
    this.similarError.set(null);

    this.recommendationService.recommend(this.query, this.maxResults).subscribe({
      next: (result) => {
        this.result.set(result);
        this.state.remember(result);
        this.loading.set(false);
        this.enrich(result.recommendations);
      },
      error: (err: unknown) => {
        this.error.set(toUserMessage(err));
        this.loading.set(false);
      },
    });
  }

  /** The orchestrator returns ids/titles/years; fetch real metadata per movie. */
  private enrich(recommendations: RecommendedMovie[]): void {
    if (recommendations.length === 0) {
      this.items.set([]);
      return;
    }
    forkJoin(
      recommendations.map((recommendation) =>
        this.movieService.getById(recommendation.movieId).pipe(
          map((movie) => ({ recommendation, movie })),
          catchError(() =>
            of({ recommendation, movie: fallbackMovie(recommendation) }),
          ),
        ),
      ),
    ).subscribe((items) => this.items.set(items));
  }

  onFindSimilar(recommendation: RecommendedMovie): void {
    this.similarTitle.set(recommendation.title);
    this.similarLoading.set(true);
    this.similarError.set(null);
    this.similar.set([]);

    this.recommendationService
      .findSimilar({ id: recommendation.movieId, title: recommendation.title }, 4)
      .subscribe({
        next: (movies) => {
          this.similar.set(movies);
          this.similarLoading.set(false);
        },
        error: (err: unknown) => {
          this.similarError.set(toUserMessage(err));
          this.similarLoading.set(false);
        },
      });
  }
}

/** No invented metadata: only fields actually returned by the orchestrator. */
function fallbackMovie(recommendation: RecommendedMovie): Movie {
  return {
    id: recommendation.movieId,
    title: recommendation.title,
    overview: null,
    genres: [],
    releaseYear: recommendation.releaseYear,
    runtimeMinutes: null,
    keywords: [],
    rating: null,
    posterUrl: null,
  };
}
