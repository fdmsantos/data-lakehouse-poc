kafka-console-consumer:
	./bin/kafka-console-consumer.sh --bootstrap-server <IP>:9094 --topic tweets --from-beginning

kafka-connect-hdfs-build:
	cd docker/kafka-connect-hdfs && docker build . -t kafka-connect-hdfs

start-lakehouse:
	aws ec2 start-instances --instance-ids $$INSTANCE_ID

dbt-build:
	docker build --no-cache -t dbt .

dbt-run:
	docker run \
    --network=host \
    --rm \
    --mount type=bind,source=/home/fsantos/Documents/Projects/Personal/data-lakehouse-poc/twitter-dbt/twitter_dbt,target=/usr/my-project \
    --mount type=bind,source=/home/fsantos/Documents/Projects/Personal/data-lakehouse-poc/twitter-dbt/twitter_dbt/profiles.yml,target=/root/.dbt/profiles.yml \
    --entrypoint /bin/bash \
    dbt -c "dbt deps; dbt run"
