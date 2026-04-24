package com.safehome.safehome_api.domain.news.controller;

import com.safehome.safehome_api.domain.news.dto.NewsDto;
import com.safehome.safehome_api.domain.news.service.NewsService;
import com.safehome.safehome_api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "News", description = "안전 뉴스 API")
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    @Operation(summary = "안전 뉴스 목록 조회")
    @GetMapping
    public ApiResponse<NewsDto.NewsPageResponse> getNews(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false)    String keyword
    ) {
        return ApiResponse.success(newsService.getNews(page, size, keyword));
    }
}