package com.safehome.safehome_api.domain.news.repository;

import com.safehome.safehome_api.domain.news.entity.NewsArticle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NewsRepository extends JpaRepository<NewsArticle, UUID> {

    Page<NewsArticle> findAllByOrderByPublishedAtDesc(Pageable pageable);

    Page<NewsArticle> findByKeywordContainingOrderByPublishedAtDesc(
            String keyword, Pageable pageable);

    Page<NewsArticle> findByTitleContainingOrDescriptionContainingOrderByPublishedAtDesc(
            String title, String description, Pageable pageable);
}