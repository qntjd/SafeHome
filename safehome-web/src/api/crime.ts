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

export interface SidoGroupResponse {
  sidoCode: string
  sidoName: string
  crimeByType: Record<string, number>
  totalCount: number
  districts: DistrictCrimeResponse[]
}

export interface AllDistrictCrimeGroupedResponse {
  sidoGroups: SidoGroupResponse[]
}

export const crimeApi = {
  getAllCrimes: () =>
    api.get<{ data: AllDistrictCrimeResponse }>('/crime'),

  getAllCrimesGrouped: () =>
    api.get<{ data: AllDistrictCrimeGroupedResponse }>('/crime/grouped'),

  getDistrictCrimes: (districtCode: string) =>
    api.get<{ data: DistrictCrimeResponse }>(`/crime/${districtCode}`),
}