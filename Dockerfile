# 1. 빌드된 .jar 파일을 실행할 가벼운 Java 환경 선택
FROM eclipse-temurin:21-jre-alpine

# 2. 컨테이너 내부의 작업 디렉토리 설정
WORKDIR /app

# 3. JAR 파일 복사
# 유성님이 `./gradlew bootJar`를 실행했을 때 생성되는 경로입니다.
# 파일 이름이 정확히 'AIQ-0.0.1-SNAPSHOT.jar'인지 꼭 확인해 보세요!
ARG JAR_FILE=build/libs/AIQ-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

# 4. 환경 변수 설정 (데이터베이스 암호 등은 실행 시점에 주입할 거예요)
# 타임존을 서울로 설정해서 로그 시간을 맞춥니다.
ENV TZ=Asia/Seoul

# 5. 실행 명령
# -Dspring.profiles.active=prod 를 추가해 서버 환경임을 명시할 수도 있습니다.
ENTRYPOINT ["java", "-jar", "app.jar"]