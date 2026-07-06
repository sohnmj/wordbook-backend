FROM eclipse-temurin:17-jre-alpine

# [꿀팁] 한국 시간(KST) 설정 (Alpine 리눅스 전용 명령어로 수정)
# Alpine 리눅스는 tzdata 패키지를 따로 설치해줘야 시간을 바꿀 수 있습니다.
ENV TZ=Asia/Seoul
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone

WORKDIR /app


ARG JAR_FILE=build/libs/*SNAPSHOT.jar

COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]