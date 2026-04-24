import { useEffect, useRef, useCallback } from 'react'
import type { AlertEvent } from '@/types'

export function useSse(onAlert: (event: AlertEvent) => void) {
  const esRef = useRef<EventSource | null>(null)

  const connect = useCallback(() => {
    const token = localStorage.getItem('accessToken')
    if (!token) return

    // SSE는 헤더 전송 불가 → 토큰을 쿼리 파라미터로 전달
    const url = `/api/alerts/live?token=${token}`
    const es = new EventSource(url)

    es.addEventListener('connect', () => {
      console.log('[SSE] 연결됨')
    })

    es.addEventListener('alert', (e) => {
      try {
        const data: AlertEvent = JSON.parse(e.data)
        onAlert(data)
      } catch {
        console.error('[SSE] 파싱 오류')
      }
    })

    es.onerror = () => {
      console.warn('[SSE] 연결 끊김, 5초 후 재연결...')
      es.close()
      setTimeout(connect, 5000)
    }

    esRef.current = es
  }, [onAlert])

  useEffect(() => {
    connect()
    return () => esRef.current?.close()
  }, [connect])
}