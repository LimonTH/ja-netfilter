FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build
COPY . .
RUN ./gradlew build --no-daemon

FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache tini

COPY --from=builder /build/build/libs/*.jar /app/ja-netfilter.jar
COPY --from=builder /build/config /app/config/
COPY --from=builder /build/plugins /app/plugins/

WORKDIR /app

EXPOSE 8080

ENTRYPOINT ["/sbin/tini", "--"]
CMD ["java", "-jar", "ja-netfilter.jar"]