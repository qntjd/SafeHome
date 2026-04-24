import api from './axios'

export interface SubscribeRequest {
  alertType: 'DISASTER' | 'CRIME' | 'ALL'
  centerLat: number
  centerLng: number
  radiusKm: number
}

export interface SubscriptionResponse {
  id: string
  alertType: string
  centerLat: number
  centerLng: number
  radiusKm: number
  isActive: boolean
}

export interface AlertHistoryResponse {
  id: string
  title: string
  message: string
  districtName: string
  level: 'INFO' | 'WARNING' | 'DANGER'
  issuedAt: string
}

export const alertApi = {
  subscribe: (data: SubscribeRequest) =>
    api.post<{ data: SubscriptionResponse }>('/alerts/subscribe', data),

  getSubscriptions: () =>
    api.get<{ data: SubscriptionResponse[] }>('/alerts/subscriptions'),

  getHistory: () =>
    api.get<{ data: AlertHistoryResponse[] }>('/alerts/history'),

  unsubscribe: (id: string) =>
    api.delete(`/alerts/subscribe/${id}`),
}