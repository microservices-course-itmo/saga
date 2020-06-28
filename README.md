# saga

Чтобы запустить Kafka, Neo4j и сам сервис в Docker:
 1. `mvn spring-boot:build-image`
 2. Запустить `local-all.yml` файл, используя Ide или с помощью команды `docker-compose -f profiles/local-all.yml up`
 
 При изменении версии в сервиса в pom файле нужно изменить версию приложения в local-all.yml