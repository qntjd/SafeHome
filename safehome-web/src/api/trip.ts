import api from './axios'

export interface StartTripRequest {
  startLat: number
  startLng: number
  endLat: number
  endLng: number
  estimatedMinutes: number
}

export interface TripResponse {
  id: string
  startLat: number
  startLng: number
  endLat: number
  endLng: number
  departureAt: string
  expectedArrivalAt: string
  arrivedAt: string | null
  status: 'IN_PROGRESS' | 'ARRIVED' | 'SOS' | 'CANCELLED'
}

export const tripApi = {
  start: (data: StartTripRequest) =>
    api.post<{ data: TripResponse }>('/trips', data),

  arrive: (tripId: string) =>
    api.patch<{ data: TripResponse }>(`/trips/${tripId}/arrive`),

  sos: (tripId: string) =>
    api.post<{ data: TripResponse }>(`/trips/${tripId}/sos`),

  cancel: (tripId: string) =>
    api.delete(`/trips/${tripId}`),
}