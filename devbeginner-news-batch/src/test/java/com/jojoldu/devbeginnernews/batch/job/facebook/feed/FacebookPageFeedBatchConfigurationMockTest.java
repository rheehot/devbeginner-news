package com.jojoldu.devbeginnernews.batch.job.facebook.feed;

import com.jojoldu.devbeginnernews.TestJobConfiguration;
import com.jojoldu.devbeginnernews.batch.job.facebook.feed.dto.FacebookPagingDto;
import com.jojoldu.devbeginnernews.batch.job.facebook.feed.dto.FacebookFeedCollection;
import com.jojoldu.devbeginnernews.batch.job.facebook.feed.dto.FacebookFeedDto;
import com.jojoldu.devbeginnernews.batch.job.facebook.feed.dto.FacebookFromDto;
import com.jojoldu.devbeginnernews.core.article.Article;
import com.jojoldu.devbeginnernews.core.article.ArticleRepository;
import com.jojoldu.devbeginnernews.core.article.facebook.ArticleFacebook;
import com.jojoldu.devbeginnernews.core.article.facebook.ArticleFacebookRepository;
import com.jojoldu.devbeginnernews.core.token.FacebookPageToken;
import com.jojoldu.devbeginnernews.core.token.FacebookPageTokenRepository;
import com.jojoldu.devbeginnernews.facebook.service.FacebookPageTokenRefresher;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@RunWith(SpringRunner.class)
@SpringBatchTest
@SpringBootTest(classes = {FacebookPageFeedBatchConfiguration.class, TestJobConfiguration.class})
public class FacebookPageFeedBatchConfigurationMockTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private ArticleFacebookRepository articleFacebookRepository;

    @Autowired
    private FacebookPageTokenRepository facebookOauthTokenRepository;

    @MockBean
    private FacebookFeedRestTemplate facebookRestTemplate;

    @MockBean
    private FacebookPageTokenRefresher facebookPageTokenRefresher;

    @After
    public void tearDown() throws Exception {
        articleRepository.deleteAll();
        facebookOauthTokenRepository.deleteAll();
    }

    @Test
    public void FacebookAPI_응답결과를_저장한다() throws Exception {
        //given
        String pageId = "1";
        String pageToken = "1";
        facebookOauthTokenRepository.save(new FacebookPageToken(pageId, pageToken));

        FacebookFeedDto feedDto = FacebookFeedDto.builder()
                .id("1")
                .message("팀 게임 즉, 회사에서 팀 단위로 일을 하다보면 \\\"에이스가 아닌 역할\\\"에 집중할때가 있습니다.\\n\\n팀을 위해 주요한 역할을 양보한다고 볼수도 있겠습니다만,\\n개인으로 봤을때 그게 도움이 될까요?\\n\\n좋은 팀에 있다고 해서 실력있는 개발자가 아닐 수 있습니다.\\n\\n이번에 그 이야기를 한번 적어보았습니다.\\n\\n출근길에 가볍게 한번 봐주세요 :)\\n\\nhttps://jojoldu.tistory.com/419")
                .createdTime("2019-07-26T00:27:08+0000")
                .from(FacebookFromDto.builder().name("a").id(pageId).build())
                .build();
        FacebookFeedCollection apiResponse = new FacebookFeedCollection(asList(feedDto), new FacebookPagingDto());

        given(facebookPageTokenRefresher.refresh(anyString()))
                .willReturn(pageToken);

        given(facebookRestTemplate.feed(anyString()))
                .willReturn(apiResponse);

        //when
        JobParameters jobParameters = new JobParametersBuilder(jobLauncherTestUtils.getUniqueJobParameters())
                .addString("pageId", pageId)
                .addDate("version", new Date())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        //then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        List<Article> articles = articleRepository.findAll();
        assertThat(articles.size()).isEqualTo(1);
        Article actual = articles.get(0);
        assertThat(actual.getRegistrationDateTime()).isEqualTo(LocalDateTime.of(2019,7,26,9,27,8));
        assertThat(actual.getRegistrationDate()).isEqualTo(LocalDate.of(2019,7,26));

        List<ArticleFacebook> articleFacebooks = articleFacebookRepository.findAll();
        assertThat(articleFacebooks.size()).isEqualTo(1);

    }
}

