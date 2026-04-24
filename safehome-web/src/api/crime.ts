import api from './axios'

export interface DistrictCrimeResponse {
  districtCode: string
  districtName: string
  crimeByType: Record<string, number>
  totalCount: number
}

export interface AllDistrictCrimeResponse {
  districts: DistrictCrimeResponse[]
}

export const crimeApi = {
  getAllCrimes: () =>
    api.get<{ data: AllDistrictCrimeResponse }>('/crime'),

  getDistrictCrimes: (districtCode: string) =>
    api.get<{ data: DistrictCrimeResponse }>(`/crime/${districtCode}`),
}