import { useState, useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Map, MapMarker } from 'react-kakao-maps-sdk'
import { safetyApi } from '@/api/safety'
import { crimeApi } from '@/api/crime'
import type { FacilityResponse } from '@/api/safety'
import type { DistrictCrimeResponse } from '@/api/crime'
import { useCurrentLocation } from '@/hooks/useCurrentLocation'

const FACILITY_CONFIG = {
  CCTV:           { color: '#4f7ef8', label: 'CCTV' },
  EMERGENCY_BELL: { color: '#f87171', label: '비상벨' },
  STREETLIGHT:    { color: '#fbbf24', label: '가로등' },
  POLICE:         { color: '#a78bfa', label: '경찰서·파출소' },
}

const GRADE_CONFIG: Record<string, { color: string; bg: string }> = {
  A: { color: 'var(--accent-green)',  bg: 'rgba(52,211,153,0.12)' },
  B: { color: 'var(--accent-blue)',   bg: 'rgba(79,126,248,0.12)' },
  C: { color: 'var(--accent-amber)',  bg: 'rgba(251,191,36,0.12)' },
  D: { color: '#fb923c',              bg: 'rgba(251,146,60,0.12)' },
  F: { color: 'var(--accent-red)',    bg: 'rgba(248,113,113,0.12)' },
}

const CRIME_TYPE_COLORS: Record<string, string> = {
  '강력범죄': '#f87171',
  '폭행':     '#fb923c',
  '절도':     '#fbbf24',
  '성범죄':   '#c084fc',
  '기타':     '#6b7280',
}

const getFacilityMarkerImage = (type: string) => {
  const color = FACILITY_CONFIG[type as keyof typeof FACILITY_CONFIG]?.color ?? '#888888'
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="28" height="36" viewBox="0 0 28 36"><path d="M14 0C6.268 0 0 6.268 0 14c0 9.333 14 22 14 22S28 23.333 28 14C28 6.268 21.732 0 14 0z" fill="${color}" stroke="white" stroke-width="2"/><circle cx="14" cy="14" r="6" fill="white"/></svg>`
  return {
    src:  'data:image/svg+xml;base64,' + btoa(unescape(encodeURIComponent(svg))),
    size: { width: 28, height: 36 },
  }
}

type TabType = 'safety' | 'crime'

export default function MapPage() {
  const { position } = useCurrentLocation()
  const [selectedFacility, setSelectedFacility] = useState<FacilityResponse | null>(null)
  const [selectedDistrict, setSelectedDistrict] = useState<DistrictCrimeResponse | null>(null)
  const [activeTab, setActiveTab]               = useState<TabType>('safety')
  const [activeTypes, setActiveTypes]           = useState<Set<string>>(
    new Set(['CCTV', 'EMERGENCY_BELL', 'POLICE'])
  )
  const [myDistrictName, setMyDistrictName]     = useState<string | null>(null)

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
    queryKey: ['facilities', position],
    queryFn:  () => safetyApi.getFacilities(position.lat, position.lng, 3000),
    enabled:  !!position,
  })

  const { data: heatmap } = useQuery({
    queryKey: ['heatmap'],
    queryFn:  () => safetyApi.getHeatmap(),
  })

  const { data: crimeData } = useQuery({
    queryKey: ['crimes'],
    queryFn:  () => crimeApi.getAllCrimes(),
  })

  const facilityList = (facilities?.data?.data ?? []).filter(f => activeTypes.has(f.type))
  const districts    = heatmap?.data?.data?.districts ?? []
  const crimes       = crimeData?.data?.data?.districts ?? []

  const toggleType = (type: string) => {
    setActiveTypes(prev => {
      const next = new Set(prev)
      next.has(type) ? next.delete(type) : next.add(type)
      return next
    })
  }

  const maxCrimeCount = Math.max(...crimes.map(d => d.totalCount), 1)

  const getCrimeColor = (count: number) => {
    const intensity = count / maxCrimeCount
    const r = Math.round(248 * intensity + 30 * (1 - intensity))
    const g = Math.round(50 * (1 - intensity))
    const b = Math.round(50 * (1 - intensity))
    return `rgba(${r}, ${g}, ${b}, ${0.2 + intensity * 0.4})`
  }

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
    <div className="flex h-full">
      {/* 지도 */}
      <div className="flex-1 relative">
        <Map
          center={position}
          style={{ width: '100%', height: '100%' }}
          level={5}
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
                background:     activeTypes.has(type) ? cfg.color : 'rgba(15,17,23,0.7)',
                color:          activeTypes.has(type) ? '#fff' : 'rgba(255,255,255,0.5)',
                border:         `1px solid ${activeTypes.has(type) ? cfg.color : 'rgba(255,255,255,0.15)'}`,
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
            className="absolute bottom-4 left-4 rounded-xl p-4 shadow-xl"
            style={{
              background:     'var(--bg-card)',
              border:         '1px solid var(--border)',
              backdropFilter: 'blur(8px)',
              minWidth:       180,
            }}
          >
            <div className="flex items-center gap-2 mb-1">
              <div
                className="w-6 h-6 rounded-full flex items-center justify-center"
                style={{ background: FACILITY_CONFIG[selectedFacility.type as keyof typeof FACILITY_CONFIG]?.color ?? '#888' }}
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
        className="w-72 flex flex-col shrink-0"
        style={{ background: 'var(--bg-secondary)', borderLeft: '1px solid var(--border)' }}
      >
        {/* 탭 */}
        <div className="flex" style={{ borderBottom: '1px solid var(--border)' }}>
          {([['safety', '안전점수'], ['crime', '범죄통계']] as [TabType, string][]).map(([tab, label]) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className="flex-1 py-3 text-sm font-medium transition-colors"
              style={{
                color:        activeTab === tab ? 'var(--accent-blue)' : 'var(--text-muted)',
                borderBottom: activeTab === tab ? '2px solid var(--accent-blue)' : '2px solid transparent',
              }}
            >
              {label}
            </button>
          ))}
        </div>

        {/* 안전점수 탭 */}
        {activeTab === 'safety' && (
          <div className="flex-1 overflow-y-auto">

            {/* 내 동네 카드 */}
            {myDistrict && (() => {
              const gc = GRADE_CONFIG[myDistrict.grade] ?? GRADE_CONFIG.F
              return (
                <div className="p-3" style={{ borderBottom: '1px solid var(--border)' }}>
                  <p className="text-xs mb-2 flex items-center gap-1"
                    style={{ color: 'var(--accent-blue)' }}>
                    <span>📍</span> 내 동네
                  </p>
                  <div
                    className="rounded-xl p-3"
                    style={{
                      background: 'rgba(79,126,248,0.08)',
                      border:     '1px solid rgba(79,126,248,0.2)',
                    }}
                  >
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-sm font-semibold" style={{ color: 'var(--text-primary)' }}>
                        {myDistrict.districtName}
                      </span>
                      <span
                        className="text-xs font-bold px-2 py-0.5 rounded-full"
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
                      <p className="text-xs" style={{ color: 'var(--text-muted)' }}>
                        {Math.round(myDistrict.totalScore)}점
                      </p>
                      <p className="text-xs" style={{ color: 'var(--text-muted)' }}>
                        CCTV {Math.round(myDistrict.cctvScore)} · 비상벨 {Math.round(myDistrict.bellScore)}
                      </p>
                    </div>
                  </div>
                </div>
              )
            })()}

            {/* 전체 지역 목록 */}
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
                      className="rounded-xl p-3"
                      style={{
                        background: 'var(--bg-card)',
                        border:     `1px solid ${isMine ? 'rgba(79,126,248,0.3)' : 'var(--border)'}`,
                      }}
                    >
                      <div className="flex items-center justify-between mb-2">
                        <div className="flex items-center gap-1.5">
                          {isMine && <span style={{ fontSize: 10 }}>📍</span>}
                          <span className="text-sm font-medium" style={{ color: 'var(--text-primary)' }}>
                            {d.districtName}
                          </span>
                        </div>
                        <span
                          className="text-xs font-bold px-2 py-0.5 rounded-full"
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
                      <p className="text-xs" style={{ color: 'var(--text-muted)' }}>
                        {Math.round(d.totalScore)}점
                      </p>
                    </div>
                  )
                })
              )}
            </div>
          </div>
        )}

        {/* 범죄통계 탭 */}
        {activeTab === 'crime' && (
          <div className="flex-1 overflow-y-auto">
            {!selectedDistrict && (
              <p className="text-xs text-center py-4" style={{ color: 'var(--text-muted)' }}>
                지역을 클릭하면 상세 통계를 볼 수 있어요
              </p>
            )}
            <div className="p-3 flex flex-col gap-2">
              {crimes
                .sort((a, b) => b.totalCount - a.totalCount)
                .map((district) => (
                <div
                  key={district.districtCode}
                  onClick={() => setSelectedDistrict(
                    selectedDistrict?.districtCode === district.districtCode ? null : district
                  )}
                  className="rounded-xl p-3 cursor-pointer transition-all"
                  style={{
                    background: selectedDistrict?.districtCode === district.districtCode
                      ? 'rgba(248,113,113,0.1)' : 'var(--bg-card)',
                    border: `1px solid ${selectedDistrict?.districtCode === district.districtCode
                      ? 'rgba(248,113,113,0.3)' : 'var(--border)'}`,
                  }}
                >
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-sm font-medium" style={{ color: 'var(--text-primary)' }}>
                      {district.districtName}
                    </span>
                    <span className="text-xs font-bold" style={{ color: 'var(--accent-red)' }}>
                      {district.totalCount.toLocaleString()}건
                    </span>
                  </div>
                  <div className="w-full rounded-full h-1.5 mb-2" style={{ background: 'var(--bg-hover)' }}>
                    <div
                      className="h-1.5 rounded-full"
                      style={{
                        width:      `${(district.totalCount / maxCrimeCount) * 100}%`,
                        background: getCrimeColor(district.totalCount),
                      }}
                    />
                  </div>
                  {selectedDistrict?.districtCode === district.districtCode && (
                    <div className="mt-3 flex flex-col gap-1.5">
                      {Object.entries(district.crimeByType)
                        .sort(([, a], [, b]) => b - a)
                        .map(([type, count]) => (
                        <div key={type} className="flex items-center justify-between">
                          <div className="flex items-center gap-1.5">
                            <div className="w-2 h-2 rounded-full"
                              style={{ background: CRIME_TYPE_COLORS[type] ?? '#888' }} />
                            <span className="text-xs" style={{ color: 'var(--text-secondary)' }}>
                              {type}
                            </span>
                          </div>
                          <div className="flex items-center gap-2">
                            <div
                              className="h-1 rounded-full"
                              style={{
                                width:      `${(count / district.totalCount) * 60}px`,
                                background: CRIME_TYPE_COLORS[type] ?? '#888',
                                opacity:    0.6,
                              }}
                            />
                            <span className="text-xs font-medium" style={{ color: 'var(--text-primary)' }}>
                              {count.toLocaleString()}
                            </span>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}

        {/* 범례 */}
        <div className="p-4" style={{ borderTop: '1px solid var(--border)' }}>
          {activeTab === 'safety' ? (
            <>
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
            </>
          ) : (
            <>
              <p className="text-xs font-medium mb-3" style={{ color: 'var(--text-muted)' }}>범죄 유형</p>
              <div className="flex flex-col gap-2">
                {Object.entries(CRIME_TYPE_COLORS).map(([type, color]) => (
                  <div key={type} className="flex items-center gap-2">
                    <div className="w-3 h-3 rounded-full" style={{ background: color }} />
                    <span className="text-xs" style={{ color: 'var(--text-secondary)' }}>{type}</span>
                  </div>
                ))}
              </div>
            </>
          )}
        </div>
      </aside>
    </div>
  )
}