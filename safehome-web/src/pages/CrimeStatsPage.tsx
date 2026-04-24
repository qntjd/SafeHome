import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  RadarChart, Radar, PolarGrid, PolarAngleAxis,
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip,
  ResponsiveContainer, Cell, Legend
} from 'recharts'
import { crimeApi } from '@/api/crime'
import type { DistrictCrimeResponse } from '@/api/crime'
import Footer from '@/components/Footer'

const CRIME_COLORS: Record<string, string> = {
  '강력범죄': '#f87171',
  '폭행':     '#fb923c',
  '절도':     '#fbbf24',
  '성범죄':   '#c084fc',
  '기타':     '#6b7280',
}

const DISTRICT_COLORS = [
  '#4f7ef8', '#34d399', '#f87171', '#fbbf24',
  '#a78bfa', '#fb923c', '#60a5fa', '#f472b6'
]

// 레이더 차트용 데이터 변환
function toRadarData(district: DistrictCrimeResponse) {
  return Object.entries(district.crimeByType).map(([type, count]) => ({
    type,
    count,
    fullMark: Math.max(...Object.values(district.crimeByType)),
  }))
}

// 바 차트용 데이터 변환
function toBarData(districts: DistrictCrimeResponse[]) {
  return districts.map((d) => ({
    name: d.districtName.replace('대구 ', ''),
    ...d.crimeByType,
    total: d.totalCount,
  }))
}

export default function CrimeStatsPage() {
  const [selectedDistrict, setSelectedDistrict] = useState<string>('')
  const [chartType, setChartType] = useState<'bar' | 'radar'>('bar')

  const { data, isLoading } = useQuery({
    queryKey: ['crimes'],
    queryFn: () => crimeApi.getAllCrimes(),
  })

  const districts = data?.data?.data?.districts ?? []
  const selected  = districts.find(d => d.districtCode === selectedDistrict) ?? districts[0]
  const barData   = toBarData(districts)
  const radarData = selected ? toRadarData(selected) : []
  const crimeTypes = selected ? Object.keys(selected.crimeByType) : []

  return (
    <div className="min-h-full pb-20 sm:pb-0" style={{ background: 'var(--bg-primary)' }}>
      <div className="max-w-5xl mx-auto px-4 py-6 sm:py-8">

        {/* 헤더 */}
        <div className="mb-6">
          <h1 className="text-xl sm:text-2xl font-semibold mb-1" style={{ color: 'var(--text-primary)' }}>
            범죄 통계
          </h1>
          <p className="text-sm" style={{ color: 'var(--text-muted)' }}>
            대구 지역 구·군별 범죄 발생 현황 (2024년 기준)
          </p>
        </div>

        {/* 요약 카드 */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-6">
          {districts.slice(0, 4).map((d, i) => (
            <div
              key={d.districtCode}
              className="rounded-xl p-4 cursor-pointer transition-all"
              style={{
                background: selectedDistrict === d.districtCode
                  ? 'rgba(79,126,248,0.15)' : 'var(--bg-card)',
                border: `1px solid ${selectedDistrict === d.districtCode
                  ? 'var(--accent-blue)' : 'var(--border)'}`,
              }}
              onClick={() => setSelectedDistrict(d.districtCode)}
            >
              <div
                className="w-2 h-2 rounded-full mb-2"
                style={{ background: DISTRICT_COLORS[i] }}
              />
              <p className="text-xs font-medium mb-1" style={{ color: 'var(--text-secondary)' }}>
                {d.districtName.replace('대구 ', '')}
              </p>
              <p className="text-xl font-bold" style={{ color: 'var(--text-primary)' }}>
                {d.totalCount.toLocaleString()}
              </p>
              <p className="text-xs" style={{ color: 'var(--text-muted)' }}>건</p>
            </div>
          ))}
        </div>

        {/* 차트 타입 선택 */}
        <div className="flex gap-2 mb-6">
          {(['bar', 'radar'] as const).map((type) => (
            <button
              key={type}
              onClick={() => setChartType(type)}
              className="px-4 py-1.5 rounded-full text-sm transition-all"
              style={{
                background: chartType === type ? 'var(--accent-blue)' : 'var(--bg-card)',
                color:      chartType === type ? '#fff' : 'var(--text-secondary)',
                border:     `1px solid ${chartType === type ? 'var(--accent-blue)' : 'var(--border)'}`,
              }}
            >
              {type === 'bar' ? '📊 지역별 비교' : '🕸 유형별 분석'}
            </button>
          ))}
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-6">

          {/* 바 차트 — 지역별 범죄 건수 */}
          <div
            className="rounded-2xl p-5"
            style={{ background: 'var(--bg-card)', border: '1px solid var(--border)' }}
          >
            <h2 className="text-sm font-semibold mb-4" style={{ color: 'var(--text-primary)' }}>
              지역별 범죄 건수
            </h2>
            {isLoading ? (
              <div className="h-64 rounded-xl animate-pulse" style={{ background: 'var(--bg-hover)' }} />
            ) : (
              <ResponsiveContainer width="100%" height={260}>
                <BarChart data={barData} margin={{ top: 0, right: 0, left: -20, bottom: 40 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
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
                        fill={CRIME_COLORS[type] ?? '#888'} radius={[0, 0, 0, 0]} />
                    ))
                  ) : (
                    <Bar dataKey="total" radius={[4, 4, 0, 0]}>
                      {barData.map((_, i) => (
                        <Cell key={i} fill={DISTRICT_COLORS[i % DISTRICT_COLORS.length]} />
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
              <h2 className="text-sm font-semibold" style={{ color: 'var(--text-primary)' }}>
                유형별 범죄 분석
              </h2>
              {/* 지역 선택 */}
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
                {districts.map((d) => (
                  <option key={d.districtCode} value={d.districtCode}>
                    {d.districtName.replace('대구 ', '')}
                  </option>
                ))}
              </select>
            </div>

            {isLoading ? (
              <div className="h-64 rounded-xl animate-pulse" style={{ background: 'var(--bg-hover)' }} />
            ) : radarData.length > 0 ? (
              <ResponsiveContainer width="100%" height={260}>
                <RadarChart data={radarData}>
                  <PolarGrid stroke="rgba(255,255,255,0.08)" />
                  <PolarAngleAxis
                    dataKey="type"
                    tick={{ fill: 'var(--text-muted)', fontSize: 11 }}
                  />
                  <Radar
                    name={selected?.districtName}
                    dataKey="count"
                    stroke="var(--accent-blue)"
                    fill="var(--accent-blue)"
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

        {/* 상세 테이블 */}
        <div
          className="rounded-2xl p-5 mb-6"
          style={{ background: 'var(--bg-card)', border: '1px solid var(--border)' }}
        >
          <h2 className="text-sm font-semibold mb-4" style={{ color: 'var(--text-primary)' }}>
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
                {districts
                  .sort((a, b) => b.totalCount - a.totalCount)
                  .map((d, i) => (
                  <tr
                    key={d.districtCode}
                    className="transition-colors cursor-pointer"
                    style={{
                      borderBottom: '1px solid var(--border)',
                      background: selectedDistrict === d.districtCode
                        ? 'rgba(79,126,248,0.08)' : 'transparent',
                    }}
                    onClick={() => setSelectedDistrict(d.districtCode)}
                    onMouseEnter={e => (e.currentTarget.style.background = 'var(--bg-hover)')}
                    onMouseLeave={e => (e.currentTarget.style.background =
                      selectedDistrict === d.districtCode ? 'rgba(79,126,248,0.08)' : 'transparent'
                    )}
                  >
                    <td className="py-2.5 pr-4">
                      <div className="flex items-center gap-2">
                        <div
                          className="w-2 h-2 rounded-full shrink-0"
                          style={{ background: DISTRICT_COLORS[i % DISTRICT_COLORS.length] }}
                        />
                        <span style={{ color: 'var(--text-primary)' }}>
                          {d.districtName.replace('대구 ', '')}
                        </span>
                      </div>
                    </td>
                    {crimeTypes.map((type) => (
                      <td key={type} className="text-right py-2.5 px-2"
                        style={{ color: 'var(--text-secondary)' }}>
                        {(d.crimeByType[type] ?? 0).toLocaleString()}
                      </td>
                    ))}
                    <td className="text-right py-2.5 pl-4 font-semibold"
                      style={{ color: 'var(--accent-red)' }}>
                      {d.totalCount.toLocaleString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

      </div>
      <Footer />
    </div>
  )
}