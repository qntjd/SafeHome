import api from './axios'

export interface SubscribeRequest {
  alertType: 'DISASTER' | 'CRIME' | 'ALL'
  sidoName: string
  sigunguName?: string
  label?: string
  isMyLocation?: boolean
}

export interface SubscriptionResponse {
  id: string
  alertType: string
  sidoName: string
  sigunguName?: string
  label?: string
  displayName: string
  isMyLocation: boolean
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

  getMyLocationHistory: () =>
    api.get<{ data: AlertHistoryResponse[] }>('/alerts/history/my'),  // 추가

  unsubscribe: (id: string) =>
    api.delete(`/alerts/subscribe/${id}`),
}