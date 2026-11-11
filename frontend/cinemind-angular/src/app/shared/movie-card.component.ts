import { Component, EventEmitter, Input, Output, signal } from '@angular/core';

import { Movie } from '../core/models/movie.model';

@Component({
  selector: 'app-movie-card',
  template: `
    <article class="card" [class.recommendation]="matchScore !== null">
      <div class="poster">
        @if (movie.posterUrl && !imageFailed()) {
          <img [src]="movie.posterUrl" [alt]="'Poster for ' + movie.title"
               loading="lazy" (error)="imageFailed.set(true)" />
        } @else {
          <div class="fallback" aria-hidden="true">
            <span class="glyph">◈</span>
            <span class="fallback-title">{{ movie.title }}</span>
          </div>
        }
        <div class="overlay" aria-hidden="true"></div>
        @if (movie.rating !== null) {
          <span class="rating" title="Rating">★ {{ movie.rating }}</span>
        }
        @if (matchScore !== null) {
          <span class="score" [style.--pct.%]="matchScore">{{ matchScore }}%</span>
        }
      </div>

      <div class="body">
        <div class="meta">
          @if (movie.releaseYear !== null) {
            <span>{{ movie.releaseYear }}</span>
          }
          @if (movie.runtimeMinutes !== null) {
            <span>{{ movie.runtimeMinutes }} min</span>
          }
        </div>
        <h3 class="title" [title]="movie.title">{{ movie.title }}</h3>

        <div class="genres">
          @for (genre of movie.genres.slice(0, 3); track genre) {
            <span class="chip">{{ genre }}</span>
          }
        </div>

        @if (matchScore !== null) {
          <div class="score-bar" aria-hidden="true">
            <i [style.width.%]="matchScore"></i>
          </div>
        }

        @if (reason) {
          <p class="reason">{{ reason }}</p>
        } @else if (movie.overview) {
          <p class="overview">{{ movie.overview }}</p>
        }

        @if (showSimilarButton) {
          <button type="button" class="similar" (click)="findSimilar.emit(movie)">
            Discover similar <span aria-hidden="true">→</span>
          </button>
        }
      </div>
    </article>
  `,
  styles: [
    `
      .card {
        height: 100%;
        display: flex;
        flex-direction: column;
        overflow: hidden;
        border-radius: var(--radius);
        border: 1px solid var(--border);
        background: linear-gradient(165deg, var(--surface-2), var(--bg-700));
        box-shadow: var(--shadow-soft);
        transition: transform 240ms ease, box-shadow 240ms ease, border-color 240ms ease;
        animation: fade-up 420ms ease both;
      }
      .card:hover {
        transform: translateY(-6px);
        border-color: rgba(155, 108, 255, 0.45);
        box-shadow: 0 22px 46px rgba(0, 0, 0, 0.55);
      }
      .poster { position: relative; aspect-ratio: 2 / 3; overflow: hidden; background: var(--bg-800); }
      .poster img { width: 100%; height: 100%; object-fit: cover; }
      .overlay {
        position: absolute;
        inset: 0;
        background: linear-gradient(to top, rgba(10, 10, 22, 0.92), transparent 55%);
      }
      .fallback {
        position: absolute;
        inset: 0;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        gap: 0.8rem;
        padding: 1rem;
        text-align: center;
        background: radial-gradient(circle at 50% 20%, rgba(155, 108, 255, 0.25), transparent 65%),
          linear-gradient(160deg, #1b1b34, #101024);
      }
      .glyph { font-size: 2.4rem; color: var(--purple); opacity: 0.75; }
      .fallback-title { font-weight: 700; font-size: 0.95rem; color: var(--text-soft); }
      .rating {
        position: absolute;
        top: 0.7rem;
        left: 0.7rem;
        padding: 0.28rem 0.6rem;
        border-radius: 999px;
        font-size: 0.75rem;
        font-weight: 700;
        color: #ffd98a;
        background: rgba(10, 10, 22, 0.75);
        border: 1px solid var(--border);
      }
      .score {
        position: absolute;
        top: 0.7rem;
        right: 0.7rem;
        padding: 0.3rem 0.62rem;
        border-radius: 999px;
        font-size: 0.78rem;
        font-weight: 800;
        color: #fff;
        background: var(--gradient);
        box-shadow: 0 6px 18px rgba(122, 92, 255, 0.5);
      }
      .body { display: flex; flex-direction: column; gap: 0.55rem; padding: 1rem 1rem 1.15rem; flex: 1; }
      .meta { display: flex; gap: 0.7rem; color: var(--muted); font-size: 0.78rem; font-weight: 600; }
      .meta span + span::before { content: '•'; margin-right: 0.7rem; color: var(--purple); }
      .title { font-size: 1.02rem; line-height: 1.3; }
      .genres { display: flex; flex-wrap: wrap; gap: 0.35rem; }
      .score-bar { height: 4px; border-radius: 4px; background: rgba(255, 255, 255, 0.08); overflow: hidden; }
      .score-bar i { display: block; height: 100%; border-radius: 4px; background: var(--gradient); }
      .reason, .overview {
        color: var(--text-soft);
        font-size: 0.83rem;
        line-height: 1.5;
        display: -webkit-box;
        -webkit-line-clamp: 3;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }
      .reason { color: var(--text-soft); font-style: italic; }
      .similar {
        margin-top: auto;
        align-self: flex-start;
        padding: 0;
        border: 0;
        background: none;
        color: var(--purple);
        font-size: 0.82rem;
        font-weight: 700;
      }
      .similar:hover { color: var(--blue); }
    `,
  ],
})
export class MovieCardComponent {
  @Input({ required: true }) movie!: Movie;
  @Input() matchScore: number | null = null;
  @Input() reason = '';
  @Input() showSimilarButton = false;
  @Output() findSimilar = new EventEmitter<Movie>();

  readonly imageFailed = signal(false);
}
