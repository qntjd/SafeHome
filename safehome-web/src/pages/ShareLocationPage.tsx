import { useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Map, MapMarker } from 'react-kakao-maps-sdk'
import { Link2, Siren } from 'lucide-react'
import { tripApi } from '@/api/trip'

export default function ShareLocationPage() {
  const { shareToken } = useParams<{ shareToken: string }>()

  const { data, isLoading, isError } = useQuery({
    queryKey: ['shared-location', shareToken],
    queryFn:  () => tripApi.getSharedLocation(shareToken!),
    refetchInterval: 30_000, // 30초마다 갱신
    enabled: !!shareToken,
  })

  const location = data?.data?.data

  if (isLoading) return (
    <div
      className="min-h-screen flex items-center justify-center"
      style={{ background: 'var(--bg-primary)' }}
    >
      <div className="text-center">
        <div
          className="w-10 h-10 rounded-full border-2 border-t-transparent animate-spin mx-auto mb-4"
          style={{ borderColor: 'var(--accent-amber)', borderTopColor: 'transparent' }}
        />
        <p className="text-sm" style={{ color: 'var(--text-muted)' }}>위치 불러오는 중...</p>
      </div>
    </div>
  )

  if (isError || !location) return (
    <div
      className="min-h-screen flex items-center justify-center"
      style={{ background: 'var(--bg-primary)' }}
    >
      <div className="text-center">
        <Link2 size={32} strokeWidth={1.75} className="mx-auto mb-3" style={{ color: 'var(--text-muted)' }} />
        <p className="text-sm" style={{ color: 'var(--text-muted)' }}>
          유효하지 않은 링크이거나 만료된 링크예요.
        </p>
      </div>
    </div>
  )

  const STATUS_LABEL: Record<string, { label: string; color: string }> = {
    IN_PROGRESS: { label: '귀가 중', color: 'var(--accent-amber)' },
    ARRIVED:     { label: '도착 완료', color: 'var(--grade-a)' },
    SOS:         { label: 'SOS 발동', color: 'var(--accent-red)' },
    CANCELLED:   { label: '취소됨', color: 'var(--ink-text-muted)' },
  }
  const statusCfg = STATUS_LABEL[location.status] ?? STATUS_LABEL.IN_PROGRESS

  return (
    <div className="flex flex-col h-screen" style={{ background: 'var(--bg-primary)' }}>
      {/* 헤더 */}
      <div
        className="px-4 py-3 flex items-center justify-between shrink-0"
        style={{ background: 'var(--ink)', borderBottom: '1px solid var(--ink-border)' }}
      >
        <div className="flex items-center gap-2">
          <div
            className="w-7 h-7 rounded-full flex items-center justify-center"
            style={{ background: 'var(--accent-amber)' }}
          >
            <span className="font-display font-black text-sm" style={{ color: 'var(--ink)' }}>S</span>
          </div>
          <span className="font-display font-extrabold text-sm tracking-tight" style={{ color: 'var(--ink-text)' }}>
            SafeHome
          </span>
        </div>
        <span
          className="text-xs px-3 py-1 rounded-full font-medium flex items-center gap-1.5"
          style={{ color: statusCfg.color, background: 'var(--ink-2)', border: `1px solid ${statusCfg.color}` }}
        >
          {location.status === 'IN_PROGRESS' && (
            <span className="beacon-dot" style={{ background: statusCfg.color }} />
          )}
          {statusCfg.label}
        </span>
      </div>

      {/* 지도 */}
      <div className="flex-1 relative">
        <Map
          center={{ lat: location.currentLat, lng: location.currentLng }}
          style={{ width: '100%', height: '100%' }}
          level={5}
        >
          {/* 현재 위치 */}
          <MapMarker
            position={{ lat: location.currentLat, lng: location.currentLng }}
            image={{
              src:  'https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/markerStar.png',
              size: { width: 24, height: 35 },
            }}
          />
          {/* 목적지 */}
          <MapMarker position={{ lat: location.endLat, lng: location.endLng }} />
        </Map>

        {/* 정보 카드 */}
        <div
          className="absolute bottom-4 left-4 right-4 rounded-2xl p-4"
          style={{
            background: 'rgba(14,21,38,0.92)',
            border: '1px solid var(--ink-border)',
            boxShadow: 'var(--shadow-md)',
            backdropFilter: 'blur(12px)',
          }}
        >
          <p className="font-display font-bold text-sm mb-3" style={{ color: 'var(--ink-text)' }}>
            {location.nickname}님의 귀가 현황
          </p>
          <div className="flex flex-col gap-2 text-xs">
            <div className="flex justify-between">
              <span style={{ color: 'var(--ink-text-muted)' }}>상태</span>
              <span style={{ color: statusCfg.color, fontWeight: 600 }}>{statusCfg.label}</span>
            </div>
            <div className="flex justify-between">
              <span style={{ color: 'var(--ink-text-muted)' }}>예상 도착</span>
              <span className="font-mono" style={{ color: 'var(--ink-text)' }}>
                {new Date(location.expectedArrivalAt).toLocaleTimeString('ko-KR')}
              </span>
            </div>
          </div>

          {location.status === 'SOS' && (
            <div
              className="mt-3 rounded-xl px-3 py-2 text-xs font-bold text-center flex items-center justify-center gap-1.5"
              style={{ background: 'rgba(215,38,61,0.25)', border: '1px solid var(--accent-red)', color: '#fff' }}
            >
              <Siren size={14} strokeWidth={2.25} />
              SOS가 발동됐어요! 즉시 연락해주세요.
            </div>
          )}

          <p className="text-xs text-center mt-3 flex items-center justify-center gap-1.5" style={{ color: 'var(--ink-text-muted)' }}>
            <span className="beacon-dot beacon-dot--static" style={{ width: 5, height: 5 }} />
            30초마다 자동 새로고침
          </p>
        </div>
      </div>
    </div>
  )
}