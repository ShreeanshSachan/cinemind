import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { RecommendationService } from './recommendation.service';

const BASE = environment.apiBaseUrl;

describe('RecommendationService', () => {
  let service: RecommendationService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(RecommendationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('posts the query and maxResults to the orchestrator', () => {
    service.recommend('dark thriller', 3).subscribe();

    const request = httpMock.expectOne(`${BASE}/api/recommendations`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ query: 'dark thriller', maxResults: 3 });
    request.flush({ recommendations: [] });
  });

  it('maps RAG similar results into movies and removes the source movie', () => {
    let result: { id: number }[] = [];
    service.findSimilar({ id: 17, title: 'Shutter Island' }, 2)
      .subscribe((movies) => (result = movies));

    const request = httpMock.expectOne(`${BASE}/api/rag/search`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body.query).toContain('Shutter Island');
    request.flush({
      results: [
        { movieId: 17, title: 'Shutter Island', genres: ['Thriller'], rating: 8.2 },
        { movieId: 18, title: 'Prisoners', genres: ['Thriller'], rating: 8.1 },
        { movieId: 19, title: 'Gone Girl', genres: ['Thriller'], rating: 8.1 },
      ],
    });

    expect(result.map((m) => m.id)).toEqual([18, 19]);
    expect(result.length).toBe(2);
  });

  it('reads orchestrator health', () => {
    let status = '';
    service.health().subscribe((health) => (status = health.status));

    httpMock.expectOne(`${BASE}/api/recommendations/health`)
      .flush({ status: 'DEGRADED', llmModel: 'gemini-2.5-flash' });

    expect(status).toBe('DEGRADED');
  });
});
