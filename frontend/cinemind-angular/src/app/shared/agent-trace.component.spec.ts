import { TestBed } from '@angular/core/testing';

import { AgentTrace, MoviePreferences } from '../core/models/recommendation.model';
import { AgentTraceComponent } from './agent-trace.component';

const trace: AgentTrace = {
  preferenceAgent: 'COMPLETED',
  ragRetrieval: 'COMPLETED',
  recommendationAgent: 'COMPLETED',
  retrievedDocuments: 10,
  candidatesAfterFilters: 6,
};

const preferences: MoviePreferences = {
  genres: ['Psychological Thriller'],
  mood: 'Mind-bending',
  similarTo: ['Shutter Island'],
  releaseYearMin: 2010,
  maxRuntime: 140,
  excludedGenres: ['Comedy'],
};

function create(): import('@angular/core/testing').ComponentFixture<AgentTraceComponent> {
  const fixture = TestBed.createComponent(AgentTraceComponent);
  fixture.componentRef.setInput('trace', trace);
  fixture.componentRef.setInput('preferences', preferences);
  fixture.componentRef.setInput('retrievedDocuments', 10);
  fixture.detectChanges();
  return fixture;
}

describe('AgentTraceComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AgentTraceComponent],
    }).compileComponents();
  });

  it('renders the backend statuses and retrieval counts', async () => {
    const fixture = create();
    await fixture.whenStable();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Preference Analyst');
    expect(text).toContain('RAG retrieval');
    expect(text).toContain('Recommendation Specialist');
    expect(text).toContain('COMPLETED');
    expect(text).toContain('Retrieved documents');
    expect(text).toContain('After hard filters');
  });

  it('renders the extracted preferences from the backend', async () => {
    const fixture = create();
    await fixture.whenStable();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Psychological Thriller');
    expect(text).toContain('Mind-bending');
    expect(text).toContain('Shutter Island');
    expect(text).toContain('Comedy');
    expect(text).toContain('2010');
    expect(text).toContain('140 min');
  });

  it('does not fabricate statuses: a non-COMPLETED backend value is shown as-is', async () => {
    const fixture = TestBed.createComponent(AgentTraceComponent);
    fixture.componentRef.setInput('trace', {
      ...trace,
      recommendationAgent: 'SKIPPED',
    });
    fixture.componentRef.setInput('preferences', preferences);
    fixture.componentRef.setInput('retrievedDocuments', 0);
    fixture.detectChanges();
    await fixture.whenStable();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('SKIPPED');
  });
});
