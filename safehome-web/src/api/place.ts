import api from './axios'

export interface FavoritePlace {
  id: string
  name: string
  address: string
  lat: number
  lng: number
  placeType: 'HOME' | 'WORK' | 'SCHOOL' | 'CUSTOM'
}

export interface CreatePlaceRequest {
  name: string
  address: string
  lat: number
  lng: number
  placeType?: string
}

export const placeApi = {
  getPlaces: () =>
    api.get<{ data: FavoritePlace[] }>('/places'),

  addPlace: (data: CreatePlaceRequest) =>
    api.post<{ data: FavoritePlace }>('/places', data),

  deletePlace: (id: string) =>
    api.delete(`/places/${id}`),
}