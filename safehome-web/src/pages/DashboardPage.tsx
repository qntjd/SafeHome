import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'
import { safetyApi } from '@/api/safety'
import { newsApi } from '@/api/news'
import { useCurrentLocation } from '@/hooks/useCurrentLocation'
import Footer from '@/components/Footer'

const GRADE_CONFIG: Record<string, { color: string; bg: string; label: string }> = {
  A: { color: '#059669', bg: '#ecfdf5', label: '매우 안전' },
  B: { color: '#2563eb', bg: '#eff6ff', label: '안전' },
  C: { color: '#d97706', bg: '#fffbeb', label: '보통' },
  D: { color: '#ea580c', bg: '#fff7ed', label: '주의' },
  F: { color: '#dc2626', bg: '#fef2f2', label: '위험' },
}

const QUICK_MENUS = [
  { path: '/map',   label: '안전 지도',  desc: '주변 안전시설 확인',  color: '#1d4ed8', bg: '#eff6ff',  icon: '🗺' },
  { path: '/trip',  label: '안심 귀가',  desc: '귀가 시작하기',       color: '#059669', bg: '#ecfdf5',  icon: '🚶' },
  { path: '/crime', label: '범죄 통계',  desc: '지역별 범죄 현황',    color: '#7c3aed', bg: '#f5f3ff',  icon: '📊' },
  { path: '/news',  label: '안전 뉴스',  desc: '최신 안전 정보',      color: '#0891b2', bg: '#ecfeff',  icon: '📰' },
]

export default function DashboardPage() {
  const nickname = useAuthStore((s) => s.nickname)
  const { position } = useCurrentLocation()
  const now = new Date()
  const hour = now.getHours()
  const greeting = hour < 12 ? '좋은 아침이에요' : hour < 18 ? '안녕하세요' : '안녕하세요'

  const { data: heatmap } = useQuery({
    queryKey: ['heatmap'],
    queryFn: () => safetyApi.getHeatmap(),
  })

  const { data: facilities } = useQuery({
    queryKey: ['facilities-dash', position],
    queryFn: () => safetyApi.getFacilities(position.lat, position.lng, 500),
    enabled: !!position,
  })

  const { data: news } = useQuery({
    queryKey: ['news-dash'],
    queryFn: () => newsApi.getNews(0, 4),
  })

  const districts = heatmap?.data?.data?.districts ?? []
  const topDistricts = [...districts].sort((a, b) => b.totalScore - a.totalScore).slice(0, 3)
  const facilityList = facilities?.data?.data ?? []
  const cctvCount = facilityList.filter((f) => f.type === 'CCTV').length
  const bellCount = facilityList.filter((f) => f.type === 'EMERGENCY_BELL').length
  const recentNews = news?.data?.data?.articles ?? []

  const avgScore = districts.length
    ? Math.round(districts.reduce((s, d) => s + d.totalScore, 0) / districts.length)
    : null

  return (
    <div className="min-h-full pb-20 sm:pb-8" style={{ background: 'var(--bg-primary)' }}>

      {/* 헤더 배너 */}
      <div style={{ background: 'var(--accent-blue)' }} className="px-4 sm:px-6 pt-6 pb-8">
        <div className="max-w-4xl mx-auto">
          <p className="text-sm mb-1" style={{ color: 'rgba(255,255,255,0.7)' }}>
            {now.toLocaleDateString('ko-KR', { month: 'long', day: 'numeric', weekday: 'short' })}
          </p>
          <h1 className="text-xl sm:text-2xl font-bold text-white mb-1">
            {greeting}, {nickname}님
          </h1>
          <p className="text-sm" style={{ color: 'rgba(255,255,255,0.75)' }}>
            오늘도 안전한 하루 되세요
          </p>
        </div>
      </div>

      <div className="max-w-4xl mx-auto px-4 sm:px-6 -mt-4">

        {/* 안전 현황 요약 카드 */}
        <div
          className="rounded-2xl p-4 mb-6 grid grid-cols-3 gap-4"
          style={{ background: 'var(--bg-card)', boxShadow: 'var(--shadow-md)' }}
        >
          <div className="text-center">
            <p className="text-xs font-medium mb-1" style={{ color: 'var(--text-muted)' }}>지역 평균 안전점수</p>
            <p className="text-2xl font-bold" style={{ color: 'var(--accent-blue)' }}>
              {avgScore ?? '--'}
            </p>
            <p className="text-xs" style={{ color: 'var(--text-muted)' }}>/ 100점</p>
          </div>
          <div className="text-center" style={{ borderLeft: '1px solid var(--border)', borderRight: '1px solid var(--border)' }}>
            <p className="text-xs font-medium mb-1" style={{ color: 'var(--text-muted)' }}>주변 CCTV</p>
            <p className="text-2xl font-bold" style={{ color: 'var(--accent-blue)' }}>{cctvCount}</p>
            <p className="text-xs" style={{ color: 'var(--text-muted)' }}>반경 500m</p>
          </div>
          <div className="text-center">
            <p className="text-xs font-medium mb-1" style={{ color: 'var(--text-muted)' }}>비상벨</p>
            <p className="text-2xl font-bold" style={{ color: 'var(--accent-blue)' }}>{bellCount}</p>
            <p className="text-xs" style={{ color: 'var(--text-muted)' }}>반경 500m</p>
          </div>
        </div>

        {/* 빠른 메뉴 */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-6">
          {QUICK_MENUS.map(({ path, label, desc, color, bg, icon }) => (
            <Link
              key={path}
              to={path}
              className="rounded-2xl p-4 transition-all"
              style={{ background: bg, border: `1px solid ${color}20` }}
              onMouseEnter={e => (e.currentTarget.style.transform = 'translateY(-2px)')}
              onMouseLeave={e => (e.currentTarget.style.transform = 'translateY(0)')}
            >
              <div className="text-2xl mb-2">{icon}</div>
              <p className="text-sm font-semibold mb-0.5" style={{ color }}>{label}</p>
              <p className="text-xs" style={{ color: `${color}99` }}>{desc}</p>
            </Link>
          ))}
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-6">

          {/* 안전점수 TOP 3 */}
          <div className="rounded-2xl p-4" style={{ background: 'var(--bg-card)', boxShadow: 'var(--shadow-sm)' }}>
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-sm font-semibold" style={{ color: 'var(--text-primary)' }}>
                안전점수 TOP 3
              </h2>
              <Link to="/map" className="text-xs font-medium" style={{ color: 'var(--accent-blue-light)' }}>
                전체보기
              </Link>
            </div>
            <div className="flex flex-col gap-3">
              {topDistricts.length === 0 ? (
                <p className="text-xs text-center py-4" style={{ color: 'var(--text-muted)' }}>
                  데이터 없음
                </p>
              ) : (
                topDistricts.map((d, i) => {
                  const gc = GRADE_CONFIG[d.grade] ?? GRADE_CONFIG.C
                  return (
                    <div key={d.districtCode} className="flex items-center gap-3">
                      <span
                        className="w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold shrink-0"
                        style={{ background: i === 0 ? '#fef9c3' : 'var(--bg-primary)', color: i === 0 ? '#854d0e' : 'var(--text-muted)' }}
                      >
                        {i + 1}
                      </span>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center justify-between mb-1">
                          <p className="text-xs font-medium truncate" style={{ color: 'var(--text-primary)' }}>
                            {d.districtName}
                          </p>
                          <span className="text-xs font-semibold ml-2" style={{ color: gc.color }}>
                            {d.grade}등급
                          </span>
                        </div>
                        <div className="w-full h-1.5 rounded-full" style={{ background: 'var(--bg-primary)' }}>
                          <div
                            className="h-1.5 rounded-full transition-all"
                            style={{ width: `${d.totalScore}%`, background: gc.color }}
                          />
                        </div>
                      </div>
                      <span className="text-xs font-bold shrink-0" style={{ color: 'var(--text-secondary)' }}>
                        {Math.round(d.totalScore)}
                      </span>
                    </div>
                  )
                })
              )}
            </div>
          </div>

          {/* 최근 안전 뉴스 */}
          <div className="rounded-2xl p-4" style={{ background: 'var(--bg-card)', boxShadow: 'var(--shadow-sm)' }}>
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-sm font-semibold" style={{ color: 'var(--text-primary)' }}>
                최근 안전 뉴스
              </h2>
              <Link to="/news" className="text-xs font-medium" style={{ color: 'var(--accent-blue-light)' }}>
                전체보기
              </Link>
            </div>
            <div className="flex flex-col gap-3">
              {recentNews.length === 0 ? (
                <p className="text-xs text-center py-4" style={{ color: 'var(--text-muted)' }}>
                  뉴스 없음
                </p>
              ) : (
                recentNews.map((article) => (
                  <a
                    key={article.id}
                    href={article.url}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="flex gap-3 group"
                  >
                    <div
                      className="w-1 rounded-full shrink-0 mt-1"
                      style={{ background: 'var(--accent-blue-light)', minHeight: 36 }}
                    />
                    <div className="min-w-0">
                      <p className="text-xs font-medium line-clamp-2 leading-relaxed group-hover:underline"
                        style={{ color: 'var(--text-primary)' }}>
                        {article.title}
                      </p>
                      <p className="text-xs mt-0.5" style={{ color: 'var(--text-muted)' }}>
                        {new Date(article.publishedAt).toLocaleDateString('ko-KR')}
                      </p>
                    </div>
                  </a>
                ))
              )}
            </div>
          </div>
        </div>

        {/* 안심 귀가 CTA */}
        <div
          className="rounded-2xl p-5 flex items-center justify-between"
          style={{ background: 'linear-gradient(135deg, #1d4ed8 0%, #2563eb 100%)', boxShadow: '0 4px 14px rgba(29,78,216,0.3)' }}
        >
          <div>
            <p className="text-white font-semibold mb-1">안심 귀가 시작하기</p>
            <p className="text-sm" style={{ color: 'rgba(255,255,255,0.75)' }}>
              목적지까지 안전하게 귀가하세요
            </p>
          </div>
          <Link
            to="/trip"
            className="px-4 py-2 rounded-xl text-sm font-semibold transition-all shrink-0"
            style={{ background: 'white', color: 'var(--accent-blue)' }}
          >
            시작하기 →
          </Link>
        </div>
          
      </div>
      <Footer />
    </div>
  )
}