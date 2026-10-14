import { apiClient } from './client'
import type { MovieResponse } from './types'

/** UC-07 — the public catalogue (SHOWING and UPCOMING); INACTIVE movies never reach customers. */
export async function listPublicMovies(): Promise<MovieResponse[]> {
  const { data } = await apiClient.get<MovieResponse[]>('/api/movies')
  return data
}
