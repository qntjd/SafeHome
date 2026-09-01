import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { Map, Footprints, BarChart3, Newspaper, ChevronRight, Camera, BellRing, ClipboardCheck } from 'lucide-react'
import { useAuthStore } from '@/store/authStore'
import { safetyApi } from '@/api/safety'
import { newsApi } from '@/api/news'
import { useCurrentLocation } from '@/hooks/useCurrentLocation'
import Footer from '@/components/Footer'
import Lighthouse from '@/components/Lighthouse'
import { aiApi } from '@/api/ai'

const GRADE_CONFIG: Record<string, { color: string; bg: string; label: string }> = {
  A: { color: 'var(--grade-a)', bg: 'var(--grade-a-bg)', label: '매우 안전' },
  B: { color: 'var(--grade-b)', bg: 'var(--grade-b-bg)', label: '안전' },
  C: { color: 'var(--grade-c)', bg: 'var(--grade-c-bg)', label: '보통' },
  D: { color: 'var(--grade-d)', bg: 'var(--grade-d-bg)', label: '주의' },
  F: { color: 'var(--grade-f)', bg: 'var(--grade-f-bg)', label: '위험' },
}

const QUICK_LINKS = [
  { path: '/map',       label: '안전 지도',       desc: '주변 CCTV·비상벨·경찰서', icon: Map,            color: 'var(--accent-blue)' },
  { path: '/crime',     label: '범죄 통계',       desc: '지역별 발생 현황',        icon: BarChart3,      color: 'var(--accent-purple)' },
  { path: '/checklist', label: '집 안심 체크리스트', desc: '집 안 안전도 점검',      icon: ClipboardCheck, color: 'var(--grade-a)' },
  { path: '/news',      label: '안전 뉴스',       desc: '최신 안전 소식',          icon: Newspaper,      color: 'var(--accent-amber-deep)' },
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

  const { data: facilityCounts } = useQuery({
    queryKey: ['facility-counts-dash', position],
    queryFn: () => safetyApi.getFacilityCounts(position.lat, position.lng, 500),
    enabled: !!position,
  })

  const { data: news } = useQuery({
    queryKey: ['news-dash'],
    queryFn: () => newsApi.getNews(0, 4),
  })

  const districts = heatmap?.data?.data?.districts ?? []
  const topDistricts = [...districts].sort((a, b) => b.totalScore - a.totalScore).slice(0, 4)
  const cctvCount = facilityCounts?.data?.data?.cctvCount ?? 0
  const bellCount = facilityCounts?.data?.data?.bellCount ?? 0
  const recentNews = news?.data?.data?.articles ?? []

  const avgScore = districts.length
    ? Math.round(districts.reduce((s, d) => s + d.totalScore, 0) / districts.length)
    : null

  const topDistrict = topDistricts[0]

  const { data: briefing } = useQuery({
    queryKey: ['ai-briefing', topDistrict?.districtCode],
    queryFn: () => aiApi.getBriefing(
      topDistrict!.districtCode,
      topDistrict!.districtName,
      Math.round(topDistrict!.totalScore),
      topDistrict!.grade
    ),
    enabled: !!topDistrict,
    staleTime: 1000 * 60 * 60, // 1시간
  })
  
  const statCards = [
    { label: '지역 평균 안전점수', value: avgScore ?? '--', icon: BarChart3, color: 'var(--accent-blue)' },
    { label: '반경 500m CCTV', value: cctvCount, icon: Camera, color: 'var(--accent-blue)' },
    { label: '반경 500m 비상벨', value: bellCount, icon: BellRing, color: 'var(--accent-amber-deep)' },
    { label: '분석 지역 수', value: districts.length, icon: Map, color: 'var(--accent-purple)' },
  ]

  return (
    <div
      className="min-h-full pb-20 sm:pb-8 relative overflow-hidden"
      style={{ background: 'linear-gradient(165deg, #0a0e1f 0%, #04050c 75%)' }}
    >
      {/* 배경 등대 사이즈 */}
      <div
        className="absolute pointer-events-none w-[360px] h-[360px] sm:w-[500px] sm:h-[500px] lg:w-[640px] lg:h-[640px] xl:w-[740px] xl:h-[740px]"
        style={{ right: '-8%', top: '-4%', opacity: 0.9 }}
      >
        <Lighthouse fill />
      </div>

      <div className="max-w-4xl mx-auto px-4 sm:px-6 pt-7 relative lg:mx-0 lg:ml-10 xl:ml-16 2xl:ml-24">

          {/* 인사말 */}
          <div className="relative mb-5">
            <div
              className="absolute -inset-x-4 -inset-y-3 sm:-inset-x-6 pointer-events-none"
              style={{ background: 'radial-gradient(120% 160% at 0% 0%, rgba(4,5,12,0.7) 0%, transparent 70%)' }}
            />
            <div className="relative">
              <p className="text-sm mb-1.5 font-mono" style={{ color: 'var(--ink-text-muted)' }}>
                {now.toLocaleDateString('ko-KR', { month: 'long', day: 'numeric', weekday: 'short' })}
              </p>
              <h1 className="font-display font-black text-2xl sm:text-3xl mb-1.5" style={{ color: 'var(--ink-text)' }}>
                {greeting}, {nickname}님
              </h1>
              <p className="text-sm flex items-center gap-2" style={{ color: 'var(--ink-text-muted)' }}>
                <span className="beacon-dot" />
                오늘도 안전한 하루 되세요
              </p>
            </div>
          </div>
        {briefing?.data?.data && (
          <div
            className="relative z-10 rounded-2xl p-4 mb-4"
            style={{
              background: 'color-mix(in srgb, var(--bg-card) 78%, transparent)',
              backdropFilter: 'blur(14px)',
              WebkitBackdropFilter: 'blur(14px)',
              border: '1px solid var(--border)',
            }}
          >
            <p className="text-xs mb-2 font-semibold flex items-center gap-1.5" style={{ color: 'var(--accent-blue)' }}>
              ✨ AI 안전 브리핑
            </p>
            <p className="text-sm leading-relaxed" style={{ color: 'var(--text-primary)' }}>
              {briefing.data.data}
            </p>
          </div>
        )}  

        {/* 안심 귀가  */}
        <Link
          to="/trip"
          className="relative z-10 flex items-center justify-between gap-4 p-5 mb-4 transition-transform"
          style={{
            borderRadius: 28,
            background: 'linear-gradient(135deg, var(--ink) 0%, var(--ink-2) 100%)',
            boxShadow: 'var(--shadow-md)',
          }}
          onMouseEnter={e => (e.currentTarget.style.transform = 'translateY(-2px)')}
          onMouseLeave={e => (e.currentTarget.style.transform = 'translateY(0)')}
        >
          <div className="flex items-center gap-3 min-w-0">
            <div
              className="w-12 h-12 rounded-full flex items-center justify-center shrink-0"
              style={{ background: 'var(--accent-amber)' }}
            >
              <Footprints size={22} color="var(--ink)" strokeWidth={2.25} />
            </div>
            <div className="min-w-0">
              <p className="font-display font-bold text-base mb-0.5 flex items-center gap-2" style={{ color: 'var(--ink-text)' }}>
                안심 귀가 시작하기
                <span className="beacon-dot" />
              </p>
              <p className="text-xs truncate" style={{ color: 'var(--ink-text-muted)' }}>
                목적지까지 안전 경로를 안내해요
              </p>
            </div>
          </div>
          <div
            className="w-9 h-9 rounded-full flex items-center justify-center shrink-0"
            style={{ background: 'rgba(255,255,255,0.08)' }}
          >
            <ChevronRight size={18} color="var(--ink-text)" />
          </div>
        </Link>

        {/* 종합 현황 */}
        <div className="flex flex-col gap-4 mb-6">
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            {statCards.map(({ label, value, icon: Icon, color }) => (
              <div
                key={label}
                className="rounded-2xl p-3.5"
                style={{
                  background: 'color-mix(in srgb, var(--bg-card) 78%, transparent)',
                  backdropFilter: 'blur(14px)',
                  WebkitBackdropFilter: 'blur(14px)',
                  border: '1px solid var(--border)',
                }}
              >
                <div className="flex items-start justify-between mb-2">
                  <div
                    className="w-8 h-8 rounded-full flex items-center justify-center shrink-0"
                    style={{ background: 'var(--bg-hover)', border: '1px solid var(--border)' }}
                  >
                    <Icon size={15} color={color} strokeWidth={2} />
                  </div>
                </div>
                <p className="font-mono text-lg font-bold leading-none mb-1" style={{ color }}>
                  {value}
                </p>
                <p className="text-[11px]" style={{ color: 'var(--text-muted)' }}>{label}</p>
              </div>
            ))}
          </div>

          {/* 동네별 안심 지수 */}
          <div
            className="rounded-2xl p-4"
            style={{
              background: 'color-mix(in srgb, var(--bg-card) 78%, transparent)',
              backdropFilter: 'blur(14px)',
              WebkitBackdropFilter: 'blur(14px)',
              border: '1px solid var(--border)',
            }}
          >
            <div className="flex items-center justify-between mb-4">
              <h2 className="font-display font-bold text-sm flex items-center gap-1.5" style={{ color: 'var(--text-primary)' }}>
                <Camera size={15} color="var(--text-muted)" />
                동네별 안심 지수
              </h2>
              <Link to="/map" className="text-xs font-semibold" style={{ color: 'var(--accent-blue)' }}>
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
                        className="w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold shrink-0 font-mono"
                        style={{ background: i === 0 ? 'var(--accent-amber-soft)' : 'var(--bg-primary)', color: i === 0 ? 'var(--accent-amber-deep)' : 'var(--text-muted)' }}
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
                      <span className="text-xs font-bold shrink-0 font-mono" style={{ color: 'var(--text-secondary)' }}>
                        {Math.round(d.totalScore)}
                      </span>
                    </div>
                  )
                })
              )}
            </div>
          </div>
        </div>

        {/* 바로가기 */}
        <div
          className="rounded-2xl mb-6 overflow-hidden"
          style={{
            background: 'color-mix(in srgb, var(--bg-card) 78%, transparent)',
            backdropFilter: 'blur(14px)',
            WebkitBackdropFilter: 'blur(14px)',
            border: '1px solid var(--border)',
          }}
        >
          {QUICK_LINKS.map(({ path, label, desc, icon: Icon, color }, i) => (
            <Link
              key={path}
              to={path}
              className="flex items-center gap-3 px-4 py-3.5 transition-colors"
              style={{ borderTop: i > 0 ? '1px solid var(--border)' : undefined }}
              onMouseEnter={e => (e.currentTarget.style.background = 'var(--bg-hover)')}
              onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}
            >
              <div
                className="w-10 h-10 rounded-full flex items-center justify-center shrink-0"
                style={{ background: 'var(--bg-hover)', border: '1px solid var(--border)' }}
              >
                <Icon size={18} color={color} strokeWidth={2} />
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-semibold" style={{ color: 'var(--text-primary)' }}>{label}</p>
                <p className="text-xs" style={{ color: 'var(--text-muted)' }}>{desc}</p>
              </div>
              <ChevronRight size={16} color="var(--text-muted)" className="shrink-0" />
            </Link>
          ))}
        </div>

        {/* 최근 안전 뉴스 */}
        <div
          className="rounded-2xl p-4 mb-6"
          style={{
            background: 'color-mix(in srgb, var(--bg-card) 78%, transparent)',
            backdropFilter: 'blur(14px)',
            WebkitBackdropFilter: 'blur(14px)',
            border: '1px solid var(--border)',
          }}
        >
          <div className="flex items-center justify-between mb-4">
            <h2 className="font-display font-bold text-sm flex items-center gap-1.5" style={{ color: 'var(--text-primary)' }}>
              <BellRing size={15} color="var(--text-muted)" />
              최근 안전 뉴스
            </h2>
            <Link to="/news" className="text-xs font-semibold" style={{ color: 'var(--accent-blue)' }}>
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
                    style={{ background: 'var(--accent-amber)', minHeight: 36 }}
                  />
                  <div className="min-w-0">
                    <p className="text-xs font-medium line-clamp-2 leading-relaxed group-hover:underline"
                      style={{ color: 'var(--text-primary)' }}>
                      {article.title}
                    </p>
                    <p className="text-xs mt-0.5 font-mono" style={{ color: 'var(--text-muted)' }}>
                      {new Date(article.publishedAt).toLocaleDateString('ko-KR')}
                    </p>
                  </div>
                </a>
              ))
            )}
          </div>
        </div>

      </div>
      <Footer />
    </div>
  )
}