import api from './axios'

export interface ProfileResponse {
  email: string
  nickname: string
  homeLat: number
  homeLng: number
}

export const userApi = {
  getMe: () =>
    api.get<{ data: ProfileResponse }>('/users/me'),

  updateProfile: (nickname: string) =>
    api.patch<{ data: ProfileResponse }>('/users/profile', { nickname }),
}