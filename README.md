
# Twitter Scanner
------------------------
running in local with debugger : 
docker run --rm --name=mysqltest -p 3306:3306 -e MYSQL_ROOT_PASSWORD=1234 -e MYSQL_DATABASE=bieber -d mysql 
mvn quarkus:dev -Ddebug -Dsecret=app_secret -Duser=twitter_user -Dpassword=twitter_password

building : mvn clean install -Dmaven.test.skip=true
building and run tests: mvn clean install -Dsecret=app_secret -Duser=twitter_user -Dpassword=twitter_password

docker:
docker build -t spannozzo/twitter-scanner -f .\src\main\docker\Dockerfile.jvm .
docker run --rm --name=mysqltest -e MYSQL_ROOT_PASSWORD=1234 -e MYSQL_DATABASE=bieber -d mysql 
docker inspect mysqltest - take not of docker ip address (e.g 172.17.0.3)
optional: docker run --rm --name myadmin -d --link mysqltest:db -p 8084:80 phpmyadmin/phpmyadmin 
docker run -e host=172.17.0.3 -e secret=app_secret -e user=twitter_user -e password=twitter_password --rm -it --link jaeger -p 8080-8083:8080-8083 -e JAEGER_AGENT_HOST="jaeger" spannozzo/twitter-scanner

for distribute log tracer:
docker run -d --rm --name jaeger -e COLLECTOR_ZIPKIN_HTTP_PORT=9411 -p 5775:5775/udp -p 6831:6831/udp -p 6832:6832/udp -p 5778:5778 -p 16686:16686 -p 14268:14268 -p 14250:14250 -p 9411:9411 jaegertracing/all-in-one:1.18

then from postman you will be able to make any search at http://localhost:8080/v2/messages/[maxTwits]/[twitter]

e.g. http://localhost:8080/v2/messages/2/ladygaga will retrieve the incoming first 2 twits related to ladygaga keyword

{
    "traceId": "423feb1a4068ae0",
    "values": [
        {
            "key": {
                "createdAt": "2018-11-11T05:10:50Z[UTC]",
                "id": "1061486386673373184",
                "name": "chaengie Â®ï¸�FANSÃ‰Â®ï¸�",
                "screenName": "mycutiepie211"
            },
            "value": [
                {
                    "author": {
                        "createdAt": "2018-11-11T05:10:50Z[UTC]",
                        "id": "1061486386673373184",
                        "name": "chaengie Â®ï¸�FANSÃ‰Â®ï¸�",
                        "screenName": "mycutiepie211"
                    },
                    "createdAt": "2020-07-08T01:17:49Z[UTC]",
                    "id": "1280672402788118528",
                    "text": "RT @chartdatab: Maiores debuts de canÃ§Ãµes internacionais em streams no Brasil em 2020 (atÃ© agora):\n\n1. @ygofficialblink - How You Like Thatâ€¦"
                }
            ]
        },
        {
            "key": {
                "createdAt": "2019-07-15T01:31:18Z[UTC]",
                "id": "1150578554293379073",
                "name": "Marco ð“†�",
                "screenName": "marciboy"
            },
            "value": [
                {
                    "author": {
                        "createdAt": "2019-07-15T01:31:18Z[UTC]",
                        "id": "1150578554293379073",
                        "name": "Marco ð“†�",
                        "screenName": "marciboy"
                    },
                    "createdAt": "2020-07-08T01:17:49Z[UTC]",
                    "id": "1280672403308318721",
                    "text": "@andypgaga @ladygaga Don't know what may have happened that didn't make your bday amazing I mean with Corona alone it  can be pretty sad"
                }
            ]
        }
    ]
}
and the trace id can be used to track the logs at http://localhost:16686/trace/423feb1a4068ae0, and in general all the logs can be seen through the trace application
at http://localhost:16686/search

Metrics: metrics are available at http://localhost:8080/metrics/application 
