#/bin/bash
mvn clean package
java -jar ./target/http2Server-vertx-1.0-SNAPSHOT-fat.jar -conf ./src/main/conf/Http2Server.json
