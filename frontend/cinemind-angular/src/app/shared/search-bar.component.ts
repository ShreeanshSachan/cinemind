import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

/** UI presets for the mood selector - these are user choices, not results. */
export const MOOD_OPTIONS = [
  { value: '', label: 'Any mood' },
  { value: 'Mind-bending', label: 'Mind-bending' },
  { value: 'Dark and gritty', label: 'Dark & gritty' },
  { value: 'Suspenseful', label: 'Suspenseful' },
  { value: 'Emotional', label: 'Emotional' },
  { value: 'Feel-good', label: 'Feel-good' },
  { value: 'Escapist', label: 'Escapist' },
];

export interface SearchCriteria {
  query: string;
  genre: string;
  mood: string;
  yearFrom: number | null;
  yearTo: number | null;
  maxRuntime: number | null;
  minRating: number | null;
  maxResults: number;
}

@Component({
  selector: 'app-search-bar',
  imports: [ReactiveFormsModule],
  templateUrl: './search-bar.component.html',
  styleUrl: './search-bar.component.css',
})
export class SearchBarComponent {
  @Input() genres: string[] = [];
  @Input() busy = false;
  @Output() search = new EventEmitter<SearchCriteria>();

  readonly moods = MOOD_OPTIONS;

  readonly form = new FormGroup({
    query: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(3), Validators.maxLength(500)],
    }),
    genre: new FormControl('', { nonNullable: true }),
    mood: new FormControl('', { nonNullable: true }),
    yearFrom: new FormControl<number | null>(null, [Validators.min(1888), Validators.max(2100)]),
    yearTo: new FormControl<number | null>(null, [Validators.min(1888), Validators.max(2100)]),
    maxRuntime: new FormControl<number | null>(null, [Validators.min(1), Validators.max(1000)]),
    minRating: new FormControl<number | null>(null, [Validators.min(0), Validators.max(10)]),
    maxResults: new FormControl(5, {
      nonNullable: true,
      validators: [Validators.required, Validators.min(1), Validators.max(5)],
    }),
  });

  submit(): void {
    if (this.form.invalid || this.busy) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.search.emit({
      query: value.query.trim(),
      genre: value.genre,
      mood: value.mood,
      yearFrom: value.yearFrom,
      yearTo: value.yearTo,
      maxRuntime: value.maxRuntime,
      minRating: value.minRating,
      maxResults: value.maxResults,
    });
  }

  usePrompt(prompt: string): void {
    this.form.controls.query.setValue(prompt);
    this.form.controls.query.markAsTouched();
  }
}
