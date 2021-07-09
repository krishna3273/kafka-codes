package kafka.demo1;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class ConsumerDemoAssignSeek {
    public static void main(String[] args) {
        String bootstrapServers="127.0.0.1:9092";
        String topic="first-topic";
        Logger logger= LoggerFactory.getLogger(ConsumerDemoAssignSeek.class);
        Properties properties=new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,bootstrapServers);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"earliest");

        KafkaConsumer<String,String> consumer=new KafkaConsumer<String, String>(properties);

        TopicPartition partitionToRead=new TopicPartition(topic,0);
        consumer.assign(Arrays.asList(partitionToRead));
        long offset=20;
        consumer.seek(partitionToRead,offset);

        int num_messages=15;
        boolean continueReading=true;
        while(continueReading){
            ConsumerRecords<String,String> records=consumer.poll(Duration.ofMillis(10000));
            for(ConsumerRecord<String,String> record:records){
                num_messages-=1;
                logger.info("Key:"+record.key()+", value:"+record.value()+"\n"+
                        "Partition:"+record.partition()+" offset:"+record.offset()+"\n");
                if(num_messages==0){
                    continueReading=false;
                    break;
                }
            }
        }

    }
}
