# Dockerfile to build container for unit testing

FROM openjdk:8

RUN apt-get update && apt-get install -y git ant

# Ant build fails if the repo dir isn't named beast2
RUN mkdir /root/beast2
WORKDIR /root/beast2

ADD . ./

CMD ant travis
