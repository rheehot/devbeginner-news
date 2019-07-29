package com.jojoldu.devbeginnernews.web.service;

import com.jojoldu.devbeginnernews.core.article.Article;
import com.jojoldu.devbeginnernews.core.article.count.ArticleCount;
import com.jojoldu.devbeginnernews.core.article.count.ArticleCountRepository;
import com.jojoldu.devbeginnernews.web.repository.ArticleWebRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@RequiredArgsConstructor
@Service
public class ArticleService {
    private final ArticleWebRepository articleWebRepository;
    private final ArticleCountRepository articleCountRepository;

    @Transactional(readOnly = true)
    public List<ArticleViewItem> findAllLimitDesc (long offset, long limit) {
        List<Article> articles = articleWebRepository.findAllLimitDesc(offset, limit);
        List<ArticleCount> articleCounts = findArticleCounts(articles);

        return new ArticleViewItems(articles, articleCounts).getArticleItems();
    }

    private List<ArticleCount> findArticleCounts(List<Article> articles) {
        List<Long> articleIds = articles.stream().map(Article::getId).collect(Collectors.toList());
        return articleCountRepository.findAllByArticleIdIn(articleIds);
    }

    @Transactional(readOnly = true)
    public ArticleViewItem findOne(Long articleId) {
        Article article = articleWebRepository.findById(articleId).orElseThrow(() -> new IllegalStateException("해당 게시글은 존재하지 않습니다. articleId=" + articleId));
        Long views = articleCountRepository.findTopByArticleId(articleId)
                .map(ArticleCount::getCount)
                .orElse(0L);

        return new ArticleViewItem(article, views);
    }

}