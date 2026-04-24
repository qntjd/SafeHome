import api from './axios'

export interface NewsArticle {
  id: string
  title: string
  description: string
  url: string
  source: string
  keyword: string
  publishedAt: string
}

export interface NewsPageResponse {
  articles: NewsArticle[]
  currentPage: number
  totalPages: number
  totalElements: number
}

export const newsApi = {
  getNews: (page = 0, size = 10, keyword?: string) =>
    api.get<{ data: NewsPageResponse }>('/news', {
      params: { page, size, keyword },
    }),
}