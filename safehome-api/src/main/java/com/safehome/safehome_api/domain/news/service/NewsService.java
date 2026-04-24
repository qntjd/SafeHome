package com.safehome.safehome_api.domain.news.service;

import com.safehome.safehome_api.domain.news.dto.NewsDto;
import com.safehome.safehome_api.domain.news.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NewsService {

    private final NewsRepository newsRepository;

    @Transactional(readOnly = true)
    public NewsDto.NewsPageResponse getNews(int page, int size, String keyword) {
        var pageable = PageRequest.of(page, size);

        var result = (keyword != null && !keyword.isBlank())
                ? newsRepository.findByTitleContainingOrDescriptionContainingOrderByPublishedAtDesc(
                        keyword, keyword, pageable)
                : newsRepository.findAllByOrderByPublishedAtDesc(pageable);

        var articles = result.getContent().stream()
                .map(NewsDto.NewsResponse::from)
                .toList();

        return new NewsDto.NewsPageResponse(
                articles,
                result.getNumber(),
                result.getTotalPages(),
                result.getTotalElements()
        );
    }
}