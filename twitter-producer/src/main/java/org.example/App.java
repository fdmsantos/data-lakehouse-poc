package org.example;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ExecutionException;

/*
 * Sample code to demonstrate the use of the Filtered Stream endpoint
 * */
public class App {

    private final static String TOPIC = "tweets";

    public static void main(String args[]) throws IOException, URISyntaxException, ExecutionException, InterruptedException {
        String bearerToken = System.getenv("BEARER_TOKEN");
        if (null != bearerToken) {
            Map<String, String> rules = new HashMap<>();
            rules.put("context:60.* context:26.706083889454813185", "nba athletes");
            setupRules(bearerToken, rules);
            connectStream(bearerToken, createProducer());
        } else {
            System.out.println("There was a problem getting your bearer token. Please make sure you set the BEARER_TOKEN environment variable");
        }
    }

    private static Producer<Object, Object> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, System.getenv("BOOTSTRAP_SERVERS"));
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "KafkaMeetupProducer");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put("schema.registry.url",  System.getenv("SCHEMA_REGISTRY_URL"));
        return new KafkaProducer<>(props);
    }

    /*
     * This method calls the filtered stream endpoint and streams Tweets from it
     * */
    private static void connectStream(String bearerToken, Producer<Object, Object> producer) throws IOException, URISyntaxException, ExecutionException, InterruptedException {

        String twitter = "{\"type\":\"record\"," +
                "\"name\":\"nba_tweet\"," +
                "\"fields\":[" +
                    "{\"name\":\"id\",\"type\":\"string\"}," +
                    "{\"name\":\"text\",\"type\":\"string\"}," +
                    "{\"name\":\"players\",\"type\": {\"type\":\"array\",\"items\":\"string\"}}" +
                "]}";

        Schema.Parser parser = new Schema.Parser();
        Schema schema = parser.parse(twitter);

        HttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setCookieSpec(CookieSpecs.STANDARD).build())
                .build();

        URIBuilder uriBuilder = new URIBuilder("https://api.twitter.com/2/tweets/search/stream");
        uriBuilder.setParameter("tweet.fields", "context_annotations");

        HttpGet httpGet = new HttpGet(uriBuilder.build());
        httpGet.setHeader("Authorization", String.format("Bearer %s", bearerToken));

        HttpResponse response = httpClient.execute(httpGet);
        HttpEntity entity = response.getEntity();
        if (null != entity) {
            BufferedReader reader = new BufferedReader(new InputStreamReader((entity.getContent())));
            String line = reader.readLine();
            while (line != null) {
                try {
                    System.out.println(line);
                    JsonObject jsonObject = JsonParser.parseString(line).getAsJsonObject();
                    GenericRecord avroRecord = new GenericData.Record(schema);
                    avroRecord.put("id", jsonObject.get("data").getAsJsonObject().get("id").toString());
                    avroRecord.put("text", jsonObject.get("data").getAsJsonObject().get("text").toString());
                    List<String> list = new ArrayList<>();
                    for (JsonElement annotations : jsonObject.get("data").getAsJsonObject().get("context_annotations").getAsJsonArray()) {
                        if (annotations.getAsJsonObject().get("domain").getAsJsonObject().get("id").toString().replaceAll("^\"|\"$", "").equals("60")) {
                            list.add(annotations.getAsJsonObject().get("entity").getAsJsonObject().get("name").toString());
                        }
                    }
                    avroRecord.put("players", list);
                    ProducerRecord<Object, Object> record = new ProducerRecord<Object, Object>(TOPIC, jsonObject.get("data").getAsJsonObject().get("id").toString(), avroRecord);
                    RecordMetadata metadata = (RecordMetadata) producer.send(record).get();
                    System.out.printf("sent record(key=%s value=%s) meta(partition=%d, offset=%d) \n",
                            record.key(), record.value(), metadata.partition(), metadata.offset());
                }
                catch (IllegalStateException e) {}
                line = reader.readLine();
            }
        }
    }

    /*
     * Helper method to setup rules before streaming data
     * */
    private static void setupRules(String bearerToken, Map<String, String> rules) throws IOException, URISyntaxException {
        List<String> existingRules = getRules(bearerToken);
        if (existingRules.size() > 0) {
            deleteRules(bearerToken, existingRules);
        }
        createRules(bearerToken, rules);
    }

    /*
     * Helper method to create rules for filtering
     * */
    private static void createRules(String bearerToken, Map<String, String> rules) throws URISyntaxException, IOException {
        HttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setCookieSpec(CookieSpecs.STANDARD).build())
                .build();

        URIBuilder uriBuilder = new URIBuilder("https://api.twitter.com/2/tweets/search/stream/rules");

        HttpPost httpPost = new HttpPost(uriBuilder.build());
        httpPost.setHeader("Authorization", String.format("Bearer %s", bearerToken));
        httpPost.setHeader("content-type", "application/json");
        StringEntity body = new StringEntity(getFormattedString("{\"add\": [%s]}", rules));
        httpPost.setEntity(body);
        HttpResponse response = httpClient.execute(httpPost);
        HttpEntity entity = response.getEntity();
        if (null != entity) {
            System.out.println(EntityUtils.toString(entity, "UTF-8"));
        }
    }

    /*
     * Helper method to get existing rules
     * */
    private static List<String> getRules(String bearerToken) throws URISyntaxException, IOException {
        List<String> rules = new ArrayList<>();
        HttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setCookieSpec(CookieSpecs.STANDARD).build())
                .build();

        URIBuilder uriBuilder = new URIBuilder("https://api.twitter.com/2/tweets/search/stream/rules");

        HttpGet httpGet = new HttpGet(uriBuilder.build());
        httpGet.setHeader("Authorization", String.format("Bearer %s", bearerToken));
        httpGet.setHeader("content-type", "application/json");
        HttpResponse response = httpClient.execute(httpGet);
        HttpEntity entity = response.getEntity();
        if (null != entity) {
            JSONObject json = new JSONObject(EntityUtils.toString(entity, "UTF-8"));
            if (json.length() > 1) {
                JSONArray array = (JSONArray) json.get("data");
                for (int i = 0; i < array.length(); i++) {
                    JSONObject jsonObject = (JSONObject) array.get(i);
                    rules.add(jsonObject.getString("id"));
                }
            }
        }
        return rules;
    }

    /*
     * Helper method to delete rules
     * */
    private static void deleteRules(String bearerToken, List<String> existingRules) throws URISyntaxException, IOException {
        HttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setCookieSpec(CookieSpecs.STANDARD).build())
                .build();

        URIBuilder uriBuilder = new URIBuilder("https://api.twitter.com/2/tweets/search/stream/rules");

        HttpPost httpPost = new HttpPost(uriBuilder.build());
        httpPost.setHeader("Authorization", String.format("Bearer %s", bearerToken));
        httpPost.setHeader("content-type", "application/json");
        StringEntity body = new StringEntity(getFormattedString("{ \"delete\": { \"ids\": [%s]}}", existingRules));
        httpPost.setEntity(body);
        HttpResponse response = httpClient.execute(httpPost);
        HttpEntity entity = response.getEntity();
        if (null != entity) {
            System.out.println(EntityUtils.toString(entity, "UTF-8"));
        }
    }

    private static String getFormattedString(String string, List<String> ids) {
        StringBuilder sb = new StringBuilder();
        if (ids.size() == 1) {
            return String.format(string, "\"" + ids.get(0) + "\"");
        } else {
            for (String id : ids) {
                sb.append("\"" + id + "\"" + ",");
            }
            String result = sb.toString();
            return String.format(string, result.substring(0, result.length() - 1));
        }
    }

    private static String getFormattedString(String string, Map<String, String> rules) {
        StringBuilder sb = new StringBuilder();
        if (rules.size() == 1) {
            String key = rules.keySet().iterator().next();
            return String.format(string, "{\"value\": \"" + key + "\", \"tag\": \"" + rules.get(key) + "\"}");
        } else {
            for (Map.Entry<String, String> entry : rules.entrySet()) {
                String value = entry.getKey();
                String tag = entry.getValue();
                sb.append("{\"value\": \"" + value + "\", \"tag\": \"" + tag + "\"}" + ",");
            }
            String result = sb.toString();
            return String.format(string, result.substring(0, result.length() - 1));
        }
    }

}
