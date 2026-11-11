/** Mirrors the Movie Service MovieResponse DTO. */
export interface Movie {
  id: number;
  title: string;
  overview: string | null;
  genres: string[];
  releaseYear: number | null;
  runtimeMinutes: number | null;
  keywords: string[];
  rating: number | null;
  posterUrl: string | null;
  createdAt?: string;
  updatedAt?: string;
}

/** Mirrors the Movie Service PagedResponse envelope. */
export interface PagedMovies {
  content: Movie[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface MovieFilterParams {
  genre?: string;
  yearFrom?: number;
  yearTo?: number;
  minRuntime?: number;
  maxRuntime?: number;
  minRating?: number;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
}
