package com.honjeong.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.honjeong.support.AbstractPostgresTest;

/**
 * Slice 7 — P1 핵심 루프 E2E 스모크 테스트.
 *
 * <p>각 슬라이스(인증·장소·체크인·같이먹기)는 단위·슬라이스 테스트로 검증됐지만, 이 테스트는 그것들이
 * <b>하나로 엮였을 때</b> 실제 요청 한 바퀴가 끝까지 도는지를 확인한다. {@code @SpringBootTest}로 전체 컨텍스트를
 * 올리고 {@code @AutoConfigureMockMvc}로 받은 MockMvc는 운영과 동일한 <b>Spring Security 필터 체인 → 컨트롤러 →
 * 서비스 → 리포지토리 → Testcontainers PostgreSQL</b>을 그대로 통과한다(JWT 인증·인가·Flyway 스키마 포함).
 * 즉 Mock으로 가린 것은 외부 연동(SMS·카카오)뿐이고, 인증 토큰·트랜잭션·DB 제약은 전부 진짜다.
 *
 * <p>검증 시나리오(설계 계획의 happy path):
 * <ol>
 *   <li>두 회원이 휴대폰 온보딩을 끝까지 마치고 정식 토큰을 받는다(신청자 Alice·수신자 Bob).</li>
 *   <li>Bob이 식당을 검색해 그 결과로 혼밥 체크인한다(검색→캐싱 upsert→체크인).</li>
 *   <li>Alice가 같은 식당 혼밥러 목록에서 Bob을 발견하고 같이먹기를 신청한다.</li>
 *   <li>Bob이 받은 신청 목록에서 확인하고 수락한다.</li>
 * </ol>
 */
@SpringBootTest
@AutoConfigureMockMvc
class CheckInMealHappyPathE2eTest extends AbstractPostgresTest {

    @Autowired
    private MockMvc mvc;

    // Boot 4는 Jackson 3(tools.jackson)을 빈으로 등록하므로 Jackson 2 ObjectMapper 빈은 없다.
    // 이 테스트는 요청 본문 직렬화·응답 파싱에만 매퍼가 필요하므로, 빈 주입 대신 직접 인스턴스화해 버전 의존을 끊는다.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("P1 핵심 루프: 온보딩 → 식당검색 → 체크인 → 혼밥러목록 → 같이먹기 신청·수락이 한 바퀴 돈다")
    void p1CoreLoop_endToEnd() throws Exception {
        // given: 두 회원이 각자 휴대폰 온보딩을 완료한다(번호·닉네임을 달리해 UNIQUE 제약 회피).
        //        수신자(Bob)는 allow_meal_request 기본값이 true라 별도 opt-in 없이 신청을 받을 수 있다.
        String aliceToken = onboard("01077770001", "e2eAlice"); // 신청자
        String bobToken = onboard("01077770002", "e2eBob");      // 수신자

        // when: Bob이 식당을 검색해 첫 결과로 혼밥 체크인한다(검색 결과의 external_id가 places로 캐싱 upsert된다).
        JsonNode search = perform(get("/api/places/search")
                .param("query", "김밥").param("lat", "37.5665").param("lng", "126.9780"), bobToken, 200);
        JsonNode place = search.path("data").path("content").get(0);
        assertThat(place).withFailMessage("검색 결과가 비어 있으면 안 된다").isNotNull();

        JsonNode checkIn = perform(jsonPost("/api/check-ins", Map.of(
                "externalId", place.path("externalId").asText(),
                "name", place.path("name").asText(),
                "latitude", place.path("latitude").asDouble(),
                "longitude", place.path("longitude").asDouble())), bobToken, 201);
        // then: 새 체크인은 ACTIVE 상태이고 체크인 id·장소 id가 발급된다.
        assertThat(checkIn.path("data").path("status").asText()).isEqualTo("ACTIVE");
        long bobCheckInId = checkIn.path("data").path("checkInId").asLong();
        long placeId = checkIn.path("data").path("placeId").asLong();

        // when: Alice가 같은 식당의 현재 혼밥러 목록을 조회하면
        JsonNode diners = perform(get("/api/places/" + placeId + "/check-ins"), aliceToken, 200);
        // then: Bob의 체크인이 목록에 노출된다.
        assertThat(containsCheckIn(diners.path("data"), bobCheckInId))
                .withFailMessage("혼밥러 목록에 Bob의 체크인이 보여야 한다").isTrue();

        // when: Alice가 Bob의 체크인을 대상으로 같이먹기를 신청하면
        JsonNode created = perform(jsonPost("/api/meal-requests", Map.of(
                "toCheckInId", bobCheckInId, "message", "같이 드실래요?")), aliceToken, 201);
        // then: PENDING 상태의 신청이 생성된다.
        assertThat(created.path("data").path("status").asText()).isEqualTo("PENDING");
        long mealRequestId = created.path("data").path("mealRequestId").asLong();

        // when: Bob이 받은 신청 목록을 조회하면 방금 신청이 들어와 있고
        JsonNode received = perform(get("/api/meal-requests").param("role", "received"), bobToken, 200);
        assertThat(containsMealRequest(received.path("data"), mealRequestId))
                .withFailMessage("Bob의 받은 신청 목록에 Alice의 신청이 보여야 한다").isTrue();

        // when: Bob이 그 신청을 수락하면
        JsonNode accepted = perform(patch("/api/meal-requests/" + mealRequestId + "/accept"), bobToken, 200);
        // then: 상태가 ACCEPTED로 전이된다(핵심 루프 완주).
        assertThat(accepted.path("data").path("status").asText()).isEqualTo("ACCEPTED");
    }

    @Test
    @DisplayName("사회적 증거 통계는 토큰 없이도 공개 조회된다(비로그인 첫 화면)")
    void stats_isPublic() throws Exception {
        // given: 토큰 없음 / when: 통계 조회 / then: 200 + 집계 필드 노출(보안 필터 체인의 permitAll 동작 확인)
        JsonNode stats = perform(get("/api/check-ins/stats"), null, 200);
        assertThat(stats.path("success").asBoolean()).isTrue();
        assertThat(stats.path("data").has("todayCount")).isTrue();
        assertThat(stats.path("data").has("activeCount")).isTrue();
    }

    // --- 헬퍼 ---

    /**
     * 휴대폰 온보딩을 끝까지 진행하고 정식 access 토큰을 돌려준다.
     * send-code → verify(코드 000000, 신규라 온보딩 토큰) → terms(필수 3종 동의) → complete(프로필) → 정식 토큰.
     */
    private String onboard(String phone, String nickname) throws Exception {
        perform(jsonPost("/api/auth/phone/send-code", Map.of("phone", phone)), null, 200);

        JsonNode verify = perform(jsonPost("/api/auth/phone/verify",
                Map.of("phone", phone, "code", "000000")), null, 200);
        assertThat(verify.path("data").path("onboarding").asBoolean())
                .withFailMessage("신규 번호는 온보딩 분기를 타야 한다").isTrue();
        String onboardingToken = verify.path("data").path("onboardingToken").asText();
        assertThat(onboardingToken).isNotBlank();

        perform(jsonPost("/api/auth/terms",
                Map.of("service", true, "privacy", true, "location", true, "marketing", false)),
                onboardingToken, 200);

        JsonNode complete = perform(jsonPost("/api/auth/complete", Map.of("nickname", nickname)),
                onboardingToken, 200);
        String accessToken = complete.path("data").path("accessToken").asText();
        assertThat(accessToken).withFailMessage("프로필 완료 시 정식 access 토큰이 발급돼야 한다").isNotBlank();
        return accessToken;
    }

    /** JSON 본문 POST 요청 빌더를 만든다. */
    private MockHttpServletRequestBuilder jsonPost(String path, Object body) throws Exception {
        return post(path).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));
    }

    /**
     * 요청을 수행하고 상태코드를 검증한 뒤 응답 본문을 {@link JsonNode}로 돌려준다.
     * token이 null이 아니면 {@code Authorization: Bearer ...} 헤더를 붙인다(보안 필터 체인이 검증).
     */
    private JsonNode perform(MockHttpServletRequestBuilder builder, String token, int expectedStatus) throws Exception {
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        String body = mvc.perform(builder)
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return body.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(body);
    }

    /** 혼밥러 목록(data 배열)에 주어진 체크인 id가 있는지. */
    private boolean containsCheckIn(JsonNode dataArray, long checkInId) {
        for (JsonNode node : dataArray) {
            if (node.path("checkInId").asLong() == checkInId) {
                return true;
            }
        }
        return false;
    }

    /** 신청 목록(data 배열)에 주어진 신청 id가 있는지. */
    private boolean containsMealRequest(JsonNode dataArray, long mealRequestId) {
        for (JsonNode node : dataArray) {
            if (node.path("mealRequestId").asLong() == mealRequestId) {
                return true;
            }
        }
        return false;
    }
}
