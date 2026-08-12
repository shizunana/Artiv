@echo off
set MAVEN_OPTS=-Dmaven.repo.local=C:\Users\95847\.m2\repository
cd /d G:\ProjectForGraduation\huiyue\backend
mvn spring-boot:run > G:\ProjectForGraduation\huiyue\backend\spring-boot-test.log 2>&1
