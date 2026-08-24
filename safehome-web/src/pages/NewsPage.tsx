import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Search } from 'lucide-react'
import { newsApi } from '@/api/news'
import type { NewsArticle } from '@/api/news'
import Footer from '@/components/Footer'

const KEYWORD_TABS = [
  { label: '전체',    value: '' },
  { label: '범죄',    value: '검거' },
  { label: '재난',    value: '재난' },
  { label: '화재',    value: '화재' },
  { label: '교통사고', value: '교통사고' },
  { label: '안전사고', value: '사고' },
]

const KEYWORD_STYLE: Record<string, { bg: string; color: string }> = {
  '대구 범죄':     { bg: 'var(--accent-red-soft)',   color: 'var(--accent-red)' },
  '대구 재난':     { bg: 'var(--grade-d-bg)',        color: 'var(--grade-d)' },
  '대구 안전사고':  { bg: 'var(--accent-amber-soft)', color: 'var(--accent-amber-deep)' },
  '대구 화재':     { bg: 'var(--accent-red-soft)',   color: 'var(--accent-red)' },
  '대구 교통사고':  { bg: 'var(--grade-b-bg)',        color: 'var(--accent-blue)' },
}
const KEYWORD_STYLE_FALLBACK = { bg: 'var(--bg-hover)', color: 'var(--text-secondary)' }

function NewsCard({ article }: { article: NewsArticle }) {
  const kw = KEYWORD_STYLE[article.keyword] ?? KEYWORD_STYLE_FALLBACK
  return (
    <a
      href={article.url}
      target="_blank"
      rel="noopener noreferrer"
      className="block rounded-2xl p-4 transition-all active:scale-[0.99]"
      style={{ background: 'var(--bg-card)', border: '1px solid var(--border)' }}
      onMouseEnter={e => (e.currentTarget.style.borderColor = 'var(--border-hover)')}
      onMouseLeave={e => (e.currentTarget.style.borderColor = 'var(--border)')}
    >
      <div className="flex items-start justify-between gap-3 mb-2">
        <h3 className="text-sm font-medium leading-snug line-clamp-2 flex-1" style={{ color: 'var(--text-primary)' }}>
          {article.title}
        </h3>
        <span
          className="text-xs px-2 py-0.5 rounded-full shrink-0 font-medium"
          style={{ background: kw.bg, color: kw.color }}
        >
          {article.keyword.replace('대구 ', '')}
        </span>
      </div>
      {article.description && (
        <p className="text-xs line-clamp-2 mb-3 leading-relaxed" style={{ color: 'var(--text-secondary)' }}>
          {article.description}
        </p>
      )}
      <div className="flex items-center justify-between text-xs" style={{ color: 'var(--text-muted)' }}>
        <span className="font-medium">{article.source}</span>
        <span className="font-mono">{new Date(article.publishedAt).toLocaleDateString('ko-KR', {
          year: 'numeric', month: 'long', day: 'numeric'
        })}</span>
      </div>
    </a>
  )
}

function SkeletonCard() {
  return (
    <div className="rounded-2xl p-4 animate-pulse" style={{ background: 'var(--bg-card)', border: '1px solid var(--border)' }}>
      <div className="flex gap-3 mb-3">
        <div className="flex-1 h-4 rounded" style={{ background: 'var(--bg-hover)' }} />
        <div className="w-12 h-4 rounded-full" style={{ background: 'var(--bg-hover)' }} />
      </div>
      <div className="h-3 rounded mb-1.5" style={{ background: 'var(--bg-hover)' }} />
      <div className="h-3 rounded w-3/4 mb-4" style={{ background: 'var(--bg-hover)' }} />
      <div className="flex justify-between">
        <div className="h-3 w-16 rounded" style={{ background: 'var(--bg-hover)' }} />
        <div className="h-3 w-20 rounded" style={{ background: 'var(--bg-hover)' }} />
      </div>
    </div>
  )
}

export default function NewsPage() {
  const [activeTab, setActiveTab]       = useState('')
  const [searchInput, setSearchInput]   = useState('')
  const [searchKeyword, setSearchKeyword] = useState('')
  const [page, setPage]                 = useState(0)

  const { data, isLoading } = useQuery({
    queryKey: ['news', page, activeTab || searchKeyword],
    queryFn: () => newsApi.getNews(page, 10, activeTab || searchKeyword || undefined),
  })

  const result   = data?.data?.data
  const articles = result?.articles ?? []

  const handleSearch = () => {
    setActiveTab('')
    setSearchKeyword(searchInput)
    setPage(0)
  }

  const handleTabChange = (value: string) => {
    setActiveTab(value)
    setSearchKeyword('')
    setSearchInput('')
    setPage(0)
  }

  return (
    <div className="min-h-full" style={{ background: 'var(--bg-primary)' }}>
      <div className="max-w-3xl mx-auto px-4 py-6 sm:py-8">

        {/* 헤더 */}
        <div className="mb-6">
          <h1 className="font-display font-black text-xl sm:text-2xl mb-1" style={{ color: 'var(--text-primary)' }}>
            안전 뉴스
          </h1>
          <p className="text-sm" style={{ color: 'var(--text-muted)' }}>전국 지역 안전·재난·범죄 관련 최신 뉴스</p>
        </div>

        {/* 검색창 */}
        <div className="flex gap-2 mb-4">
          <input
            type="text"
            placeholder="뉴스 검색..."
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
            className="flex-1 rounded-xl px-4 py-2.5 text-sm outline-none"
            style={{ background: 'var(--bg-card)', border: '1px solid var(--border)', color: 'var(--text-primary)' }}
            onFocus={e => (e.currentTarget.style.borderColor = 'var(--accent-blue)')}
            onBlur={e => (e.currentTarget.style.borderColor = 'var(--border)')}
          />
          <button
            onClick={handleSearch}
            className="px-4 py-2.5 rounded-xl text-sm font-semibold transition-all shrink-0"
            style={{ background: 'var(--accent-blue)', color: '#fff' }}
            onMouseEnter={e => (e.currentTarget.style.background = 'var(--accent-blue-hover)')}
            onMouseLeave={e => (e.currentTarget.style.background = 'var(--accent-blue)')}
          >
            검색
          </button>
        </div>

        {/* 카테고리 탭 — 모바일에서 가로 스크롤 */}
        <div className="flex gap-2 mb-6 overflow-x-auto pb-1 scrollbar-none">
          {KEYWORD_TABS.map((tab) => (
            <button
              key={tab.value}
              onClick={() => handleTabChange(tab.value)}
              className="text-sm px-4 py-1.5 rounded-full whitespace-nowrap transition-all shrink-0"
              style={{
                background: activeTab === tab.value ? 'var(--accent-blue)' : 'var(--bg-card)',
                color:      activeTab === tab.value ? '#fff' : 'var(--text-secondary)',
                border:     `1px solid ${activeTab === tab.value ? 'var(--accent-blue)' : 'var(--border)'}`,
              }}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* 결과 수 */}
        {!isLoading && result && (
          <p className="text-xs mb-3 flex items-center gap-1.5" style={{ color: 'var(--text-muted)' }}>
            <span className="beacon-dot beacon-dot--static" />
            <span className="font-mono">총 {result.totalElements.toLocaleString()}건</span>
          </p>
        )}

        {/* 뉴스 목록 */}
        {isLoading ? (
          <div className="flex flex-col gap-3">
            {Array.from({ length: 5 }).map((_, i) => <SkeletonCard key={i} />)}
          </div>
        ) : articles.length === 0 ? (
          <div className="text-center py-20" style={{ color: 'var(--text-muted)' }}>
            <Search size={32} strokeWidth={1.75} className="mx-auto mb-3" />
            <p className="text-sm">검색 결과가 없습니다.</p>
          </div>
        ) : (
          <div className="flex flex-col gap-3">
            {articles.map((article) => (
              <NewsCard key={article.id} article={article} />
            ))}
          </div>
        )}

        {/* 페이지네이션 */}
        {result && result.totalPages > 1 && (
          <div className="flex items-center justify-center gap-3 mt-8 pb-6">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              className="px-4 py-2 text-sm rounded-xl disabled:opacity-40 transition-all"
              style={{ background: 'var(--bg-card)', border: '1px solid var(--border)', color: 'var(--text-secondary)' }}
            >
              이전
            </button>
            <div className="flex gap-1">
              {Array.from({ length: Math.min(result.totalPages, 5) }).map((_, i) => {
                const pageNum = Math.max(0, Math.min(page - 2, result.totalPages - 5)) + i
                return (
                  <button
                    key={pageNum}
                    onClick={() => setPage(pageNum)}
                    className="w-9 h-9 text-sm rounded-xl transition-all font-mono"
                    style={{
                      background: page === pageNum ? 'var(--accent-blue)' : 'var(--bg-card)',
                      color:      page === pageNum ? '#fff' : 'var(--text-secondary)',
                      border:     `1px solid ${page === pageNum ? 'var(--accent-blue)' : 'var(--border)'}`,
                    }}
                  >
                    {pageNum + 1}
                  </button>
                )
              })}
            </div>
            <button
              onClick={() => setPage((p) => Math.min(result.totalPages - 1, p + 1))}
              disabled={page >= result.totalPages - 1}
              className="px-4 py-2 text-sm rounded-xl disabled:opacity-40 transition-all"
              style={{ background: 'var(--bg-card)', border: '1px solid var(--border)', color: 'var(--text-secondary)' }}
            >
              다음
            </button>
          </div>
        )}
      </div>
        <Footer />
    </div>
  )
}
