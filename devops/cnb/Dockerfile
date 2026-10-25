FROM alpine:3.20

RUN apk add --no-cache \
      bash=5.2.26-r0 \
      openssh-client=9.7_p1-r5 \
    && adduser -D -u 1000 -h /home/deploy deploy

USER 1000
WORKDIR /home/deploy
