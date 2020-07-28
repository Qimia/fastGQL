FROM mysql:8

LABEL maintainer="Mingyi Zhang"

COPY docker.cnf /etc/mysql/conf.d/
COPY init.sql /docker-entrypoint-initdb.d/