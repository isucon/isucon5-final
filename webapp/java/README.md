### Build

    ./mvnw clean package -Dmaven.test.skip=true

### Run

    java -jar target/isucon5f-0.0.1-SNAPSHOT.jar

Specifying server port

    java -jar target/isucon5f-0.0.1-SNAPSHOT.jar --server.port=8888



Enabling SQL log (for development)

    java -jar target/isucon5f-0.0.1-SNAPSHOT.jar --spring.profiles.active=sqllog

### Option (Memo)

    docker run --rm --name isucon5f -p 5432:5432 -e POSTGRES_PASSWORD= -e POSTGRES_USER=isucon5f -e LC_ALL=C.UTF-8 postgres
    
    export ISUCON5_DB_HOST=192.168.99.100
    export ISUCON5_DB_USER=isucon5f