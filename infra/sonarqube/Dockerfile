FROM alpine:3.22 AS branch-plugin

# Checksums from the immutable 26.6.0 GitHub release assets.
ADD --checksum=sha256:934ee985bbccfe5aff667c28ec8de3716f28b5203a635cac2d3e9162b6244747 \
    https://github.com/mc1arke/sonarqube-community-branch-plugin/releases/download/26.6.0/sonarqube-community-branch-plugin-26.6.0.jar /plugin.jar
ADD --checksum=sha256:4b75ef6e94c39d15fb3d1134785357ebbf94f124f15196de2a4fe1f2465eef75 \
    https://github.com/mc1arke/sonarqube-community-branch-plugin/releases/download/26.6.0/sonarqube-webapp.zip /webapp.zip
RUN mkdir /webapp && unzip -q /webapp.zip -d /webapp

FROM sonarqube:26.6.0.123539-community

COPY --from=branch-plugin --chown=sonarqube:root /plugin.jar /opt/sonarqube/extensions/plugins/sonarqube-community-branch-plugin-26.6.0.jar
RUN chmod -R 770 /opt/sonarqube/web && rm -rf /opt/sonarqube/web/*
COPY --from=branch-plugin --chown=sonarqube:root /webapp/ /opt/sonarqube/web/
RUN chmod -R 550 /opt/sonarqube/web

ENV SONAR_WEB_JAVAADDITIONALOPTS="-javaagent:/opt/sonarqube/extensions/plugins/sonarqube-community-branch-plugin-26.6.0.jar=web" \
    SONAR_CE_JAVAADDITIONALOPTS="-javaagent:/opt/sonarqube/extensions/plugins/sonarqube-community-branch-plugin-26.6.0.jar=ce"
