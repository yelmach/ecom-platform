FROM jenkins/jenkins:2.580.1-jdk21

COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

COPY jenkins.yaml /usr/share/jenkins/config/jenkins.yaml
COPY jobs/ /usr/share/jenkins/config/jobs/
