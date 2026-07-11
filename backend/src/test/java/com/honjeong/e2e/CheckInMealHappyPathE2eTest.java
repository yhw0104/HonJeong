package com.honjeong.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.honjeong.place.domain.Place;
import com.honjeong.place.repository.PlaceRepository;
import com.honjeong.support.AbstractPostgresTest;

/**
 * Slice 7/8 — P1 핵심 루프 E2E 스모크 테스트.
 *
 * <p>각 슬라이스(인증·장소·체크인·같이먹기)는 단위·슬라이스 테스트로 검증됐지만, 이 테스트는 그것들이
 * <b>하나로 엮였을 때</b> 실제 요청 한 바퀴가 끝까지 도는지를 확인한다. {@code @SpringBootTest}로 전체 컨텍스트를
 * 올리고 {@code @AutoConfigureMockMvc}로 받은 MockMvc는 운영과 동일한 <b>Spring Security 필터 체인 → 컨트롤러 →
 * 서비스 → 리포지토리 → Testcontainers PostgreSQL</b>을 그대로 통과한다(JWT 인증·인가·Flyway 스키마 포함).
 * 즉 Mock으로 가린 것은 외부 연동(SMS)뿐이고, 인증 토큰·트랜잭션·DB 제약은 전부 진짜다.
 *
 * <p>검증 시나리오(설계 계획의 happy path):
 * <ol>
 *   <li>두 회원이 휴대폰 온보딩을 끝까지 마치고 정식 토큰을 받는다(신청자 Alice·수신자 Bob).</li>
 *   <li>장소를 DB에 직접 시드하고 그 placeId로 Bob이 혼밥 체크인한다(체크인의 정문은 SEEKING·모집중).</li>
 *   <li>Alice가 Bob의 체크인(SEEKING)을 대상으로 같이먹기를 신청한다.</li>
 *   <li>Bob이 받은 신청 목록에서 확인하고 수락한다(수신자 SEEKING→TOGETHER 매칭).</li>
 * </ol>
 *
 * <p>혼밥러 목록(GET /api/places/{placeId}/check-ins)은 아직 ACTIVE(혼밥중)만 노출한다 — SEEKING(모집중)
 * 대상을 보여주는 목록으로의 전환은 이 재설계 플랜의 후속 태스크(모집중 목록 조회) 몫이라 이 해피패스에서는
 * 그 중간 발견 단계를 검증하지 않는다(Alice는 체크인 응답으로 받은 bobCheckInId를 바로 사용한다).
 *
 * <p>추가로 {@code accept_whenSenderHasExistingActiveElsewhere_endsItAndInsertsNewTogether}는 발신자가
 * 이미 다른 식당에 ACTIVE 체크인을 가진 상태에서 수락되는 경로를 실 Postgres로 검증한다 —
 * {@code MealRequestService.accept}의 flush-before-insert 순서가 {@code uq_check_ins_current_user}
 * 부분 유니크 인덱스 위반을 실제로 회피하는지를 확인한다(단위 테스트는 Mockito라 실 제약 위반을 잡지 못한다).
 */
@SpringBootTest
@AutoConfigureMockMvc
class CheckInMealHappyPathE2eTest extends AbstractPostgresTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** 이 테스트가 커밋한 행(check_ins·places·meal_requests 등)이 다른 테스트를 오염시키지 않도록 정리한다. */
    @AfterEach
    void cleanUp() {
        jdbcTemplate.execute("TRUNCATE meal_requests, review_tags, review_photos, reviews, check_ins, favorites, places RESTART IDENTITY");
    }

    // Boot 4는 Jackson 3(tools.jackson)을 빈으로 등록하므로 Jackson 2 ObjectMapper 빈은 없다.
    // 이 테스트는 요청 본문 직렬화·응답 파싱에만 매퍼가 필요하므로, 빈 주입 대신 직접 인스턴스화해 버전 의존을 끊는다.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("P1 핵심 루프: 온보딩 → 식당체크인(SEEKING) → 같이먹기 신청·수락이 한 바퀴 돈다")
    void p1CoreLoop_endToEnd() throws Exception {
        // given: 두 회원이 각자 휴대폰 온보딩을 완료한다(번호·닉네임을 달리해 UNIQUE 제약 회피).
        //        수신자(Bob)는 allow_meal_request 기본값이 true라 별도 opt-in 없이 신청을 받을 수 있다.
        String aliceToken = onboard("01077770001", "e2eAlice"); // 신청자
        String bobToken = onboard("01077770002", "e2eBob");      // 수신자

        // when: Bob이 식당을 검색한다(우리 DB 기반 검색, E2E 환경에서는 공공데이터 미적재라 빈 결과 정상).
        //       검색 엔드포인트 자체가 200을 반환하는지만 확인한다.
        perform(get("/api/places/search").param("query", "김밥"), bobToken, 200);

        // 장소를 DB에 직접 시드한다(공공데이터 마스터 기반 placeId 체크인 흐름).
        Place e2ePlace = placeRepository.save(
                Place.ofPublicData("E2E-001", "E2E테스트식당", "한식", "서울 어딘가", "서울 도로명",
                        37.5665, 126.9780, "02-000-0000", "영업"));
        long placeId = e2ePlace.getId();

        // Bob이 시드된 식당 id로 혼밥 체크인한다.
        JsonNode checkIn = perform(jsonPost("/api/check-ins", Map.of("placeId", placeId)), bobToken, 201);
        // then: 새 체크인은 SEEKING(모집중) 상태이고 체크인 id·장소 id가 발급된다(체크인의 정문은 SEEKING).
        assertThat(checkIn.path("data").path("status").asText()).isEqualTo("SEEKING");
        long bobCheckInId = checkIn.path("data").path("checkInId").asLong();
        assertThat(checkIn.path("data").path("placeId").asLong()).isEqualTo(placeId);

        // when: Alice가 Bob의 체크인(SEEKING)을 대상으로 같이먹기를 신청하면
        // (혼밥러 목록 GET /api/places/{placeId}/check-ins은 아직 ACTIVE 전용이라 SEEKING인 Bob이 보이지
        //  않는다 — 모집중 목록으로의 전환은 후속 태스크 몫. 여기서는 체크인 응답의 checkInId를 바로 쓴다.)
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
    @DisplayName("수락: 발신자가 다른 식당에 기존 ACTIVE를 가진 채로 수락돼도 유니크 위반 없이 매칭된다"
            + "(flush-before-insert 실 DB 검증)")
    void accept_whenSenderHasExistingActiveElsewhere_endsItAndInsertsNewTogether() throws Exception {
        // given: 발신자(Alice)·수신자(Bob) 온보딩.
        String aliceToken = onboard("01077771001", "e2eAliceActive"); // 발신자 — 기존 ACTIVE 보유 예정
        String bobToken = onboard("01077771002", "e2eBobActive");      // 수신자

        // 식당 X(수신자 Bob이 체크인)·Y(발신자 Alice가 먼저 체크인) 시드.
        Place placeX = placeRepository.save(
                Place.ofPublicData("E2E-X", "식당X", "한식", "서울 어딘가", "서울 도로명X",
                        37.5665, 126.9780, "02-000-0001", "영업"));
        Place placeY = placeRepository.save(
                Place.ofPublicData("E2E-Y", "식당Y", "한식", "서울 어딘가", "서울 도로명Y",
                        37.5651, 126.9895, "02-000-0002", "영업"));

        // 1) 수신자 B가 식당 X에 체크인(ACTIVE).
        JsonNode bobCheckIn = perform(jsonPost("/api/check-ins", Map.of("placeId", placeX.getId())), bobToken, 201);
        long bobCheckInId = bobCheckIn.path("data").path("checkInId").asLong();

        // 2) 발신자 A가 다른 식당 Y에 체크인(정문은 SEEKING)한 뒤 혼자 먹기로 전환해 ACTIVE(혼밥중)로 만든다
        //    — 이 테스트가 검증하려는 "기존 ACTIVE 보유" 상태는 이제 SEEKING→dine-alone을 거쳐야 나온다.
        JsonNode aliceCheckIn = perform(jsonPost("/api/check-ins", Map.of("placeId", placeY.getId())), aliceToken, 201);
        long aliceOriginalCheckInId = aliceCheckIn.path("data").path("checkInId").asLong();
        JsonNode aliceDineAlone = perform(
                patch("/api/check-ins/" + aliceOriginalCheckInId + "/dine-alone"), aliceToken, 200);
        assertThat(aliceDineAlone.path("data").path("status").asText()).isEqualTo("ACTIVE");

        // 3) A가 B의 체크인으로 같이먹기 신청 → B가 수락.
        JsonNode created = perform(jsonPost("/api/meal-requests", Map.of(
                "toCheckInId", bobCheckInId, "message", "같이 드실래요?")), aliceToken, 201);
        long mealRequestId = created.path("data").path("mealRequestId").asLong();

        // when: B가 수락하면 예외 없이 200이 돌아온다
        // (= 서비스가 발신자의 기존 ACTIVE를 end() + flush()한 뒤 새 TOGETHER를 insert해
        //   uq_check_ins_current_user 유니크 인덱스 위반을 실제로 회피했다는 뜻).
        JsonNode accepted = perform(patch("/api/meal-requests/" + mealRequestId + "/accept"), bobToken, 200);
        assertThat(accepted.path("data").path("status").asText()).isEqualTo("ACCEPTED");

        // then(실 DB 왕복 재조회): 각 체크인의 최종 상태를 원시 SQL로 직접 확인한다.
        // A의 식당 Y 체크인 → ENDED.
        Map<String, Object> aliceOriginalRow = jdbcTemplate.queryForMap(
                "SELECT status FROM check_ins WHERE id = ?", aliceOriginalCheckInId);
        assertThat(aliceOriginalRow.get("status")).isEqualTo("ENDED");

        // B의 식당 X 체크인 → TOGETHER(matched_at·meal_request_id 세팅).
        Map<String, Object> bobRow = jdbcTemplate.queryForMap(
                "SELECT status, matched_at, meal_request_id FROM check_ins WHERE id = ?", bobCheckInId);
        assertThat(bobRow.get("status")).isEqualTo("TOGETHER");
        assertThat(bobRow.get("matched_at")).isNotNull();
        assertThat(((Number) bobRow.get("meal_request_id")).longValue()).isEqualTo(mealRequestId);

        // A의 새 체크인(식당 X, TOGETHER, meal_request_id=신청 id) 1건 존재 — 기존 행의 update가 아니라 별도 insert여야 한다.
        Long aliceUserId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE nickname = ?", Long.class, "e2eAliceActive");
        List<Map<String, Object>> aliceTogetherRows = jdbcTemplate.queryForList(
                "SELECT id, place_id, meal_request_id FROM check_ins WHERE user_id = ? AND status = 'TOGETHER'",
                aliceUserId);
        assertThat(aliceTogetherRows).hasSize(1);
        Map<String, Object> aliceNewRow = aliceTogetherRows.get(0);
        assertThat(((Number) aliceNewRow.get("id")).longValue()).isNotEqualTo(aliceOriginalCheckInId);
        assertThat(((Number) aliceNewRow.get("place_id")).longValue()).isEqualTo(placeX.getId());
        assertThat(((Number) aliceNewRow.get("meal_request_id")).longValue()).isEqualTo(mealRequestId);

        // 인덱스 불변식: A는 ACTIVE/TOGETHER를 통틀어 정확히 1건만 가진다(uq_check_ins_current_user 유지).
        Integer aliceActiveOrTogetherCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM check_ins WHERE user_id = ? AND status IN ('ACTIVE','TOGETHER')",
                Integer.class, aliceUserId);
        assertThat(aliceActiveOrTogetherCount).isEqualTo(1);
    }

    @Test
    @DisplayName("종료: TOGETHER 매칭 중 한쪽이 종료 엔드포인트를 호출하면 양쪽 체크인이 모두 ENDED된다"
            + "(매칭 쌍 동시 종료 불변식, 실 DB 확증)")
    void endCheckIn_whenTogether_endsBothSides() throws Exception {
        // given: 발신자(Alice)·수신자(Bob) 온보딩(다른 테스트와 전화번호·닉네임 유니크 충돌 없게 새 값 사용).
        String aliceToken = onboard("01077772001", "e2eAliceEnd"); // 발신자
        String bobToken = onboard("01077772002", "e2eBobEnd");      // 수신자

        // 식당 X 시드 — 수신자 Bob이 체크인할 곳.
        Place placeX = placeRepository.save(
                Place.ofPublicData("E2E-END-X", "식당종료X", "한식", "서울 어딘가", "서울 도로명종료X",
                        37.5665, 126.9780, "02-000-0003", "영업"));

        // 1) 수신자 B가 식당 X에 체크인(ACTIVE).
        JsonNode bobCheckIn = perform(jsonPost("/api/check-ins", Map.of("placeId", placeX.getId())), bobToken, 201);
        long bobCheckInId = bobCheckIn.path("data").path("checkInId").asLong();

        // 2) 발신자 A가 B에게 같이먹기를 신청하고, B가 수락한다 — A·B 둘 다 TOGETHER로 전이.
        JsonNode created = perform(jsonPost("/api/meal-requests", Map.of(
                "toCheckInId", bobCheckInId, "message", "같이 드실래요?")), aliceToken, 201);
        long mealRequestId = created.path("data").path("mealRequestId").asLong();
        perform(patch("/api/meal-requests/" + mealRequestId + "/accept"), bobToken, 200);

        // A의 새 TOGETHER 체크인 id를 원시 SQL로 확보한다(accept 응답 본문엔 체크인 id가 없다).
        Long aliceUserId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE nickname = ?", Long.class, "e2eAliceEnd");
        Long aliceCheckInId = jdbcTemplate.queryForObject(
                "SELECT id FROM check_ins WHERE user_id = ? AND status = 'TOGETHER'", Long.class, aliceUserId);

        // when: 매칭 한쪽(Bob)만 자기 체크인을 종료 엔드포인트로 종료하면
        JsonNode ended = perform(patch("/api/check-ins/" + bobCheckInId + "/end"), bobToken, 200);
        assertThat(ended.path("data").path("status").asText()).isEqualTo("ENDED");

        // then(실 DB 왕복 재조회): 종료를 요청하지 않은 A의 체크인도 함께 ENDED여야 한다
        // (같이먹기는 한쪽만 끝낼 수 없다 — CheckInService.endCheckIn의 파트너 동시 종료 로직을 실 Postgres로 확증).
        Map<String, Object> bobRow = jdbcTemplate.queryForMap(
                "SELECT status, ended_at FROM check_ins WHERE id = ?", bobCheckInId);
        assertThat(bobRow.get("status")).isEqualTo("ENDED");
        assertThat(bobRow.get("ended_at")).isNotNull();

        Map<String, Object> aliceRow = jdbcTemplate.queryForMap(
                "SELECT status, ended_at FROM check_ins WHERE id = ?", aliceCheckInId);
        assertThat(aliceRow.get("status")).isEqualTo("ENDED");
        assertThat(aliceRow.get("ended_at")).isNotNull();
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
