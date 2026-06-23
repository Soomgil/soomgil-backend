FROM eclipse-temurin:21-jdk

WORKDIR /workspace

COPY gradle ./gradle
COPY gradlew build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

COPY . .

EXPOSE 8080

CMD ["./gradlew", "bootRun", "--continuous", "--no-daemon"]
