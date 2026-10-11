import { apiClient } from './client'
import type { MovieRequest, MovieResponse, MovieStatus } from './types'

export async function listMovies(): Promise<MovieResponse[]> {
  const { data } = await apiClient.get<MovieResponse[]>('/api/admin/movies')
  return data
}

export async function createMovie(request: MovieRequest): Promise<MovieResponse> {
  const { data } = await apiClient.post<MovieResponse>('/api/admin/movies', request)
  return data
}

export async function updateMovie(id: string, request: MovieRequest): Promise<MovieResponse> {
  const { data } = await apiClient.put<MovieResponse>(`/api/admin/movies/${id}`, request)
  return data
}

export async function updateMovieStatus(id: string, status: MovieStatus): Promise<MovieResponse> {
  const { data } = await apiClient.patch<MovieResponse>(`/api/admin/movies/${id}/status`, { status })
  return data
}
