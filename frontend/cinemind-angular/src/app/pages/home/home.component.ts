import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';

import { toUserMessage } from '../../core/api-error';
import { Movie } from '../../core/models/movie.model';
import { MovieService } from '../../core/services/movie.service';
import { ErrorComponent } from '../../shared/error.component';
import { LoadingComponent } from '../../shared/loading.component';
import { MovieCardComponent } from '../../shared/movie-card.component';
import { SearchBarComponent, SearchCriteria } from '../../shared/search-bar.component';

@Component({
  selector: 'app-home',
  imports: [SearchBarComponent, MovieCardComponent, LoadingComponent, ErrorComponent],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css',
})
export class HomeComponent {
  private readonly movieService = inject(MovieService);
  private readonly router = inject(Router);

  readonly featured = signal<Movie[]>([]);
  readonly genres = signal<string[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly busy = signal(false);

  constructor() {
    this.loadCatalogue();
  }

  loadCatalogue(): void {
    this.loading.set(true);
    this.error.set(null);
    forkJoin({
      movies: this.movieService.getFeatured(8),
      genres: this.movieService.getAvailableGenres(),
    }).subscribe({
      next: ({ movies, genres }) => {
        this.featured.set(movies);
        this.genres.set(genres);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(toUserMessage(err));
        this.loading.set(false);
      },
    });
  }

  onSearch(criteria: SearchCriteria): void {
    this.busy.set(true);
    const query = this.buildNaturalLanguageQuery(criteria);
    void this.router
      .navigate(['/recommendations'], {
        queryParams: { query, maxResults: criteria.maxResults },
      })
      .finally(() => this.busy.set(false));
  }

  /** UI selections are folded into the natural-language query for Agent 1. */
  private buildNaturalLanguageQuery(criteria: SearchCriteria): string {
    const parts = [criteria.query];
    if (criteria.genre) {
      parts.push(`genre: ${criteria.genre}`);
    }
    if (criteria.mood) {
      parts.push(`mood: ${criteria.mood}`);
    }
    if (criteria.yearFrom !== null) {
      parts.push(`released after ${criteria.yearFrom - 1}`);
    }
    if (criteria.yearTo !== null) {
      parts.push(`released before ${criteria.yearTo + 1}`);
    }
    if (criteria.maxRuntime !== null) {
      parts.push(`under ${criteria.maxRuntime} minutes`);
    }
    if (criteria.minRating !== null) {
      parts.push(`rated at least ${criteria.minRating}`);
    }
    return parts.join(', ');
  }
}
