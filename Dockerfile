FROM eclipse-temurin:11
WORKDIR /home/app
COPY ./build/libs/ftp-mock-server.jar .
COPY ./resources ./resources
EXPOSE 21210 21212 21213
CMD ["java", "-jar", "ftp-mock-server.jar", "/home/app/resources"]
