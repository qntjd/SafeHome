import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  RadarChart, Radar, PolarGrid, PolarAngleAxis,
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip,
  ResponsiveContainer, Cell,
} from 'recharts'
import { BarChart3, Radar as RadarIcon, ChevronDown, ChevronRight } from 'lucide-react'
import { crimeApi } from '@/api/crime'
import type { DistrictCrimeResponse, SidoGroupResponse } from '@/api/crime'
import Footer from '@/components/Footer'

// Recharts는 fill/stroke에 리터럴 hex 문자열이 필요하므로 index.css의 실제 값을 그대로 사용한다.
const CRIME_COLORS: Record<string, string> = {
  '강력범죄':   '#d7263d', // --grade-f
  '폭행':       '#d97a34', // --grade-d
  '절도':       '#e8a33d', // --grade-c / --accent-amber
  '사기·지능':  '#7c5cbf', // --accent-purple
  '풍속·마약':  '#c97e1d', // --accent-amber-deep
  '교통범죄':   '#0b6e82', // --accent-blue
  '기타':       '#8996ac', // --text-muted
}
const CRIME_COLOR_FALLBACK = '#8996ac' // --text-muted

const SIDO_COLORS = [
  '#0b6e82', '#1e8a6e', '#d7263d', '#e8a33d', '#7c5cbf',
  '#d97a34', '#1690a3', '#c97e1d',
]

// 레이더 차트용 데이터 변환
function toRadarData(district: DistrictCrimeResponse) {
  return Object.entries(district.crimeByType).map(([type, count]) => ({
    type,
    count,
    fullMark: Math.max(...Object.values(district.crimeByType)),
  }))
}

// 바 차트용 데이터 변환 (시/도 단위)
function toBarData(sidoGroups: SidoGroupResponse[]) {
  return sidoGroups.map((g) => ({
    name: g.sidoName,
    ...g.crimeByType,
    total: g.totalCount,
  }))
}

export default function CrimeStatsPage() {
  const [selectedDistrict, setSelectedDistrict] = useState<string>('')
  const [expandedSido, setExpandedSido] = useState<Set<string>>(new Set())
  const [chartType, setChartType] = useState<'bar' | 'radar'>('bar')

  const { data: groupedData, isLoading: groupedLoading } = useQuery({
    queryKey: ['crimes-grouped'],
    queryFn: () => crimeApi.getAllCrimesGrouped(),
  })

  const { data: flatData } = useQuery({
    queryKey: ['crimes-flat'],
    queryFn: () => crimeApi.getAllCrimes(),
  })

  const sidoGroups = [...(groupedData?.data?.data?.sidoGroups ?? [])]
    .sort((a, b) => b.totalCount - a.totalCount)
  const flatDistricts = flatData?.data?.data?.districts ?? []

  const selected  = flatDistricts.find(d => d.districtCode === selectedDistrict) ?? flatDistricts[0]
  const barData   = toBarData(sidoGroups)
  const radarData = selected ? toRadarData(selected) : []

  const crimeTypes = Array.from(
    new Set(sidoGroups.flatMap(g => Object.keys(g.crimeByType)))
  )

  const toggleSido = (sidoCode: string) => {
    setExpandedSido(prev => {
      const next = new Set(prev)
      if (next.has(sidoCode)) next.delete(sidoCode)
      else next.add(sidoCode)
      return next
    })
  }

  return (
    <div className="min-h-full pb-20 sm:pb-0" style={{ background: 'var(--bg-primary)' }}>
      <div className="max-w-5xl mx-auto px-4 py-6 sm:py-8">

        {/* 헤더 */}
        <div className="mb-6">
          <h1 className="font-display font-black text-xl sm:text-2xl mb-1" style={{ color: 'var(--text-primary)' }}>
            범죄 통계
          </h1>
          <p className="text-sm" style={{ color: 'var(--text-muted)' }}>
            전국 시/도·시/군/구별 범죄 발생 현황 (2024년 기준)
          </p>
        </div>

        {/* 요약 카드 */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-6">
          {sidoGroups.slice(0, 4).map((g, i) => (
            <div
              key={g.sidoCode}
              className="rounded-2xl p-4 cursor-pointer transition-all"
              style={{ background: 'var(--bg-card)', border: '1px solid var(--border)' }}
              onClick={() => { setSelectedDistrict(g.sidoCode); setChartType('radar') }}
            >
              <div
                className="w-2 h-2 rounded-full mb-2"
                style={{ background: SIDO_COLORS[i] }}
              />
              <p className="text-xs font-medium mb-1" style={{ color: 'var(--text-secondary)' }}>
                {g.sidoName}
              </p>
              <p className="text-xl font-bold font-mono" style={{ color: 'var(--text-primary)' }}>
                {g.totalCount.toLocaleString()}
              </p>
              <p className="text-xs" style={{ color: 'var(--text-muted)' }}>건</p>
            </div>
          ))}
        </div>

        {/* 차트 타입 선택 */}
        <div className="flex gap-2 mb-6">
          {(['bar', 'radar'] as const).map((type) => {
            const Icon = type === 'bar' ? BarChart3 : RadarIcon
            return (
              <button
                key={type}
                onClick={() => setChartType(type)}
                className="px-4 py-1.5 rounded-full text-sm transition-all flex items-center gap-1.5"
                style={{
                  background: chartType === type ? 'var(--accent-blue)' : 'var(--bg-card)',
                  color:      chartType === type ? '#fff' : 'var(--text-secondary)',
                  border:     `1px solid ${chartType === type ? 'var(--accent-blue)' : 'var(--border)'}`,
                }}
              >
                <Icon size={15} strokeWidth={2} />
                {type === 'bar' ? '지역별 비교' : '유형별 분석'}
              </button>
            )
          })}
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-6">

          {/* 바 차트 — 시/도별 범죄 건수 */}
          <div
            className="rounded-2xl p-5"
            style={{ background: 'var(--bg-card)', border: '1px solid var(--border)' }}
          >
            <h2 className="font-display font-bold text-sm mb-4" style={{ color: 'var(--text-primary)' }}>
              시/도별 범죄 건수
            </h2>
            {groupedLoading ? (
              <div className="h-64 rounded-xl animate-pulse" style={{ background: 'var(--bg-hover)' }} />
            ) : (
              <ResponsiveContainer width="100%" height={260}>
                <BarChart data={barData} margin={{ top: 0, right: 0, left: -20, bottom: 40 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                  <XAxis
                    dataKey="name"
                    tick={{ fill: 'var(--text-muted)', fontSize: 10 }}
                    angle={-35}
                    textAnchor="end"
                    interval={0}
                  />
                  <YAxis tick={{ fill: 'var(--text-muted)', fontSize: 10 }} />
                  <Tooltip
                    contentStyle={{
                      background: 'var(--bg-card)',
                      border: '1px solid var(--border)',
                      borderRadius: 8,
                      color: 'var(--text-primary)',
                      fontSize: 12,
                    }}
                  />
                  {chartType === 'bar' ? (
                    crimeTypes.map((type) => (
                      <Bar key={type} dataKey={type} stackId="a"
                        fill={CRIME_COLORS[type] ?? CRIME_COLOR_FALLBACK} radius={[0, 0, 0, 0]} />
                    ))
                  ) : (
                    <Bar dataKey="total" radius={[4, 4, 0, 0]}>
                      {barData.map((_, i) => (
                        <Cell key={i} fill={SIDO_COLORS[i % SIDO_COLORS.length]} />
                      ))}
                    </Bar>
                  )}
                </BarChart>
              </ResponsiveContainer>
            )}
          </div>

          {/* 레이더 차트 — 유형별 분석 */}
          <div
            className="rounded-2xl p-5"
            style={{ background: 'var(--bg-card)', border: '1px solid var(--border)' }}
          >
            <div className="flex items-center justify-between mb-4">
              <h2 className="font-display font-bold text-sm" style={{ color: 'var(--text-primary)' }}>
                유형별 범죄 분석
              </h2>
              {/* 지역 선택 (시/도 + 시/군/구 전체) */}
              <select
                value={selectedDistrict}
                onChange={(e) => setSelectedDistrict(e.target.value)}
                className="text-xs rounded-lg px-2 py-1 outline-none"
                style={{
                  background: 'var(--bg-hover)',
                  border: '1px solid var(--border)',
                  color: 'var(--text-secondary)',
                }}
              >
                {flatDistricts.map((d) => (
                  <option key={d.districtCode} value={d.districtCode}>
                    {d.districtName}
                  </option>
                ))}
              </select>
            </div>

            {!selected ? (
              <div className="h-64 rounded-xl animate-pulse" style={{ background: 'var(--bg-hover)' }} />
            ) : radarData.length > 0 ? (
              <ResponsiveContainer width="100%" height={260}>
                <RadarChart data={radarData}>
                  <PolarGrid stroke="var(--border)" />
                  <PolarAngleAxis
                    dataKey="type"
                    tick={{ fill: 'var(--text-muted)', fontSize: 11 }}
                  />
                  <Radar
                    name={selected?.districtName}
                    dataKey="count"
                    stroke="#0b6e82"
                    fill="#0b6e82"
                    fillOpacity={0.2}
                  />
                  <Tooltip
                    contentStyle={{
                      background: 'var(--bg-card)',
                      border: '1px solid var(--border)',
                      borderRadius: 8,
                      color: 'var(--text-primary)',
                      fontSize: 12,
                    }}
                  />
                </RadarChart>
              </ResponsiveContainer>
            ) : (
              <p className="text-xs text-center py-20" style={{ color: 'var(--text-muted)' }}>
                지역을 선택해주세요
              </p>
            )}
          </div>
        </div>

        {/* 상세 테이블 — 시/도 클릭 시 시/군/구 펼치기 */}
        <div
          className="rounded-2xl p-5 mb-6"
          style={{ background: 'var(--bg-card)', border: '1px solid var(--border)' }}
        >
          <h2 className="font-display font-bold text-sm mb-4" style={{ color: 'var(--text-primary)' }}>
            지역별 범죄 유형 상세
          </h2>
          <div className="overflow-x-auto">
            <table className="w-full text-xs">
              <thead>
                <tr style={{ borderBottom: '1px solid var(--border)' }}>
                  <th className="text-left py-2 pr-4" style={{ color: 'var(--text-muted)' }}>지역</th>
                  {crimeTypes.map((type) => (
                    <th key={type} className="text-right py-2 px-2" style={{ color: CRIME_COLORS[type] ?? 'var(--text-muted)' }}>
                      {type}
                    </th>
                  ))}
                  <th className="text-right py-2 pl-4" style={{ color: 'var(--text-muted)' }}>합계</th>
                </tr>
              </thead>
              <tbody>
                {sidoGroups.map((g, i) => {
                  const hasChildren = g.districts.length > 0
                  const isOpen = expandedSido.has(g.sidoCode)
                  return (
                    <>
                      <tr
                        key={g.sidoCode}
                        className="transition-colors cursor-pointer"
                        style={{
                          borderBottom: '1px solid var(--border)',
                          background: selectedDistrict === g.sidoCode
                            ? 'color-mix(in srgb, var(--accent-blue) 8%, transparent)' : 'transparent',
                        }}
                        onClick={() => {
                          setSelectedDistrict(g.sidoCode)
                          if (hasChildren) toggleSido(g.sidoCode)
                        }}
                        onMouseEnter={e => (e.currentTarget.style.background = 'var(--bg-hover)')}
                        onMouseLeave={e => (e.currentTarget.style.background =
                          selectedDistrict === g.sidoCode ? 'color-mix(in srgb, var(--accent-blue) 8%, transparent)' : 'transparent'
                        )}
                      >
                        <td className="py-2.5 pr-4">
                          <div className="flex items-center gap-2">
                            {hasChildren ? (
                              isOpen
                                ? <ChevronDown size={13} strokeWidth={2} style={{ color: 'var(--text-muted)' }} className="shrink-0" />
                                : <ChevronRight size={13} strokeWidth={2} style={{ color: 'var(--text-muted)' }} className="shrink-0" />
                            ) : (
                              <div
                                className="w-2 h-2 rounded-full shrink-0"
                                style={{ background: SIDO_COLORS[i % SIDO_COLORS.length] }}
                              />
                            )}
                            <span className="font-semibold" style={{ color: 'var(--text-primary)' }}>
                              {g.sidoName}
                            </span>
                          </div>
                        </td>
                        {crimeTypes.map((type) => (
                          <td key={type} className="text-right py-2.5 px-2 font-mono font-semibold"
                            style={{ color: 'var(--text-secondary)' }}>
                            {(g.crimeByType[type] ?? 0).toLocaleString()}
                          </td>
                        ))}
                        <td className="text-right py-2.5 pl-4 font-bold font-mono"
                          style={{ color: 'var(--accent-red)' }}>
                          {g.totalCount.toLocaleString()}
                        </td>
                      </tr>

                      {hasChildren && isOpen && [...g.districts]
                        .sort((a, b) => b.totalCount - a.totalCount)
                        .map((d) => (
                        <tr
                          key={d.districtCode}
                          className="transition-colors cursor-pointer"
                          style={{
                            borderBottom: '1px solid var(--border)',
                            background: selectedDistrict === d.districtCode
                              ? 'color-mix(in srgb, var(--accent-blue) 8%, transparent)' : 'transparent',
                          }}
                          onClick={() => setSelectedDistrict(d.districtCode)}
                          onMouseEnter={e => (e.currentTarget.style.background = 'var(--bg-hover)')}
                          onMouseLeave={e => (e.currentTarget.style.background =
                            selectedDistrict === d.districtCode ? 'color-mix(in srgb, var(--accent-blue) 8%, transparent)' : 'transparent'
                          )}
                        >
                          <td className="py-2 pr-4 pl-6">
                            <span style={{ color: 'var(--text-secondary)' }}>
                              {d.districtName}
                            </span>
                          </td>
                          {crimeTypes.map((type) => (
                            <td key={type} className="text-right py-2 px-2 font-mono"
                              style={{ color: 'var(--text-muted)' }}>
                              {(d.crimeByType[type] ?? 0).toLocaleString()}
                            </td>
                          ))}
                          <td className="text-right py-2 pl-4 font-semibold font-mono"
                            style={{ color: 'var(--text-secondary)' }}>
                            {d.totalCount.toLocaleString()}
                          </td>
                        </tr>
                      ))}
                    </>
                  )
                })}
              </tbody>
            </table>
          </div>
        </div>

      </div>
      <Footer />
    </div>
  )
}
