# Dockerfile to build container for unit testing.
#
# To build the image, run the following from this directory:
#   docker build -t beast_testing .
#
# To run the tests, use
#   docker run beast_testing
#
# To run just the core tests, use
#   docker run beast_testing test-core
#
# To run the tests interactively, use
#   docker run --entrypoint /bin/bash -it -p 5900:5900 beast_testing
# This will give you a shell in the container. From this
# shell, run
#   ./entrypoint.sh
# or
#   ./entrypoint.sh test-core
#
# The previous command exposes the VNC session, so while the
# BEAUti test suite is running you should be able to run a
# VNC viewer and connect it to localhost (password: password)
# to observe the graphical output of these tests.

FROM openjdk:8

# Install Apache Ant
RUN apt-get update && apt-get install -y ant

# Install and configure VNC server
RUN apt-get update && apt-get install -y tightvncserver twm
RUN mkdir /root/.vnc
RUN echo password | vncpasswd -f > /root/.vnc/passwd
RUN chmod 600 /root/.vnc/passwd

# Install BEAGLE
RUN apt-get update && apt-get install -y build-essential autoconf automake libtool pkg-config
# use latest release v3.0.1, issue #786
RUN cd /root && git clone --branch v3.0.1 --depth=1 https://github.com/beagle-dev/beagle-lib.git
RUN cd /root/beagle-lib && ./autogen.sh && ./configure --prefix=/usr/local && make install
RUN ldconfig

ADD . ./

RUN echo "#!/bin/bash\n" \
        "export USER=root\n" \
        "export DISPLAY=:1\n" \
        "vncserver :1 -geometry 1920x1080\n" \
        "ant -f build-testing.xml \$1\n" > entrypoint.sh
RUN chmod a+x entrypoint.sh

ENTRYPOINT ["./entrypoint.sh"]

CMD ["test-all"]
