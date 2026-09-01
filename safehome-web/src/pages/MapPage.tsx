import { useState, useEffect, useCallback } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Map, MapMarker } from 'react-kakao-maps-sdk'
import { MapPin } from 'lucide-react'
import { safetyApi } from '@/api/safety'
import type { FacilityResponse } from '@/api/safety'
import { useCurrentLocation } from '@/hooks/useCurrentLocation'
import { getFacilityMarkerImage } from '@/utils/mapUtils'

const FACILITY_CONFIG = {
  CCTV:           { color: 'var(--accent-blue)',   label: 'CCTV' },
  EMERGENCY_BELL: { color: 'var(--accent-red)',    label: '비상벨' },
  POLICE:         { color: 'var(--accent-purple)', label: '경찰서·파출소' },
}

const GRADE_CONFIG: Record<string, { color: string; bg: string }> = {
  A: { color: 'var(--grade-a)', bg: 'var(--grade-a-bg)' },
  B: { color: 'var(--grade-b)', bg: 'var(--grade-b-bg)' },
  C: { color: 'var(--grade-c)', bg: 'var(--grade-c-bg)' },
  D: { color: 'var(--grade-d)', bg: 'var(--grade-d-bg)' },
  F: { color: 'var(--grade-f)', bg: 'var(--grade-f-bg)' },
}

export default function MapPage() {
  const { position } = useCurrentLocation()
  const [mapCenter, setMapCenter] = useState<{ lat: number; lng: number } | null>(null)
  const [selectedFacility, setSelectedFacility] = useState<FacilityResponse | null>(null)
  const [activeTypes, setActiveTypes] = useState<Set<string>>(
    new Set(['CCTV', 'EMERGENCY_BELL', 'POLICE'])
  )
  const [myDistrictName, setMyDistrictName] = useState<string | null>(null)

  useEffect(() => {
    if (position && !mapCenter) {
      setMapCenter(position)
    }
  }, [position, mapCenter])

  // 현재 위치 → 역지오코딩으로 시도명 추출
  useEffect(() => {
    if (!position || !window.kakao) return
    const geocoder = new window.kakao.maps.services.Geocoder()
    geocoder.coord2Address(position.lng, position.lat, (result: any[], status: string) => {
      if (status === window.kakao.maps.services.Status.OK) {
        const addr = result[0]?.address?.address_name ?? ''
        const parts = addr.split(' ')
        if (parts.length >= 2) {
          const sido = parts[0]
            .replace('특별시', '')
            .replace('광역시', '')
            .replace('특별자치시', '')
            .replace('도', '')
            .trim()
          const sigungu = parts[1]
          setMyDistrictName(`${sido} ${sigungu}`)
        }
      }
    })
  }, [position])

  const { data: facilities } = useQuery({
    queryKey: ['facilities', mapCenter],
    queryFn:  () => safetyApi.getFacilities(mapCenter!.lat, mapCenter!.lng, 3000),
    enabled:  !!mapCenter,
  })

  // 지도 마커는 트래픽 최적화를 위해 최대 300개로 제한되므로,
  // "내 동네" 요약에는 실제 총개수를 세는 전용 카운트 API를 따로 쓴다.
  const { data: facilityCounts } = useQuery({
    queryKey: ['facility-counts', position],
    queryFn:  () => safetyApi.getFacilityCounts(position.lat, position.lng, 3000),
    enabled:  !!position,
  })

  const { data: heatmap } = useQuery({
    queryKey: ['heatmap'],
    queryFn:  () => safetyApi.getHeatmap(),
  })

  const facilityList = (facilities?.data?.data ?? []).filter(f => activeTypes.has(f.type))
  const districts    = heatmap?.data?.data?.districts ?? []

  const toggleType = (type: string) => {
    setActiveTypes(prev => {
      const next = new Set(prev)
      next.has(type) ? next.delete(type) : next.add(type)
      return next
    })
  }

  // 지도 이동/줌이 끝나면 중심 좌표 갱신 → 새 지역 마커 로드
  const handleMapChange = useCallback((map: kakao.maps.Map) => {
    const center = map.getCenter()
    setMapCenter({ lat: center.getLat(), lng: center.getLng() })
  }, [])

  // 내 동네 매칭
  const myDistrict = myDistrictName
    ? districts.find(d =>
        d.districtName === myDistrictName ||
        d.districtName.includes(myDistrictName.split(' ')[1] ?? '_')
      )
    : null

  const isMyDistrict = (districtName: string) =>
    myDistrict?.districtName === districtName

  return (
    <div className="flex flex-col sm:flex-row h-full">
      {/* 지도 */}
      <div className="relative h-[45vh] shrink-0 sm:h-full sm:flex-1 sm:shrink">
        <Map
          center={position}
          style={{ width: '100%', height: '100%' }}
          level={5}
          onDragEnd={handleMapChange}
          onZoomChanged={handleMapChange}
        >
          <MapMarker position={position} />
          {facilityList.map((f, i) => (
            <MapMarker
              key={i}
              position={{ lat: f.lat, lng: f.lng }}
              onClick={() => setSelectedFacility(f)}
              image={getFacilityMarkerImage(f.type)}
            />
          ))}
        </Map>

        {/* 필터 버튼 */}
        <div className="absolute top-3 left-3 flex gap-2 flex-wrap z-10">
          {Object.entries(FACILITY_CONFIG).map(([type, cfg]) => (
            <button
              key={type}
              onClick={() => toggleType(type)}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-medium transition-all"
              style={{
                background:     activeTypes.has(type) ? cfg.color : 'rgba(14,21,38,0.7)',
                color:          activeTypes.has(type) ? '#fff' : 'var(--ink-text-muted)',
                border:         `1px solid ${activeTypes.has(type) ? cfg.color : 'var(--ink-border)'}`,
                backdropFilter: 'blur(8px)',
              }}
            >
              <div className="w-2 h-2 rounded-full"
                style={{ background: activeTypes.has(type) ? '#fff' : cfg.color }} />
              {cfg.label}
            </button>
          ))}
        </div>

        {/* 선택된 시설 팝업 */}
        {selectedFacility && (
          <div
            className="absolute bottom-4 left-4 rounded-2xl p-4"
            style={{
              background:     'var(--bg-card)',
              border:         '1px solid var(--border)',
              boxShadow:      'var(--shadow-md)',
              backdropFilter: 'blur(8px)',
              minWidth:       180,
            }}
          >
            <div className="flex items-center gap-2 mb-1">
              <div
                className="w-6 h-6 rounded-full flex items-center justify-center"
                style={{ background: FACILITY_CONFIG[selectedFacility.type as keyof typeof FACILITY_CONFIG]?.color ?? 'var(--text-muted)' }}
              >
                <div className="w-2 h-2 rounded-full bg-white" />
              </div>
              <p className="text-sm font-medium" style={{ color: 'var(--text-primary)' }}>
                {FACILITY_CONFIG[selectedFacility.type as keyof typeof FACILITY_CONFIG]?.label ?? selectedFacility.type}
              </p>
            </div>
            <p className="text-xs mb-3" style={{ color: 'var(--text-muted)' }}>
              {selectedFacility.districtName}
            </p>
            <button
              onClick={() => setSelectedFacility(null)}
              className="text-xs px-3 py-1 rounded-lg"
              style={{ color: 'var(--text-muted)', border: '1px solid var(--border)' }}
            >
              닫기
            </button>
          </div>
        )}
      </div>

      {/* 사이드바 */}
      <aside
        className="w-full flex-1 min-h-0 sm:w-72 sm:flex-none sm:shrink-0 flex flex-col"
        style={{ background: 'var(--bg-secondary)', borderLeft: '1px solid var(--border)' }}
      >
        <div className="p-4" style={{ borderBottom: '1px solid var(--border)' }}>
          <h1 className="font-display font-bold text-sm" style={{ color: 'var(--text-primary)' }}>
            안전점수
          </h1>
        </div>

        <div className="flex-1 overflow-y-auto">
          {myDistrict && (() => {
            const gc = GRADE_CONFIG[myDistrict.grade] ?? GRADE_CONFIG.F
            return (
              <div className="p-3" style={{ borderBottom: '1px solid var(--border)' }}>
                <p className="text-xs mb-2 flex items-center gap-1"
                  style={{ color: 'var(--accent-blue)' }}>
                  <MapPin size={12} strokeWidth={2} /> 내 동네
                </p>
                <div
                  className="rounded-2xl p-3"
                  style={{
                    background: 'rgba(11,110,130,0.08)',
                    border:     '1px solid rgba(11,110,130,0.2)',
                  }}
                >
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-sm font-display font-semibold" style={{ color: 'var(--text-primary)' }}>
                      {myDistrict.districtName}
                    </span>
                    <span
                      className="text-xs font-bold font-mono px-2 py-0.5 rounded-full"
                      style={{ color: gc.color, background: gc.bg }}
                    >
                      {myDistrict.grade}등급
                    </span>
                  </div>
                  <div className="w-full rounded-full h-1.5 mb-1"
                    style={{ background: 'var(--bg-hover)' }}>
                    <div className="h-1.5 rounded-full transition-all"
                      style={{ width: `${myDistrict.totalScore}%`, background: gc.color }} />
                  </div>
                  <div className="flex items-center justify-between mt-1">
                    <p className="text-xs font-mono" style={{ color: 'var(--text-muted)' }}>
                      {Math.round(myDistrict.totalScore)}점
                    </p>
                    <p className="text-xs font-mono" style={{ color: 'var(--text-muted)' }}>
                      CCTV {facilityCounts?.data?.data?.cctvCount ?? '--'} · 비상벨 {facilityCounts?.data?.data?.bellCount ?? '--'}
                    </p>
                  </div>
                </div>
              </div>
            )
          })()}

          <div className="p-3 flex flex-col gap-2">
            {districts.length === 0 ? (
              <div className="flex flex-col gap-2 mt-2">
                {Array.from({ length: 5 }).map((_, i) => (
                  <div key={i} className="h-16 rounded-xl animate-pulse"
                    style={{ background: 'var(--bg-card)' }} />
                ))}
              </div>
            ) : (
              districts.map((d) => {
                const gc     = GRADE_CONFIG[d.grade] ?? GRADE_CONFIG.F
                const isMine = isMyDistrict(d.districtName)
                return (
                  <div
                    key={d.districtCode}
                    className="rounded-2xl p-3"
                    style={{
                      background: 'var(--bg-card)',
                      border:     `1px solid ${isMine ? 'rgba(11,110,130,0.3)' : 'var(--border)'}`,
                    }}
                  >
                    <div className="flex items-center justify-between mb-2">
                      <div className="flex items-center gap-1.5">
                        {isMine && <MapPin size={11} strokeWidth={2} color="var(--accent-blue)" />}
                        <span className="text-sm font-medium" style={{ color: 'var(--text-primary)' }}>
                          {d.districtName}
                        </span>
                      </div>
                      <span
                        className="text-xs font-bold font-mono px-2 py-0.5 rounded-full"
                        style={{ color: gc.color, background: gc.bg }}
                      >
                        {d.grade}
                      </span>
                    </div>
                    <div className="w-full rounded-full h-1.5 mb-1"
                      style={{ background: 'var(--bg-hover)' }}>
                      <div className="h-1.5 rounded-full transition-all"
                        style={{ width: `${d.totalScore}%`, background: gc.color }} />
                    </div>
                    <p className="text-xs font-mono" style={{ color: 'var(--text-muted)' }}>
                      {Math.round(d.totalScore)}점
                    </p>
                  </div>
                )
              })
            )}
          </div>
        </div>

        <div className="p-4 pb-28" style={{ borderTop: '1px solid var(--border)' }}>
          <p className="text-xs font-medium mb-3" style={{ color: 'var(--text-muted)' }}>시설 종류</p>
          <div className="flex flex-col gap-2">
            {Object.entries(FACILITY_CONFIG).map(([type, cfg]) => (
              <div key={type} className="flex items-center gap-2">
                <div className="w-3 h-3 rounded-full"
                  style={{ background: cfg.color, opacity: activeTypes.has(type) ? 1 : 0.3 }} />
                <span className="text-xs"
                  style={{ color: activeTypes.has(type) ? 'var(--text-secondary)' : 'var(--text-muted)' }}>
                  {cfg.label}
                </span>
              </div>
            ))}
          </div>
        </div>
      </aside>
    </div>
  )
}