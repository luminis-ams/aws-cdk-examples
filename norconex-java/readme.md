# Building the image and sending it to AWS ECR
```shell
$ mvn clean verify install
$ mkdir -p target/dependency && (cd target/dependency; jar -xf ../*.jar)
docker build -t luminis/norconex-java .
docker tag luminis/norconex-java:latest 044915237328.dkr.ecr.eu-west-1.amazonaws.com/norconex-java:latest

aws ecr get-login-password --region eu-west-1 | docker login --username AWS --password-stdin 044915237328.dkr.ecr.eu-west-1.amazonaws.com
docker push 044915237328.dkr.ecr.eu-west-1.amazonaws.com/norconex-java:latest
```

# Running it in your local Docker environment

```shell
docker network create norconexnet
docker run --name=elasticsearch --network=norconexnet -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" docker.elastic.co/elasticsearch/elasticsearch:7.12.0
docker run --network=norconexnet -p 5601:5601 docker.elastic.co/kibana/kibana:7.12.0
docker run --network=norconexnet -p 8080:8080 -e norconex.action=START -e AWS_ACCESS_KEY_ID=${aws_access_key_id} -e AWS_SECRET_KEY=${aws_secret_access_key} -e norconix.collectorName=EnvironmentCollector -e norconex.elasticsearch-nodes=http://elasticsearch:9200 -e aws.use-local=false -e aws.table-prefix=JettroCrawler -e aws.profile-name=default luminis/norconex-java:latest

docker run --network=norconexnet -p 8080:8080 -e norconex.action=CLEAN -e AWS_ACCESS_KEY_ID=${aws_access_key_id} -e AWS_SECRET_KEY=${aws_secret_access_key} -e norconix.collectorName=EnvironmentCollector -e norconex.elasticsearch-nodes=http://elasticsearch:9200 -e aws.use-local=false -e aws.table-prefix=JettroCrawler -e aws.profile-name=default luminis/norconex-java:latest
```
