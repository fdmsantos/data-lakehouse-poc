# Data Lakehouse Poc
Project to spin up data lakehouse poc on AWS EC2

## Configure Nobody

```shell
hadoop fs -mkdir  /user
hadoop fs -chmod 777 /user
hadoop fs -mkdir  /user/nobody
```

## Configure Hive

```shell
hadoop fs -mkdir  /user/hive
hadoop fs -mkdir  /user/hive/warehouse
hadoop fs -mkdir  /user/tmp
hadoop fs -chmod g+w /user/tmp
hadoop fs -chmod g+w /user/hive/warehouse
```