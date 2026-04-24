import { useState, useEffect } from 'react'
import type { Position } from '@/types'

const DEFAULT_POSITION: Position = { lat: 35.8714, lng: 128.6014 } // 대구 기본값

export function useCurrentLocation() {
  const [position, setPosition] = useState<Position>(DEFAULT_POSITION)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!navigator.geolocation) {
      setError('위치 정보를 지원하지 않는 브라우저입니다.')
      return
    }

    navigator.geolocation.getCurrentPosition(
      ({ coords }) => {
        setPosition({ lat: coords.latitude, lng: coords.longitude })
      },
      () => {
        setError('위치 권한이 거부되었습니다. 기본 위치를 사용합니다.')
      }
    )
  }, [])

  return { position, error }
}