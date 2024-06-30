#FROM ubuntu:latest
#LABEL authors="hiyeop"
#
#ENTRYPOINT ["top", "-b"]

# 프로젝트 루트 디렉토리에 Dockerfile 생성

# Dockerfile

# 1. Java 이미지를 베이스 이미지로 사용
FROM openjdk:17-jdk-slim

# 2. 작업 디렉토리를 설정
WORKDIR /app

# 3. jar 파일을 컨테이너에 복사
COPY build/libs/server-0.0.1-SNAPSHOT.jar /app/myapp.jar

# 4. 환경 변수 설정 (필요 시)
# ENV SPRING_PROFILES_ACTIVE=prod

# 5. 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "myapp.jar"]
