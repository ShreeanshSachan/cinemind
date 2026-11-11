import { HttpErrorResponse } from '@angular/common/http';
import { environment } from '../../environments/environment';

/** Turns an HTTP failure into a short, user-facing message. */
export function toUserMessage(error: unknown): string {
  if (!(error instanceof HttpErrorResponse)) {
    return 'Something went wrong. Please try again.';
  }

  if (error.status === 0) {
    return `Cannot reach the CineMind API Gateway at ${environment.apiBaseUrl}. Is the backend running?`;
  }

  const payload = error.error as { message?: string } | null;
  if (payload && typeof payload.message === 'string' && payload.message.trim()) {
    return payload.message;
  }

  if (error.status === 503) {
    return 'A CineMind service is temporarily unavailable. Please try again shortly.';
  }
  if (error.status === 404) {
    return 'The requested resource was not found.';
  }
  if (error.status === 400) {
    return 'The request was not valid. Please adjust your search and try again.';
  }
  return `Request failed (HTTP ${error.status}).`;
}
