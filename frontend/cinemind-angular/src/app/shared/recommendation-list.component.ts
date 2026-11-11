import { Component, EventEmitter, Input, Output } from '@angular/core';

import { Movie } from '../core/models/movie.model';
import {
  EnrichedRecommendation,
  RecommendedMovie,
} from '../core/models/recommendation.model';
import { MovieCardComponent } from './movie-card.component';

@Component({
  selector: 'app-recommendation-list',
  imports: [MovieCardComponent],
  templateUrl: './recommendation-list.component.html',
  styleUrl: './recommendation-list.component.css',
})
export class RecommendationListComponent {
  @Input({ required: true }) items: EnrichedRecommendation[] = [];
  @Output() findSimilar = new EventEmitter<RecommendedMovie>();

  onFindSimilar(movie: Movie): void {
    const match = this.items.find((item) => item.movie.id === movie.id);
    if (match) {
      this.findSimilar.emit(match.recommendation);
    }
  }
}
