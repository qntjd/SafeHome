import api from './axios'

export interface FacilityResponse {
  type: 'CCTV' | 'EMERGENCY_BELL' | 'STREETLIGHT' | 'SAFE_ZONE'
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
}