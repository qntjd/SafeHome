import { useState, useCallback } from 'react'
import { useMutation } from '@tanstack/react-query'
import { Map, MapMarker } from 'react-kakao-maps-sdk'
import { tripApi } from '@/api/trip'
import type { TripResponse } from '@/api/trip'
import { useCurrentLocation } from '@/hooks/useCurrentLocation'
import TripStatusCard from '@/components/trip/TripStatusCard'

interface Place {
  name: string
  address: string
  lat: number
  lng: number
}

export default function TripPage() {
  const { position } = useCurrentLocation()
  const [activeTrip, setActiveTrip] = useState<TripResponse | null>(null)
  const [searchKeyword, setSearchKeyword] = useState('')
  const [searchResults, setSearchResults] = useState<Place[]>([])
  const [selectedPlace, setSelectedPlace] = useState<Place | null>(null)
  const [estimatedMinutes, setEstimatedMinutes] = useState(15)

  const searchPlace = useCallback(() => {
    if (!searchKeyword.trim()) return
    const ps = new window.kakao.maps.services.Places()
    ps.keywordSearch(searchKeyword, (result: any[], status: string) => {
      if (status === window.kakao.maps.services.Status.OK) {
        setSearchResults(result.map((item) => ({
          name: item.place_name,
          address: item.road_address_name || item.address_name,
          lat: parseFloat(item.y),
          lng: parseFloat(item.x),
        })))
      } else {
        setSearchResults([])
      }
    })
  }, [searchKeyword])

  const handleMapClick = useCallback((_: any, mouseEvent: any) => {
    const lat = mouseEvent.latLng.getLat()
    const lng = mouseEvent.latLng.getLng()
    const geocoder = new window.kakao.maps.services.Geocoder()
    geocoder.coord2Address(lng, lat, (result: any[], status: string) => {
      if (status === window.kakao.maps.services.Status.OK) {
        const addr =
          result[0].road_address?.address_name ||
          result[0].address?.address_name || '주소 없음'
        setSelectedPlace({ name: addr, address: addr, lat, lng })
        setSearchResults([])
        setSearchKeyword('')
      }
    })
  }, [])

  const handleSelectPlace = (place: Place) => {
    setSelectedPlace(place)
    setSearchResults([])
    setSearchKeyword('')
  }

  const startMutation = useMutation({
    mutationFn: () => tripApi.start({
      startLat: position.lat, startLng: position.lng,
      endLat: selectedPlace!.lat, endLng: selectedPlace!.lng,
      estimatedMinutes,
    }),
    onSuccess: ({ data }) => setActiveTrip(data.data),
    onError: () => alert('귀가 시작에 실패했습니다.'),
  })

  const arriveMutation = useMutation({
    mutationFn: () => tripApi.arrive(activeTrip!.id),
    onSuccess: ({ data }) => setActiveTrip(data.data),
  })

  const sosMutation = useMutation({
    mutationFn: () => tripApi.sos(activeTrip!.id),
    onSuccess: ({ data }) => setActiveTrip(data.data),
  })

  const cancelMutation = useMutation({
    mutationFn: () => tripApi.cancel(activeTrip!.id),
    onSuccess: () => setActiveTrip(null),
  })

  if (activeTrip) {
    return (
      <div
        className="min-h-full flex items-start justify-center px-4 py-8"
        style={{ background: 'var(--bg-primary)' }}
      >
        <div className="w-full max-w-md">
          <h1 className="text-xl font-semibold mb-6" style={{ color: 'var(--text-primary)' }}>
            안심 귀가
          </h1>
          <TripStatusCard
            trip={activeTrip}
            onArrive={() => arriveMutation.mutate()}
            onSos={() => sosMutation.mutate()}
            onCancel={() => cancelMutation.mutate()}
            isLoading={arriveMutation.isPending || sosMutation.isPending}
          />
        </div>
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
          <h1 className="text-base font-semibold mb-3" style={{ color: 'var(--text-primary)' }}>
            안심 귀가
          </h1>
          <div className="flex gap-2">
            <input
              type="text"
              placeholder="목적지 검색..."
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && searchPlace()}
              className="flex-1 rounded-xl px-3 py-2 text-sm outline-none"
              style={{
                background: 'var(--bg-card)',
                border: '1px solid var(--border)',
                color: 'var(--text-primary)',
              }}
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
          <div
            className="overflow-y-auto max-h-52"
            style={{ borderBottom: '1px solid var(--border)' }}
          >
            {searchResults.map((place, i) => (
              <button
                key={i}
                onClick={() => handleSelectPlace(place)}
                className="w-full text-left px-4 py-3 transition-colors"
                style={{ borderBottom: '1px solid var(--border)' }}
                onMouseEnter={e => (e.currentTarget.style.background = 'var(--bg-hover)')}
                onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}
              >
                <p className="text-sm font-medium" style={{ color: 'var(--text-primary)' }}>
                  {place.name}
                </p>
                <p className="text-xs mt-0.5" style={{ color: 'var(--text-muted)' }}>
                  {place.address}
                </p>
              </button>
            ))}
          </div>
        )}

        {/* 선택된 목적지 */}
        {selectedPlace && (
          <div
            className="mx-3 mt-3 rounded-xl p-3"
            style={{ background: 'rgba(79,126,248,0.1)', border: '1px solid rgba(79,126,248,0.2)' }}
          >
            <p className="text-xs font-medium mb-1" style={{ color: 'var(--accent-blue)' }}>
              목적지
            </p>
            <p className="text-sm font-medium" style={{ color: 'var(--text-primary)' }}>
              {selectedPlace.name}
            </p>
            <p className="text-xs mt-0.5" style={{ color: 'var(--text-muted)' }}>
              {selectedPlace.address}
            </p>
            <button
              onClick={() => setSelectedPlace(null)}
              className="text-xs mt-2"
              style={{ color: 'var(--text-muted)' }}
            >
              취소
            </button>
          </div>
        )}

        {/* 소요 시간 */}
        <div className="p-4" style={{ borderBottom: '1px solid var(--border)' }}>
          <div className="flex items-center justify-between mb-2">
            <label className="text-sm" style={{ color: 'var(--text-secondary)' }}>
              예상 소요 시간
            </label>
            <span className="text-sm font-semibold" style={{ color: 'var(--accent-blue)' }}>
              {estimatedMinutes}분
            </span>
          </div>
          <input
            type="range" min={5} max={60} step={5}
            value={estimatedMinutes}
            onChange={(e) => setEstimatedMinutes(Number(e.target.value))}
            className="w-full"
          />
          <div className="flex justify-between text-xs mt-1" style={{ color: 'var(--text-muted)' }}>
            <span>5분</span><span>60분</span>
          </div>
        </div>

        {/* 안내 */}
        {!selectedPlace && (
          <div className="flex-1 flex items-center justify-center p-4">
            <p className="text-sm text-center" style={{ color: 'var(--text-muted)' }}>
              주소를 검색하거나<br />지도를 클릭해서<br />목적지를 설정하세요
            </p>
          </div>
        )}

        {/* 귀가 시작 버튼 */}
        <div className="p-4">
          <button
            onClick={() => startMutation.mutate()}
            disabled={!selectedPlace || startMutation.isPending}
            className="w-full rounded-xl py-3 text-sm font-medium transition-all disabled:opacity-40"
            style={{ background: 'var(--accent-blue)', color: '#fff' }}
          >
            {startMutation.isPending ? '시작 중...' : '귀가 시작'}
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
              src: 'https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/markerStar.png',
              size: { width: 24, height: 35 },
            }}
          />
          {selectedPlace && (
            <MapMarker position={{ lat: selectedPlace.lat, lng: selectedPlace.lng }} />
          )}
        </Map>

        {/* 지도 안내 */}
        <div
          className="absolute top-3 left-1/2 -translate-x-1/2 px-4 py-1.5 rounded-full text-xs pointer-events-none"
          style={{
            background: 'rgba(15,17,23,0.8)',
            color: 'var(--text-secondary)',
            border: '1px solid var(--border)',
            backdropFilter: 'blur(8px)',
          }}
        >
          지도를 클릭하면 목적지로 설정돼요
        </div>
      </div>
    </div>
  )
}