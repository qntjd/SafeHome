import { useState, useCallback, useEffect } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Map, MapMarker } from 'react-kakao-maps-sdk'
import { tripApi } from '@/api/trip'
import { placeApi } from '@/api/place'
import { safetyApi, type SafeRouteResponse } from '@/api/safety'
import type { TripResponse } from '@/api/trip'
import type { FavoritePlace } from '@/api/place'
import { useCurrentLocation } from '@/hooks/useCurrentLocation'
import TripStatusCard from '@/components/trip/TripStatusCard'
import { getFacilityMarkerImage } from '@/utils/mapUtils'
import type { NearbyDangerResponse } from '@/api/safety'

interface Place {
  name: string
  address: string
  lat: number
  lng: number
}

const PLACE_TYPE_CONFIG = {
  HOME:   { icon: '🏠', label: '집' },
  WORK:   { icon: '💼', label: '직장' },
  SCHOOL: { icon: '🏫', label: '학교' },
  CUSTOM: { icon: '📍', label: '즐겨찾기' },
}

export default function TripPage() {
  const { position }  = useCurrentLocation()
  const queryClient   = useQueryClient()
  const [activeTrip, setActiveTrip]         = useState<TripResponse | null>(null)
  const [searchKeyword, setSearchKeyword]   = useState('')
  const [searchResults, setSearchResults]   = useState<Place[]>([])
  const [selectedPlace, setSelectedPlace]   = useState<Place | null>(null)
  const [estimatedMinutes, setEstimatedMinutes] = useState(15)
  const [showAddFavorite, setShowAddFavorite]   = useState(false)
  const [favoriteLabel, setFavoriteLabel]       = useState('')
  const [isNightMode, setIsNightMode]           = useState(false)

  const [safeRoute, setSafeRoute] = useState<SafeRouteResponse | null>(null)
  const [loadingRoute, setLoadingRoute] = useState(false)
  const [nearbyDanger, setNearbyDanger] = useState<NearbyDangerResponse | null>(null)
  const [dangerAlert, setDangerAlert]   = useState(false)

  // 야간 모드 자동 전환 (오후 11시 ~ 오전 6시)
  useEffect(() => {
    const checkNightMode = () => {
      const hour = new Date().getHours()
      setIsNightMode(hour >= 23 || hour < 6)
    }
    checkNightMode()
    const timer = setInterval(checkNightMode, 60_000)
    return () => clearInterval(timer)
  }, [])

// 목적지 선택 시 안전 경로 분석
  useEffect(() => {
  if (!selectedPlace) {
    setSafeRoute(null)
    return
  }
  setLoadingRoute(true)
  safetyApi.getSafeRoute(
    position.lat, position.lng,
    selectedPlace.lat, selectedPlace.lng
  )
  .then(({ data }) => setSafeRoute(data.data))
  .catch(() => setSafeRoute(null))
  .finally(() => setLoadingRoute(false))
}, [selectedPlace])

  // 귀가 중 주변 위험 감지
  useEffect(() => {
    if (!activeTrip || activeTrip.status !== 'IN_PROGRESS') return

    const checkDanger = async () => {
      try {
        const { data } = await safetyApi.getNearbyDanger(position.lat, position.lng)
        const prev = nearbyDanger?.dangerLevel
        setNearbyDanger(data.data)

        
        if (data.data.dangerLevel === 'DANGER' && prev !== 'DANGER') {
          setDangerAlert(true)
          setTimeout(() => setDangerAlert(false), 8000)
        }
      } catch (e) {
        console.error('[위험 감지] 오류:', e)
      }
    }

    checkDanger()
    const timer = setInterval(checkDanger, 30_000)
    return () => clearInterval(timer)
  }, [activeTrip, position])


  // 즐겨찾기 목록
  const { data: favoritesData } = useQuery({
    queryKey: ['places'],
    queryFn:  () => placeApi.getPlaces(),
  })
  const favorites = favoritesData?.data?.data ?? []

  // 귀가 중 주변 시설 
  const { data: facilitiesData } = useQuery({
    queryKey: ['trip-facilities', position],
    queryFn:  () => safetyApi.getFacilities(position.lat, position.lng, 500),
    enabled:  !!activeTrip && activeTrip.status === 'IN_PROGRESS',
    refetchInterval: 30_000, 
  })
  const facilities = facilitiesData?.data?.data ?? []
  const cctvCount  = facilities.filter(f => f.type === 'CCTV').length
  const bellCount  = facilities.filter(f => f.type === 'EMERGENCY_BELL').length
  const policeCount = facilities.filter(f => f.type === 'POLICE').length

  const addFavoriteMutation = useMutation({
    mutationFn: () => placeApi.addPlace({
      name:      favoriteLabel || selectedPlace!.name,
      address:   selectedPlace!.address,
      lat:       selectedPlace!.lat,
      lng:       selectedPlace!.lng,
      placeType: 'CUSTOM',
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['places'] })
      setShowAddFavorite(false)
      setFavoriteLabel('')
      alert('즐겨찾기에 추가됐어요!')
    },
  })

  const deleteFavoriteMutation = useMutation({
    mutationFn: (id: string) => placeApi.deletePlace(id),
    onSuccess:  () => queryClient.invalidateQueries({ queryKey: ['places'] }),
  })

  // 카카오 장소 검색
  const searchPlace = useCallback(() => {
    if (!searchKeyword.trim()) return
    const ps = new window.kakao.maps.services.Places()
    ps.keywordSearch(searchKeyword, (result: any[], status: string) => {
      if (status === window.kakao.maps.services.Status.OK) {
        setSearchResults(result.map((item) => ({
          name:    item.place_name,
          address: item.road_address_name || item.address_name,
          lat:     parseFloat(item.y),
          lng:     parseFloat(item.x),
        })))
      } else {
        setSearchResults([])
      }
    })
  }, [searchKeyword])

  // 지도 클릭 
  const handleMapClick = useCallback((_: any, mouseEvent: any) => {
    const lat = mouseEvent.latLng.getLat()
    const lng = mouseEvent.latLng.getLng()
    const geocoder = new window.kakao.maps.services.Geocoder()
    geocoder.coord2Address(lng, lat, (result: any[], status: string) => {
      if (status === window.kakao.maps.services.Status.OK) {
        const addr = result[0].road_address?.address_name ||
                     result[0].address?.address_name || '주소 없음'
        setSelectedPlace({ name: addr, address: addr, lat, lng })
        setSearchResults([])
        setSearchKeyword('')
      }
    })
  }, [])

  const handleSelectPlace = (place: Place | FavoritePlace) => {
    setSelectedPlace({
      name:    place.name,
      address: place.address,
      lat:     place.lat,
      lng:     place.lng,
    })
    setSearchResults([])
    setSearchKeyword('')
  }

  const startMutation = useMutation({
    mutationFn: () => tripApi.start({
      startLat: position.lat,
      startLng: position.lng,
      endLat:   selectedPlace!.lat,
      endLng:   selectedPlace!.lng,
      estimatedMinutes,
    }),
    onSuccess: ({ data }) => setActiveTrip(data.data),
    onError:   () => alert('귀가 시작에 실패했습니다.'),
  })

  const arriveMutation  = useMutation({ mutationFn: () => tripApi.arrive(activeTrip!.id),  onSuccess: ({ data }) => setActiveTrip(data.data) })
  const sosMutation     = useMutation({ mutationFn: () => tripApi.sos(activeTrip!.id),     onSuccess: ({ data }) => setActiveTrip(data.data) })
  const cancelMutation  = useMutation({ mutationFn: () => tripApi.cancel(activeTrip!.id),  onSuccess: () => setActiveTrip(null) })

  if (activeTrip) {
    return (
      <div className="flex h-full">
        {/* 귀가 중 지도 */}
        <div className="flex-1 relative">
          <Map
            center={position}
            style={{ width: '100%', height: '100%' }}
            level={4}
          >
            <MapMarker position={position} />
            <MapMarker position={{ lat: activeTrip.endLat, lng: activeTrip.endLng }} />
          </Map>

          {/* 주변 위험 알림 배너 */}
          {nearbyDanger && (
            <div
              className="absolute top-3 right-3 left-3 rounded-xl px-4 py-3 z-10 transition-all"
              style={{
                background: nearbyDanger.dangerLevel === 'SAFE'
                  ? 'rgba(52,211,153,0.15)' : nearbyDanger.dangerLevel === 'CAUTION'
                  ? 'rgba(251,191,36,0.15)' : 'rgba(248,113,113,0.15)',
                border: `1px solid ${
                  nearbyDanger.dangerLevel === 'SAFE'    ? 'rgba(52,211,153,0.4)'
                  : nearbyDanger.dangerLevel === 'CAUTION' ? 'rgba(251,191,36,0.4)'
                  : 'rgba(248,113,113,0.4)'
                }`,
                backdropFilter: 'blur(8px)',
              }}
            >
              <div className="flex items-center gap-2">
                <span style={{ fontSize: 16 }}>
                  {nearbyDanger.dangerLevel === 'SAFE' ? '✅'
                  : nearbyDanger.dangerLevel === 'CAUTION' ? '⚠️' : '🚨'}
                </span>
                <div>
                  <p
                    className="text-xs font-semibold"
                    style={{
                      color: nearbyDanger.dangerLevel === 'SAFE'    ? 'var(--accent-green)'
                          : nearbyDanger.dangerLevel === 'CAUTION' ? 'var(--accent-amber)'
                          : 'var(--accent-red)',
                    }}
                  >
                    {nearbyDanger.dangerLevel === 'SAFE'    ? '안전 구역'
                  : nearbyDanger.dangerLevel === 'CAUTION' ? '주의 구역'
                  : '위험 구역'}
                  </p>
                  <p className="text-xs mt-0.5" style={{ color: 'var(--text-secondary)' }}>
                    {nearbyDanger.message}
                  </p>
                </div>
              </div>

              {/* 시설 현황 */}
              <div className="flex gap-3 mt-2 text-xs" style={{ color: 'var(--text-muted)' }}>
                <span>📹 CCTV {nearbyDanger.cctvCount}</span>
                <span>🔔 비상벨 {nearbyDanger.bellCount}</span>
                <span>🚔 경찰 {nearbyDanger.policeCount}</span>
              </div>
            </div>
          )}

          {/* DANGER 팝업 알림 */}
          {dangerAlert && (
            <div
              className="absolute top-1/2 left-4 right-4 -translate-y-1/2 rounded-2xl p-5 z-20 text-center"
              style={{
                background: 'rgba(220,38,38,0.95)',
                border: '2px solid rgba(248,113,113,0.5)',
                backdropFilter: 'blur(12px)',
              }}
            >
              <p className="text-2xl mb-2">🚨</p>
              <p className="text-white font-bold mb-1">위험 구역 진입!</p>
              <p className="text-sm mb-4" style={{ color: 'rgba(255,255,255,0.8)' }}>
                주변에 안전시설이 없어요.<br />
                빠르게 안전한 곳으로 이동하세요.
              </p>
              <button
                onClick={() => setDangerAlert(false)}
                className="text-sm px-4 py-2 rounded-xl"
                style={{ background: 'rgba(255,255,255,0.2)', color: 'white' }}
              >
                확인
              </button>
            </div>
          )}

          {/* 야간 모드 표시 */}
          {isNightMode && (
            <div
              className="absolute top-3 left-3 flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-medium z-10"
              style={{ background: 'rgba(167,139,250,0.2)', border: '1px solid rgba(167,139,250,0.4)', color: '#a78bfa', backdropFilter: 'blur(8px)' }}
            >
              🌙 야간 안전 모드
            </div>
          )}

          {/* 주변 안전시설 현황 */}
          <div
            className="absolute bottom-4 left-4 right-4 rounded-xl p-4 z-10"
            style={{ background: 'rgba(15,17,23,0.85)', border: '1px solid var(--border)', backdropFilter: 'blur(8px)' }}
          >
            <p className="text-xs font-medium mb-3" style={{ color: 'var(--text-muted)' }}>
              현재 위치 반경 500m 안전시설
            </p>
            <div className="grid grid-cols-3 gap-3 text-center">
              <div>
                <p className="text-xl font-bold" style={{ color: '#4f7ef8' }}>{cctvCount}</p>
                <p className="text-xs" style={{ color: 'var(--text-muted)' }}>CCTV</p>
              </div>
              <div>
                <p className="text-xl font-bold" style={{ color: '#f87171' }}>{bellCount}</p>
                <p className="text-xs" style={{ color: 'var(--text-muted)' }}>비상벨</p>
              </div>
              <div>
                <p className="text-xl font-bold" style={{ color: '#a78bfa' }}>{policeCount}</p>
                <p className="text-xs" style={{ color: 'var(--text-muted)' }}>경찰서</p>
              </div>
            </div>
          </div>
        </div>

        {/* 귀가 현황 사이드바 */}
        <aside
          className="w-80 flex flex-col shrink-0"
          style={{ background: 'var(--bg-secondary)', borderLeft: '1px solid var(--border)' }}
        >

          {/* 위치 공유 버튼 */}
          {activeTrip.status === 'IN_PROGRESS' && activeTrip.shareToken && (
            <div
              className="mx-4 mb-3 rounded-xl p-3"
              style={{ background: 'var(--bg-hover)', border: '1px solid var(--border)' }}
            >
              <p className="text-xs font-medium mb-2" style={{ color: 'var(--text-muted)' }}>
                위치 공유
              </p>
              <button
                onClick={() => {
                  const url = `${window.location.origin}/share/${activeTrip.shareToken}`
                  navigator.clipboard.writeText(url)
                  alert('위치 공유 링크가 복사됐어요!')
                }}
                className="w-full rounded-xl py-2 text-sm font-medium transition-all"
                style={{ background: 'var(--accent-blue)', color: '#fff' }}
              >
                🔗 위치 공유 링크 복사
              </button>
              <p className="text-xs text-center mt-1.5" style={{ color: 'var(--text-muted)' }}>
                링크를 받은 사람이 실시간 위치를 볼 수 있어요
              </p>
            </div>
          )}
          <div className="p-4 flex-1">
            <TripStatusCard
              trip={activeTrip}
              onArrive={() => arriveMutation.mutate()}
              onSos={() => sosMutation.mutate()}
              onCancel={() => cancelMutation.mutate()}
              isLoading={arriveMutation.isPending || sosMutation.isPending}
            />
          </div>
        </aside>
      </div>
    )
  }

  return (
    <div className="flex h-full">
      {/* 사이드바 */}
      <aside
        className="w-80 flex flex-col shrink-0"
        style={{ background: 'var(--bg-secondary)', borderRight: '1px solid var(--border)' }}
      >
        {/* 헤더 */}
        <div className="p-4" style={{ borderBottom: '1px solid var(--border)' }}>
          <div className="flex items-center justify-between mb-3">
            <h1 className="text-base font-semibold" style={{ color: 'var(--text-primary)' }}>
              안심 귀가
            </h1>
            {/* 야간 모드 뱃지 */}
            {isNightMode && (
              <span
                className="text-xs px-2 py-0.5 rounded-full"
                style={{ background: 'rgba(167,139,250,0.15)', color: '#a78bfa' }}
              >
                🌙 야간
              </span>
            )}
          </div>

          {/* 검색창 */}
          <div className="flex gap-2">
            <input
              type="text"
              placeholder="목적지 검색..."
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && searchPlace()}
              className="flex-1 rounded-xl px-3 py-2 text-sm outline-none"
              style={{ background: 'var(--bg-card)', border: '1px solid var(--border)', color: 'var(--text-primary)' }}
            />
            <button
              onClick={searchPlace}
              className="px-3 py-2 rounded-xl text-sm font-medium"
              style={{ background: 'var(--accent-blue)', color: '#fff' }}
            >
              검색
            </button>
          </div>
        </div>

        {/* 검색 결과 */}
        {searchResults.length > 0 && (
          <div className="overflow-y-auto max-h-52" style={{ borderBottom: '1px solid var(--border)' }}>
            {searchResults.map((place, i) => (
              <button
                key={i}
                onClick={() => handleSelectPlace(place)}
                className="w-full text-left px-4 py-3 transition-colors"
                style={{ borderBottom: '1px solid var(--border)' }}
                onMouseEnter={e => (e.currentTarget.style.background = 'var(--bg-hover)')}
                onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}
              >
                <p className="text-sm font-medium" style={{ color: 'var(--text-primary)' }}>{place.name}</p>
                <p className="text-xs mt-0.5" style={{ color: 'var(--text-muted)' }}>{place.address}</p>
              </button>
            ))}
          </div>
        )}

        {/* 즐겨찾기 목록 */}
        {!searchResults.length && favorites.length > 0 && (
          <div className="p-3" style={{ borderBottom: '1px solid var(--border)' }}>
            <p className="text-xs font-medium mb-2" style={{ color: 'var(--text-muted)' }}>즐겨찾기</p>
            <div className="flex flex-col gap-1.5">
              {favorites.map((fav) => {
                const cfg = PLACE_TYPE_CONFIG[fav.placeType] ?? PLACE_TYPE_CONFIG.CUSTOM
                return (
                  <div key={fav.id} className="flex items-center gap-2">
                    <button
                      onClick={() => handleSelectPlace(fav)}
                      className="flex-1 flex items-center gap-2 px-3 py-2 rounded-xl text-left transition-all"
                      style={{
                        background: selectedPlace?.address === fav.address
                          ? 'rgba(79,126,248,0.1)' : 'var(--bg-card)',
                        border: `1px solid ${selectedPlace?.address === fav.address
                          ? 'rgba(79,126,248,0.3)' : 'var(--border)'}`,
                      }}
                    >
                      <span>{cfg.icon}</span>
                      <div className="min-w-0">
                        <p className="text-sm font-medium truncate" style={{ color: 'var(--text-primary)' }}>
                          {fav.name}
                        </p>
                        <p className="text-xs truncate" style={{ color: 'var(--text-muted)' }}>
                          {fav.address}
                        </p>
                      </div>
                    </button>
                    <button
                      onClick={() => deleteFavoriteMutation.mutate(fav.id)}
                      className="text-xs px-2 py-1 rounded-lg shrink-0"
                      style={{ color: 'var(--text-muted)' }}
                    >
                      ✕
                    </button>
                  </div>
                )
              })}
            </div>
          </div>
        )}

        {/* 선택된 목적지 */}
        {selectedPlace && (
          <div className="mx-3 mt-3 rounded-xl p-3" style={{ background: 'rgba(79,126,248,0.1)', border: '1px solid rgba(79,126,248,0.2)' }}>
            <p className="text-xs font-medium mb-1" style={{ color: 'var(--accent-blue)' }}>목적지</p>
            <p className="text-sm font-medium" style={{ color: 'var(--text-primary)' }}>{selectedPlace.name}</p>
            <p className="text-xs mt-0.5" style={{ color: 'var(--text-muted)' }}>{selectedPlace.address}</p>
            <div className="flex items-center justify-between mt-2">
              <button onClick={() => setSelectedPlace(null)} className="text-xs" style={{ color: 'var(--text-muted)' }}>
                취소
              </button>
              {/* 즐겨찾기 추가 버튼 */}
              {!favorites.find(f => f.address === selectedPlace.address) && (
                <button
                  onClick={() => setShowAddFavorite(!showAddFavorite)}
                  className="text-xs px-2 py-1 rounded-lg"
                  style={{ color: 'var(--accent-blue)', border: '1px solid rgba(79,126,248,0.3)' }}
                >
                  ⭐ 즐겨찾기 추가
                </button>
              )}
            </div>

            {/* 즐겨찾기 이름 입력 */}
            {showAddFavorite && (
              <div className="mt-2 flex gap-2">
                <input
                  value={favoriteLabel}
                  onChange={e => setFavoriteLabel(e.target.value)}
                  placeholder="이름 (예: 집, 직장)"
                  className="flex-1 rounded-lg px-2 py-1.5 text-xs outline-none"
                  style={{ background: 'var(--bg-hover)', border: '1px solid var(--border)', color: 'var(--text-primary)' }}
                  onKeyDown={e => e.key === 'Enter' && addFavoriteMutation.mutate()}
                />
                <button
                  onClick={() => addFavoriteMutation.mutate()}
                  disabled={addFavoriteMutation.isPending}
                  className="text-xs px-2 py-1.5 rounded-lg"
                  style={{ background: 'var(--accent-blue)', color: '#fff' }}
                >
                  저장
                </button>
              </div>
            )}
          </div>
        )}
        {/* 안전 경로 분석 */}
        {selectedPlace && (
          <div className="px-4 py-3" style={{ borderBottom: '1px solid var(--border)' }}>
            <p className="text-xs font-medium mb-2" style={{ color: 'var(--text-muted)' }}>
              경로 안전 분석
            </p>

            {loadingRoute ? (
              <div className="h-16 rounded-xl animate-pulse" style={{ background: 'var(--bg-card)' }} />
            ) : safeRoute ? (
              <>
                <div
                  className="rounded-xl p-3 mb-2"
                  style={{ background: 'var(--bg-card)', border: '1px solid var(--border)' }}
                >
                  {/* 안전점수 */}
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-xs" style={{ color: 'var(--text-secondary)' }}>
                      경로 안전점수
                    </span>
                    <span
                      className="text-sm font-bold"
                      style={{
                        color: safeRoute.safetyScore >= 60 ? 'var(--accent-green)'
                            : safeRoute.safetyScore >= 30 ? 'var(--accent-amber)'
                            : 'var(--accent-red)'
                      }}
                    >
                      {Math.round(safeRoute.safetyScore)}점
                    </span>
                  </div>
                  <div className="w-full rounded-full h-1.5 mb-3" style={{ background: 'var(--bg-hover)' }}>
                    <div
                      className="h-1.5 rounded-full transition-all"
                      style={{
                        width: `${Math.min(safeRoute.safetyScore, 100)}%`,
                        background: safeRoute.safetyScore >= 60 ? 'var(--accent-green)'
                                  : safeRoute.safetyScore >= 30 ? 'var(--accent-amber)'
                                  : 'var(--accent-red)',
                      }}
                    />
                  </div>

                  {/* 시설 현황 */}
                  <div className="grid grid-cols-3 gap-2 text-center mb-3">
                    <div>
                      <p className="text-sm font-bold" style={{ color: '#4f7ef8' }}>{safeRoute.totalCctv}</p>
                      <p className="text-xs" style={{ color: 'var(--text-muted)' }}>CCTV</p>
                    </div>
                    <div>
                      <p className="text-sm font-bold" style={{ color: '#f87171' }}>{safeRoute.totalBell}</p>
                      <p className="text-xs" style={{ color: 'var(--text-muted)' }}>비상벨</p>
                    </div>
                    <div>
                      <p className="text-sm font-bold" style={{ color: '#a78bfa' }}>{safeRoute.totalPolice}</p>
                      <p className="text-xs" style={{ color: 'var(--text-muted)' }}>경찰서</p>
                    </div>
                  </div>

                  {/* 안내 문구 */}
                  <div
                    className="rounded-lg px-3 py-2 text-xs leading-relaxed"
                    style={{
                      background: safeRoute.safetyScore >= 60
                        ? 'rgba(52,211,153,0.1)' : safeRoute.safetyScore >= 30
                        ? 'rgba(251,191,36,0.1)' : 'rgba(248,113,113,0.1)',
                      color: safeRoute.safetyScore >= 60
                        ? 'var(--accent-green)' : safeRoute.safetyScore >= 30
                        ? 'var(--accent-amber)' : 'var(--accent-red)',
                    }}
                  >
                    {safeRoute.totalCctv + safeRoute.totalBell + safeRoute.totalPolice > 0 ? (
                      <>
                        이 경로 주변에 CCTV {safeRoute.totalCctv}개,
                        비상벨 {safeRoute.totalBell}개가 있어요.<br />
                        지도의 마커 근처로 이동하면 더 안전해요. 🔵
                      </>
                    ) : (
                      <>
                        ⚠️ 이 경로 주변에 안전시설이 부족해요.<br />
                        가능하면 밝은 길이나 사람이 많은 곳으로 이동하세요.
                      </>
                    )}
                  </div>
                </div>

                {/* 카카오맵 길찾기 */}
                <a
                  href={`https://map.kakao.com/link/to/${encodeURIComponent(selectedPlace.name)},${selectedPlace.lat},${selectedPlace.lng}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="w-full flex items-center justify-center gap-2 rounded-xl py-2.5 text-sm font-medium transition-all"
                  style={{ background: '#FEE500', color: '#3C1E1E' }}
                >
                  <span>🗺</span>
                  카카오맵 길찾기 (참고용)
                </a>
              </>
            ) : null}
          </div>
        )}
        

        {/* 소요 시간 */}
        <div className="p-4" style={{ borderBottom: '1px solid var(--border)' }}>
          <div className="flex items-center justify-between mb-2">
            <label className="text-sm" style={{ color: 'var(--text-secondary)' }}>예상 소요 시간</label>
            <span className="text-sm font-semibold" style={{ color: 'var(--accent-blue)' }}>{estimatedMinutes}분</span>
          </div>
          <input
            type="range" min={5} max={60} step={5}
            value={estimatedMinutes}
            onChange={e => setEstimatedMinutes(Number(e.target.value))}
            className="w-full"
          />
          <div className="flex justify-between text-xs mt-1" style={{ color: 'var(--text-muted)' }}>
            <span>5분</span><span>60분</span>
          </div>
        </div>

        {/* 안내 */}
        {!selectedPlace && !searchResults.length && (
          <div className="flex-1 flex items-center justify-center p-4">
            <p className="text-sm text-center" style={{ color: 'var(--text-muted)' }}>
              주소를 검색하거나<br />즐겨찾기에서 선택하거나<br />지도를 클릭하세요
            </p>
          </div>
        )}

        {/* 귀가 시작 버튼 */}
        <div className="p-4">
          <button
            onClick={() => startMutation.mutate()}
            disabled={!selectedPlace || startMutation.isPending}
            className="w-full rounded-xl py-3 text-sm font-medium transition-all disabled:opacity-40"
            style={{ background: isNightMode ? '#a78bfa' : 'var(--accent-blue)', color: '#fff' }}
          >
            {startMutation.isPending ? '시작 중...' : isNightMode ? '🌙 야간 안심 귀가 시작' : '귀가 시작'}
          </button>
        </div>
      </aside>

      {/* 지도 */}
      <div className="flex-1 relative">
        <Map
          center={selectedPlace ? { lat: selectedPlace.lat, lng: selectedPlace.lng } : position}
          style={{ width: '100%', height: '100%' }}
          level={4}
          onClick={handleMapClick}
        >
          <MapMarker
            position={position}
            image={{
              src:  'https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/markerStar.png',
              size: { width: 24, height: 35 },
            }}
          />
          {selectedPlace && (
            <MapMarker position={{ lat: selectedPlace.lat, lng: selectedPlace.lng }} />
          )}
            {/* 안전 경유 지점 마커 추가 */}
          {safeRoute?.safePoints.map((point, i) => (
            <MapMarker
              key={`route-${i}`}
              position={{ lat: point.lat, lng: point.lng }}
              image={getFacilityMarkerImage(point.type)}
            />
          ))}
        </Map>

        {/* 지도 안내 */}
        <div
          className="absolute top-3 left-1/2 -translate-x-1/2 px-4 py-1.5 rounded-full text-xs pointer-events-none"
          style={{ background: 'rgba(15,17,23,0.8)', color: 'var(--text-secondary)', border: '1px solid var(--border)', backdropFilter: 'blur(8px)' }}
        >
          지도를 클릭하면 목적지로 설정돼요
        </div>
      </div>
    </div>
  )
}