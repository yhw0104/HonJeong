package com.honjeong.global.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import com.honjeong.chat.ws.ChatWebSocketHandler;
import com.honjeong.chat.ws.WsHandshakeInterceptor;

import jakarta.servlet.ServletContext;
import jakarta.websocket.server.ServerContainer;

/**
 * 채팅 소켓 등록.
 *
 * <p><b>{@code setAllowedOrigins("*")}를 명시적으로 연다.</b> 이유는 세 가지다.
 *
 * <ol>
 *   <li><b>클라이언트가 네이티브 앱이라 Origin을 검증할 수 있는 값으로 볼 수 없다.</b> iOS RN의
 *       SocketRocket 계층은 {@code wss://호스트}를 {@code https://호스트}로 바꿔 Origin 헤더를
 *       실어 보낸다(안 보낸다고 가정하면 안 된다). 그렇다고 그 값이 무언가를 증명하지도 않는다 —
 *       Origin은 브라우저가 강제할 때만 의미가 있고, 네이티브 클라이언트는 아무 값이나 넣을 수 있다.
 *   <li><b>인증은 Origin이 아니라 티켓이 한다</b>({@link WsHandshakeInterceptor}). 유효한 1회용
 *       티켓이 없으면 Origin이 무엇이든 연결되지 않는다. 그래서 {@code "*"}가 열어 주는 것은
 *       "티켓을 이미 가진 사람이 어느 Origin에서든 붙을 수 있다"는 것뿐이고, 티켓은 로그인한
 *       본인만 {@code POST /api/ws-ticket}으로 받을 수 있다.
 *   <li><b>TLS를 끊는 리버스 프록시 뒤에서는 same-origin 검사 자체가 성립하지 않는다.</b> Caddy가
 *       TLS를 종료하고 {@code reverse_proxy app:8080}으로 평문 HTTP를 넘기므로, 서버가 보는
 *       자기 스킴은 {@code http}인데 Origin 헤더는 {@code https}다. Spring이 기본으로 끼워 넣는
 *       {@code OriginHandshakeInterceptor}는 {@code WebUtils.isSameOrigin()}으로 이 둘을 비교하고
 *       {@code X-Forwarded-*}를 의도적으로 무시하므로, 열어 두지 않으면 운영에서 핸드셰이크가
 *       전부 403이 된다(로컬은 전부 {@code http://localhost:8080}이라 통과해 안 드러난다).
 * </ol>
 *
 * <p>★ 그 기본 인터셉터는 우리 인터셉터 <b>뒤에</b> 붙는다. 즉 403으로 거절되기 전에 티켓이 이미
 * 소모되므로, 열어 두지 않으면 앱이 30초마다 티켓을 태우며 영원히 재시도한다.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);

    /**
     * 서버가 조용한 세션을 끊는 한계 시간(ms).
     *
     * <p>★ <b>이 값은 앱의 ping 주기와 한 쌍이다</b>({@code app/src/shared/ws/client.ts}의
     * {@code PING_MS} = 30초). 살아 있는 연결은 30초마다 ping을 보내므로 60초 창 안에 항상
     * 두 번의 기회가 있다 — 한 번 유실돼도 끊기지 않는다. 한쪽만 바꾸면(예: 여기를 20초로
     * 줄이거나 앱의 ping을 60초로 늘리면) <b>멀쩡히 살아 있는 소켓이 조용히 끊긴다.</b>
     * 증상은 "가끔 대화가 멈춘다"라서 원인을 찾기 어렵다. 반드시 같이 조정한다.
     */
    private static final long SESSION_IDLE_TIMEOUT_MS = 60_000L;

    private final ChatWebSocketHandler handler;
    private final WsHandshakeInterceptor handshakeInterceptor;

    public WebSocketConfig(ChatWebSocketHandler handler, WsHandshakeInterceptor handshakeInterceptor) {
        this.handler = handler;
        this.handshakeInterceptor = handshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins("*");
    }

    /**
     * 유휴 세션 정리. 앱이 죽거나 네트워크가 사라져 close 프레임이 오지 않은 세션은
     * {@code afterConnectionClosed}가 불리지 않아 {@code WsSessionRegistry}에 영원히 남는다 —
     * 그러면 죽은 세션에 계속 전송을 시도하고 메모리도 샌다. 하트비트가 끊긴 세션을 컨테이너가
     * 직접 끊게 해서 정리 경로를 태운다(설계 문서 §8).
     *
     * <p>★ 진짜 서블릿 컨테이너가 있을 때만 만든다. {@code @SpringBootTest}의 기본 MOCK 웹 환경은
     * {@code MockServletContext}라 {@code ServerContainer} 속성이 없고,
     * {@code ServletServerContainerFactoryBean}은 그 속성이 없으면 초기화 단계에서 예외를 던져
     * <b>통합 테스트의 컨텍스트 로딩을 통째로 깨뜨린다.</b> 설정할 대상이 애초에 없는 환경이므로
     * 조용히 건너뛴다.
     *
     * <p>운영 경로의 동작은 그대로다 — 내장 Tomcat은 {@code onRefresh()}에서 이미 기동해
     * {@code WsSci}가 이 속성을 채워 둔 뒤에 이 빈이 만들어지므로, 여기서 null이 나올 일이 없다.
     * 그럼에도 "조용히 적용 안 됨"은 증상이 안 보이는 고장이라(세션이 영영 안 끊긴다) 로그로 남긴다.
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer(ServletContext servletContext) {
        if (servletContext.getAttribute(ServerContainer.class.getName()) == null) {
            log.info("[ws] 서블릿 컨테이너가 없어 유휴 세션 타임아웃을 건너뛴다(테스트 환경)");
            return null;
        }
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxSessionIdleTimeout(SESSION_IDLE_TIMEOUT_MS);
        return container;
    }
}
