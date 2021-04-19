FROM adoptopenjdk/openjdk11:alpine-jre
RUN mkdir -p /work
RUN addgroup -S spring && adduser -S spring -G spring
RUN chown spring:spring /work
USER spring:spring
ARG DEPENDENCY=target/dependency
COPY ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY ${DEPENDENCY}/META-INF /app/META-INF
COPY ${DEPENDENCY}/BOOT-INF/classes /app
ENTRYPOINT ["java","-cp","app:app/lib/*","eu.luminis.aws.norconex.NorconexApplication"]
