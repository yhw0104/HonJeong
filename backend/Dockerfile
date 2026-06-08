# 멀티스테이지 빌드: 1) Gradle로 빌드 → 2) 경량 JRE 런타임
# 배포 단계에서 사용. (로컬 개발은 ./gradlew bootRun + H2)

# --- 1단계: 빌드 ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
# 의존성 캐시 레이어 (소스 변경 시에도 재다운로드 최소화)
RUN ./gradlew --no-daemon dependencies || true
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar

# --- 2단계: 런타임 ---
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "app.jar"]
