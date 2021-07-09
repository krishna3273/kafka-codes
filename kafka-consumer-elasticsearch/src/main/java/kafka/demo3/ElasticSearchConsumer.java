package kafka.demo3;

import com.google.gson.JsonParser;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class ElasticSearchConsumer {

    public static KafkaConsumer<String,String> createConsumer(String topic){
        String bootstrapServers="127.0.0.1:9092";
        String group_id="elasticsearch-demo";
        Properties properties=new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,bootstrapServers);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG,group_id);
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"earliest");
        properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,"false");
        properties.setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG,"100");

        KafkaConsumer<String,String> consumer=new KafkaConsumer<String, String>(properties);

//        consumer.subscribe(Collections.singleton(topic));
        consumer.subscribe(Arrays.asList(topic));

        return  consumer;
    }

    public static RestHighLevelClient createClient(){

        /////////// FOR  LOCAL ELASTICSEARCH
        //  String hostname = "localhost";
        //  RestClientBuilder builder = RestClient.builder(new HttpHost(hostname,9200,"http"));
        /////////// FOR  HOSTED ELASTICSEARCH
        String hostname = "kafka-demo-9342230194.ap-southeast-2.bonsaisearch.net"; // localhost or bonsai url
        String username = "yu1x6uyyio"; // needed only for bonsai
        String password = "t5ro30svuf"; // needed only for bonsai

        // credentials provider help supply username and password
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(username, password));

        RestClientBuilder builder = RestClient.builder(
                new HttpHost(hostname,443,"https"))
                .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                    @Override
                    public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                        return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    }
                });

        RestHighLevelClient client = new RestHighLevelClient(builder);
        return client;
    }
    public static void main(String[] args) throws IOException {
        Logger logger= LoggerFactory.getLogger(ElasticSearchConsumer.class.getName());
        RestHighLevelClient client=createClient();


        KafkaConsumer<String,String> consumer=createConsumer("twitter-tweets");
        while(true){
            ConsumerRecords<String,String> records=consumer.poll(Duration.ofMillis(10000));
            int recordCount=records.count();
            logger.info("Recieved "+recordCount+" records");
            BulkRequest bulkRequest=new BulkRequest();
            for(ConsumerRecord<String,String> record:records){
                // 1st method(generic,works for any stream of data)
//                String id=record.topic()+"_"+record.partition()+"_"+record.offset();
                //2nd method(stream specific)
                try {
                    String id = extractIdFromTweet(record.value());
                    IndexRequest request = new IndexRequest("twitter-tweets")
                            .id(id)
                            .source(record.value(), XContentType.JSON);
                    bulkRequest.add(request);
                }catch (NullPointerException e){
                    logger.warn("Bad dat without id: "+record.value());
                }
            }
            if(recordCount>0) {
                BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
                logger.info("Commiting the offsets......");
                consumer.commitSync();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                logger.info("Offsets have been committed");
            }
        }
//        client.close();
    }
    private static String extractIdFromTweet(String value) {
        return JsonParser.parseString(value).getAsJsonObject().get("id_str").getAsString();
    }
}
