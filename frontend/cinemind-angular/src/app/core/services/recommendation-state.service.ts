import { Injectable, signal } from '@angular/core';

import { RecommendationResult } from '../models/recommendation.model';

/**
 * Shares the most recent REAL backend response between the results page and
 * the dedicated Agent Trace page. Nothing here is fabricated: if no request
 * has been made, the value stays null.
 */
@Injectable({ providedIn: 'root' })
export class RecommendationStateService {
  readonly lastResult = signal<RecommendationResult | null>(null);

  remember(result: RecommendationResult): void {
    this.lastResult.set(result);
  }

  clear(): void {
    this.lastResult.set(null);
  }
}
