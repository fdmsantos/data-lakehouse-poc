/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.example;

import org.apache.avro.generic.GenericArray;
import org.apache.avro.generic.GenericRecord;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.formats.avro.registry.confluent.ConfluentRegistryAvroDeserializationSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.util.Collector;

/**
 * Skeleton for a Flink DataStream Job.
 *
 * <p>For a tutorial how to write a Flink application, check the
 * tutorials and examples on the <a href="https://flink.apache.org">Flink Website</a>.
 *
 * <p>To package your application into a JAR file for execution, run
 * 'mvn clean package' on the command line.
 *
 * <p>If you change the name of the main class (with the public static void main(String[] args))
 * method, change the respective entry in the POM.xml file (simply search for 'mainClass').
 */
public class DataStreamJob {

	private final static String TOPIC = "tweets";
	private final static String CONSUMER_GROUP_ID = "flink-consumer";

	public static void main(String[] args) throws Exception {
		final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

		org.apache.avro.Schema SCHEMA$  = new org.apache.avro.Schema.Parser(). parse("{\"type\":\"record\"," +
				"\"name\":\"nba_tweet\"," +
				"\"fields\":[" +
				"{\"name\":\"id\",\"type\":\"string\"}," +
				"{\"name\":\"text\",\"type\":\"string\"}," +
				"{\"name\":\"players\",\"type\": {\"type\":\"array\",\"items\":\"string\"}}" +
				"]}");

		ConfluentRegistryAvroDeserializationSchema<GenericRecord> avroDeserializationSchema
				= ConfluentRegistryAvroDeserializationSchema.forGeneric(SCHEMA$, System.getenv("SCHEMA_REGISTRY_URL"));

		KafkaSource<GenericRecord> source = KafkaSource.<GenericRecord>builder()
				.setBootstrapServers(System.getenv("BOOTSTRAP_SERVERS"))
				.setTopics(TOPIC)
				.setGroupId(CONSUMER_GROUP_ID)
				.setStartingOffsets(OffsetsInitializer.latest())
				.setProperty("schema.registry.url", System.getenv("SCHEMA_REGISTRY_URL"))
				.setValueOnlyDeserializer(avroDeserializationSchema)
				.build();

		DataStream<Tuple2<String,Integer>> dataStream = env
				.fromSource(source, WatermarkStrategy.noWatermarks(),"Kafka Source")
				.flatMap(new FlatMapFunction<GenericRecord, Tuple2<String,Integer>>() {
					public void flatMap(GenericRecord record, Collector<Tuple2<String, Integer>> out) {
						GenericArray players = (GenericArray) record.get("players");
						for (Object player : players) {
							out.collect(new Tuple2<>(player.toString(), 1));
						}
					}
				})
				.keyBy(value -> value.f0)
				.window(TumblingProcessingTimeWindows.of(Time.seconds(10)))
				.sum(1);

		dataStream.print();

		// Execute program, beginning computation.
		env.execute("Flink Java API Skeleton");
	}


}
