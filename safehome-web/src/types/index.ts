export interface Position {
  lat: number
  lng: number
}

export interface AlertEvent {
  type: 'DISASTER' | 'CRIME'
  message: string
  district: string
  level: 'INFO' | 'WARNING' | 'DANGER'
  issuedAt?: string
}