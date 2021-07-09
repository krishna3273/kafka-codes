package kafka.demo4;

import com.google.gson.JsonParser;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;

import java.util.Properties;

public class FilterTweets {
    public static void main(String[] args) {
        Properties properties=new Properties();
        properties.setProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,"127.0.0.1:9092");
        properties.setProperty(StreamsConfig.APPLICATION_ID_CONFIG,"kafka-streams-demo");
        properties.setProperty(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class.getName());
        properties.setProperty(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,Serdes.StringSerde.class.getName());

        StreamsBuilder streamsBuilder=new StreamsBuilder();
        KStream<String,String> inputTopic=streamsBuilder.stream("twitter-tweets");
        KStream<String,String> filteredStream=inputTopic.filter(
                (k,tweet)->{
                    try {
                        int num_followers=JsonParser.parseString(tweet)
                                .getAsJsonObject()
                                .get("user")
                                .getAsJsonObject()
                                .get("followers_count")
                                .getAsInt();
                        return num_followers>1000;
                    }catch (NullPointerException e){
                        e.printStackTrace();
                        return false;
                    }
                }
        );
        filteredStream.to("important-tweets");

        KafkaStreams kafkaStreams=new KafkaStreams(streamsBuilder.build(),properties);

        kafkaStreams.start();
    }
}
