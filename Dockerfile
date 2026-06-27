FROM eclipse-temurin:21-jdk

# 컨테이너 OS/JVM 기본 타임존을 KST로 (now() 기반 캔들 시각이 UTC로 밀리는 문제 방지)
ENV TZ=Asia/Seoul

WORKDIR /app

COPY build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]