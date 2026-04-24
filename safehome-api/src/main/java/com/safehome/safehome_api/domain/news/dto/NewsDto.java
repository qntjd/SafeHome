package com.safehome.safehome_api.domain.news.dto;

import com.safehome.safehome_api.domain.news.entity.NewsArticle;

import java.time.LocalDateTime;
import java.util.UUID;

public class NewsDto {

    public record NewsResponse(
            UUID id,
            String title,
            String description,
            String url,
            String source,
            String keyword,
            LocalDateTime publishedAt
    ) {
        public static NewsResponse from(NewsArticle a) {
            return new NewsResponse(
                    a.getId(),
                    a.getTitle(),
                    a.getDescription(),
                    a.getUrl(),
                    a.getSource(),
                    a.getKeyword(),
                    a.getPublishedAt()
            );
        }
    }

    public record NewsPageResponse(
            java.util.List<NewsResponse> articles,
            int currentPage,
            int totalPages,
            long totalElements
    ) {}
}