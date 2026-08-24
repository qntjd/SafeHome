import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { alertApi } from '@/api/alert'
import type { SubscribeRequest } from '@/api/alert'
import { useCurrentLocation } from '@/hooks/useCurrentLocation'
import { getRegionFromCoords } from '@/api/kakaoLocal'
import { useState } from 'react'
import Footer from '@/components/Footer'

const LEVEL_STYLE = {
  INFO:    { bg: 'var(--grade-b-bg)',        color: 'var(--accent-blue)' },
  WARNING: { bg: 'var(--accent-amber-soft)', color: 'var(--accent-amber-deep)' },
  DANGER:  { bg: 'var(--accent-red-soft)',   color: 'var(--accent-red)' },
}

export default function AlertPage() {
  const queryClient = useQueryClient()
  const { position } = useCurrentLocation()
  const [isSubscribing, setIsSubscribing] = useState(false)

  const { data: subs } = useQuery({
    queryKey: ['subscriptions'],
    queryFn: () => alertApi.getSubscriptions(),
  })

  const { data: history } = useQuery({
    queryKey: ['alertHistory'],
    queryFn: () => alertApi.getHistory(),
  })

  const subscribeMutation = useMutation({
    mutationFn: (req: SubscribeRequest) => alertApi.subscribe(req),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['subscriptions'] }),
  })

  const unsubscribeMutation = useMutation({
    mutationFn: (id: string) => alertApi.unsubscribe(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['subscriptions'] }),
  })

  const handleSubscribe = async () => {
    setIsSubscribing(true)
    try {
      const region = await getRegionFromCoords(position.lat, position.lng)
      subscribeMutation.mutate({
        alertType: 'ALL',
        sidoName: region.sidoName,
        sigunguName: region.sigunguName,
        isMyLocation: true,
      })
    } catch (err) {
      console.error('행정구역 변환 실패:', err)
    } finally {
      setIsSubscribing(false)
    }
  }

  const subscriptions = subs?.data?.data ?? []
  const alerts = history?.data?.data ?? []

  return (
    <div className="min-h-full" style={{ background: 'var(--bg-primary)' }}>
      <div className="max-w-2xl mx-auto py-10 px-4 flex flex-col gap-6">
        {/* 구독 등록 */}
        <section
          className="rounded-2xl p-6"
          style={{ background: 'var(--bg-card)', border: '1px solid var(--border)', boxShadow: 'var(--shadow-sm)' }}
        >
          <h2 className="font-display font-bold mb-4 flex items-center gap-2" style={{ color: 'var(--text-primary)' }}>
            <span className="beacon-dot beacon-dot--static" />
            현재 위치 알림 구독
          </h2>
          <button
            onClick={handleSubscribe}
            disabled={subscribeMutation.isPending || isSubscribing}
            className="w-full rounded-xl py-2.5 text-sm font-bold disabled:opacity-50 transition-all"
            style={{ background: 'var(--accent-amber)', color: 'var(--ink)' }}
          >
            {isSubscribing || subscribeMutation.isPending ? '등록 중...' : '현재 위치로 구독 등록'}
          </button>
        </section>

        {/* 내 구독 목록 */}
        {subscriptions.length > 0 && (
          <section
            className="rounded-2xl p-6"
            style={{ background: 'var(--bg-card)', border: '1px solid var(--border)', boxShadow: 'var(--shadow-sm)' }}
          >
            <h2 className="font-display font-bold mb-4" style={{ color: 'var(--text-primary)' }}>내 구독 목록</h2>
            <div className="flex flex-col gap-3">
              {subscriptions.map((sub) => (
                <div
                  key={sub.id}
                  className="flex items-center justify-between p-3 rounded-xl"
                  style={{ background: 'var(--bg-hover)' }}
                >
                  <div>
                    <p className="text-sm font-medium" style={{ color: 'var(--text-secondary)' }}>{sub.alertType}</p>
                    <p className="text-xs" style={{ color: 'var(--text-muted)' }}>{sub.displayName}</p>
                  </div>
                  <button
                    onClick={() => unsubscribeMutation.mutate(sub.id)}
                    className="text-xs transition-colors"
                    style={{ color: 'var(--accent-red)' }}
                  >
                    해제
                  </button>
                </div>
              ))}
            </div>
          </section>
        )}

        {/* 최근 알림 이력 */}
        <section
          className="rounded-2xl p-6"
          style={{ background: 'var(--bg-card)', border: '1px solid var(--border)', boxShadow: 'var(--shadow-sm)' }}
        >
          <h2 className="font-display font-bold mb-4" style={{ color: 'var(--text-primary)' }}>최근 재난알림</h2>
          {alerts.length === 0 && (
            <p className="text-sm" style={{ color: 'var(--text-muted)' }}>최근 알림이 없습니다.</p>
          )}
          <div className="flex flex-col gap-3">
            {alerts.map((alert) => {
              const ls = LEVEL_STYLE[alert.level]
              return (
                <div
                  key={alert.id}
                  className="rounded-xl p-4 text-sm"
                  style={{ background: ls.bg, color: ls.color, border: `1px solid color-mix(in srgb, ${ls.color} 35%, transparent)` }}
                >
                  <div className="flex items-center justify-between mb-1">
                    <span className="font-medium">{alert.districtName}</span>
                    <span className="text-xs font-mono" style={{ opacity: 0.75 }}>
                      {new Date(alert.issuedAt).toLocaleString('ko-KR')}
                    </span>
                  </div>
                  <p className="text-xs leading-relaxed" style={{ opacity: 0.85 }}>{alert.message}</p>
                </div>
              )
            })}
          </div>
        </section>
        <Footer />
      </div>
    </div>
  )
}
