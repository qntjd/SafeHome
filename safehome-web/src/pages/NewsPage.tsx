import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { newsApi } from '@/api/news'
import type { NewsArticle } from '@/api/news'
import Footer from '@/components/Footer'

const KEYWORD_TABS = [
  { label: '전체',    value: '' },
  { label: '범죄',    value: '범죄' },
  { label: '재난',    value: '재난' },
  { label: '화재',    value: '화재' },
  { label: '교통사고', value: '교통사고' },
]

const KEYWORD_COLORS: Record<string, string> = {
  '대구 범죄':     'bg-red-50 text-red-700 border-red-100',
  '대구 재난':     'bg-orange-50 text-orange-700 border-orange-100',
  '대구 안전사고':  'bg-yellow-50 text-yellow-700 border-yellow-100',
  '대구 화재':     'bg-red-50 text-red-600 border-red-100',
  '대구 교통사고':  'bg-blue-50 text-blue-700 border-blue-100',
}

function NewsCard({ article }: { article: NewsArticle }) {
  return (
    <a
      href={article.url}
      target="_blank"
      rel="noopener noreferrer"
      className="block bg-white border border-gray-200 rounded-xl p-4 hover:border-blue-300 hover:shadow-sm active:scale-[0.99] transition-all"
    >
      <div className="flex items-start justify-between gap-3 mb-2">
        <h3 className="text-sm font-medium text-gray-900 leading-snug line-clamp-2 flex-1">
          {article.title}
        </h3>
        <span className={`text-xs px-2 py-0.5 rounded-full shrink-0 border ${
          KEYWORD_COLORS[article.keyword] ?? 'bg-gray-100 text-gray-600 border-gray-100'
        }`}>
          {article.keyword.replace('대구 ', '')}
        </span>
      </div>
      {article.description && (
        <p className="text-xs text-gray-500 line-clamp-2 mb-3 leading-relaxed">
          {article.description}
        </p>
      )}
      <div className="flex items-center justify-between text-xs text-gray-400">
        <span className="font-medium">{article.source}</span>
        <span>{new Date(article.publishedAt).toLocaleDateString('ko-KR', {
          year: 'numeric', month: 'long', day: 'numeric'
        })}</span>
      </div>
    </a>
  )
}

function SkeletonCard() {
  return (
    <div className="bg-white border border-gray-100 rounded-xl p-4 animate-pulse">
      <div className="flex gap-3 mb-3">
        <div className="flex-1 h-4 bg-gray-100 rounded" />
        <div className="w-12 h-4 bg-gray-100 rounded-full" />
      </div>
      <div className="h-3 bg-gray-100 rounded mb-1.5" />
      <div className="h-3 bg-gray-100 rounded w-3/4 mb-4" />
      <div className="flex justify-between">
        <div className="h-3 w-16 bg-gray-100 rounded" />
        <div className="h-3 w-20 bg-gray-100 rounded" />
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
    <div className="min-h-full bg-gray-50">
      <div className="max-w-3xl mx-auto px-4 py-6 sm:py-8">

        {/* 헤더 */}
        <div className="mb-6">
          <h1 className="text-xl sm:text-2xl font-semibold text-gray-900 mb-1">안전 뉴스</h1>
          <p className="text-sm text-gray-400">대구 지역 안전·재난·범죄 관련 최신 뉴스</p>
        </div>

        {/* 검색창 */}
        <div className="flex gap-2 mb-4">
          <input
            type="text"
            placeholder="뉴스 검색..."
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
            className="flex-1 border border-gray-300 rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white"
          />
          <button
            onClick={handleSearch}
            className="bg-blue-600 text-white px-4 py-2.5 rounded-xl text-sm font-medium hover:bg-blue-700 transition-colors shrink-0"
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
              className={`text-sm px-4 py-1.5 rounded-full border whitespace-nowrap transition-colors shrink-0 ${
                activeTab === tab.value
                  ? 'bg-blue-600 text-white border-blue-600'
                  : 'bg-white text-gray-600 border-gray-200 hover:border-blue-300'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* 결과 수 */}
        {!isLoading && result && (
          <p className="text-xs text-gray-400 mb-3">
            총 {result.totalElements.toLocaleString()}건
          </p>
        )}

        {/* 뉴스 목록 */}
        {isLoading ? (
          <div className="flex flex-col gap-3">
            {Array.from({ length: 5 }).map((_, i) => <SkeletonCard key={i} />)}
          </div>
        ) : articles.length === 0 ? (
          <div className="text-center py-20 text-gray-400">
            <p className="text-3xl mb-3">🔍</p>
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
              className="px-4 py-2 text-sm border border-gray-200 rounded-xl bg-white disabled:opacity-40 hover:bg-gray-50 transition-colors"
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
                    className={`w-9 h-9 text-sm rounded-xl transition-colors ${
                      page === pageNum
                        ? 'bg-blue-600 text-white'
                        : 'bg-white border border-gray-200 text-gray-600 hover:bg-gray-50'
                    }`}
                  >
                    {pageNum + 1}
                  </button>
                )
              })}
            </div>
            <button
              onClick={() => setPage((p) => Math.min(result.totalPages - 1, p + 1))}
              disabled={page >= result.totalPages - 1}
              className="px-4 py-2 text-sm border border-gray-200 rounded-xl bg-white disabled:opacity-40 hover:bg-gray-50 transition-colors"
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