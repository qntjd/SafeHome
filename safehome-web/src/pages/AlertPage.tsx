import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { alertApi } from '@/api/alert'
import  type { SubscribeRequest } from '@/api/alert'
import { useCurrentLocation } from '@/hooks/useCurrentLocation'
import { useState } from 'react'
import Footer from '@/components/Footer'

const LEVEL_STYLE = {
  INFO:    'bg-blue-50 text-blue-700 border-blue-100',
  WARNING: 'bg-yellow-50 text-yellow-700 border-yellow-100',
  DANGER:  'bg-red-50 text-red-700 border-red-100',
}

export default function AlertPage() {
  const queryClient = useQueryClient()
  const { position } = useCurrentLocation()
  const [radius, setRadius] = useState(3)

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

  const subscriptions = subs?.data?.data ?? []
  const alerts = history?.data?.data ?? []

  return (
    <div className="max-w-2xl mx-auto py-10 px-4 flex flex-col gap-8">
      {/* 구독 등록 */}
      <section className="bg-white rounded-2xl border border-gray-200 p-6">
        <h2 className="font-semibold text-gray-900 mb-4">현재 위치 알림 구독</h2>
        <div className="flex items-center gap-3 mb-4">
          <label className="text-sm text-gray-600">반경</label>
          <input
            type="range" min={1} max={10} step={1}
            value={radius}
            onChange={(e) => setRadius(Number(e.target.value))}
            className="flex-1"
          />
          <span className="text-sm font-medium w-12">{radius} km</span>
        </div>
        <button
          onClick={() => subscribeMutation.mutate({
            alertType: 'ALL',
            centerLat: position.lat,
            centerLng: position.lng,
            radiusKm: radius,
          })}
          disabled={subscribeMutation.isPending}
          className="w-full bg-blue-600 text-white rounded-xl py-2.5 text-sm font-medium hover:bg-blue-700 disabled:opacity-50 transition-colors"
        >
          {subscribeMutation.isPending ? '등록 중...' : '현재 위치로 구독 등록'}
        </button>
      </section>

      {/* 내 구독 목록 */}
      {subscriptions.length > 0 && (
        <section className="bg-white rounded-2xl border border-gray-200 p-6">
          <h2 className="font-semibold text-gray-900 mb-4">내 구독 목록</h2>
          <div className="flex flex-col gap-3">
            {subscriptions.map((sub) => (
              <div key={sub.id} className="flex items-center justify-between p-3 bg-gray-50 rounded-xl">
                <div>
                  <p className="text-sm font-medium text-gray-700">{sub.alertType}</p>
                  <p className="text-xs text-gray-400">반경 {sub.radiusKm}km</p>
                </div>
                <button
                  onClick={() => unsubscribeMutation.mutate(sub.id)}
                  className="text-xs text-red-400 hover:text-red-600 transition-colors"
                >
                  해제
                </button>
              </div>
            ))}
          </div>
        </section>
      )}

      {/* 최근 알림 이력 */}
      <section className="bg-white rounded-2xl border border-gray-200 p-6">
        <h2 className="font-semibold text-gray-900 mb-4">최근 재난알림</h2>
        {alerts.length === 0 && (
          <p className="text-sm text-gray-400">최근 알림이 없습니다.</p>
        )}
        <div className="flex flex-col gap-3">
          {alerts.map((alert) => (
            <div
              key={alert.id}
              className={`border rounded-xl p-4 text-sm ${LEVEL_STYLE[alert.level]}`}
            >
              <div className="flex items-center justify-between mb-1">
                <span className="font-medium">{alert.districtName}</span>
                <span className="text-xs opacity-70">
                  {new Date(alert.issuedAt).toLocaleString('ko-KR')}
                </span>
              </div>
              <p className="text-xs leading-relaxed opacity-80">{alert.message}</p>
            </div>
          ))}
        </div>
      </section>
      <Footer />
    </div>
    
  )
}