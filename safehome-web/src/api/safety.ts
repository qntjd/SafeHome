import api from './axios'

export interface FacilityResponse {
  type: 'CCTV' | 'EMERGENCY_BELL' | 'STREETLIGHT' | 'SAFE_ZONE' | 'POLICE'
  lat: number
  lng: number
  districtName: string
}

export interface ScoreResponse {
  districtCode: string
  districtName: string
  cctvScore: number
  crimeScore: number
  lightScore: number
  bellScore: number
  totalScore: number
  grade: 'A' | 'B' | 'C' | 'D' | 'F'
  calculatedAt: string
}

export interface HeatmapResponse {
  districts: ScoreResponse[]
}

export interface NearbyDangerResponse {
  cctvCount: number
  bellCount: number
  policeCount: number
  dangerLevel: 'SAFE' | 'CAUTION' | 'DANGER'
  message: string
}

export const safetyApi = {
  getFacilities: (lat: number, lng: number, radius = 500) =>
    api.get<{ data: FacilityResponse[] }>('/safety/facilities', {
      params: { lat, lng, radius },
    }),

  getScore: (districtCode: string) =>
    api.get<{ data: ScoreResponse }>('/safety/score', {
      params: { districtCode },
    }),

  getHeatmap: () =>
    api.get<{ data: HeatmapResponse }>('/safety/heatmap'),

  getSafeRoute: (startLat: number, startLng: number, endLat: number, endLng: number) =>
  api.get<{ data: SafeRouteResponse }>('/safety/safe-route', {
    params: { startLat, startLng, endLat, endLng }
  }),

  getNearbyDanger: (lat: number, lng: number) =>
  api.get<{ data: NearbyDangerResponse }>('/safety/nearby-danger', {
    params: { lat, lng }
  }),
}

export interface RoutePoint {
  lat: number
  lng: number
  type: string
  districtName: string
}

export interface SafeRouteResponse {
  safePoints: RoutePoint[]
  totalCctv: number
  totalBell: number
  totalPolice: number
  safetyScore: number
}