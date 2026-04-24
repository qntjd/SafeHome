import api from './axios'

export interface RegisterRequest {
  email: string
  password: string
  nickname: string
  homeLat?: number
  homeLng?: number
}

export interface LoginRequest {
  email: string
  password: string
}

export interface TokenResponse {
  accessToken: string
  refreshToken: string
  email: string
  nickname: string
}

export const authApi = {
  register: (data: RegisterRequest) =>
    api.post<{ data: TokenResponse }>('/auth/register', data),

  login: (data: LoginRequest) =>
    api.post<{ data: TokenResponse }>('/auth/login', data),

  refresh: (refreshToken: string) =>
    api.post<{ data: TokenResponse }>('/auth/refresh', { refreshToken }),
}