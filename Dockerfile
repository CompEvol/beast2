# Dockerfile to build container for unit testing.
#
# To build the image, run the following from this directory:
#   docker build -t beast_testing .
#
# To run the tests, use
#   docker run beast_testing
#
# To run the tests interactively, use
#   docker run -it -p 5900:5900 beast_testing /bin/bash
# This will give you a shell in the container. From this
# shell, run
#   vncserver $DISPLAY -geometry 1920x1080; ant travis
#
# The previous command exposes the VNC session, so while the
# BEAUti test suite is running you can run a VNC viewer and
# connect it to localhost (password: password) to observe
# the graphical output of these tests.

FROM openjdk:8

RUN apt-get update && apt-get install -y ant tightvncserver twm

ENV DISPLAY :0
ENV USER root
RUN mkdir /root/.vnc
RUN echo password | vncpasswd -f > /root/.vnc/passwd
RUN chmod 600 /root/.vnc/passwd

# Ant build fails if the repo dir isn't named beast2
RUN mkdir /root/beast2
WORKDIR /root/beast2

ADD . ./

CMD vncserver $DISPLAY -geometry 1920x1080; ant travis
