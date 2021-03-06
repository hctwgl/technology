package clonegod.kafka.client.consumer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import clonegod.kafka.client.conf.KafkaProperties;
import kafka.utils.ShutdownableThread;

/**
 * 手动批量提交消息
 *	- 即 消费端在成功处理消息后，才向broker发送消费确认的消息。
 */
public class ConsumerManualCommit extends ShutdownableThread {

    private final KafkaConsumer<Integer, String> consumer;
    private final String topic;
    List<ConsumerRecord<Integer,String>> recordList = new ArrayList<>();

    public ConsumerManualCommit(String topic) {
        super("KafkaConsumerExample", false);
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaProperties.KAFKA_SERVER_URL_LIST);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "DemoConsumer1");
        
        /** 关闭自动提交消息offset */
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        
        // 自动提交的时间间隔
        // props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); // or latest
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.IntegerDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");

        consumer = new KafkaConsumer<>(props);
        this.topic = topic;
    }

    @Override
    public void doWork() {
        consumer.subscribe(Collections.singletonList(this.topic));
        ConsumerRecords<Integer, String> records = consumer.poll(1000);
        for (ConsumerRecord<Integer, String> record : records) {
            System.out.println("["+ record.partition() +"]" + 
            			"Received message: (" + record.key() + ", " + record.value() + ") at offset " + record.offset());
            recordList.add(record);
        }
        
        // 手动批量提交
        // 注意：消费端每次拉取到的消息个数是不确定的
        if(recordList.size() >= 5) {
        	System.out.println("Start Commit Message Offset");
        	// 1.同步提交
            //consumer.commitSync();
            // 2.异步提交
            consumer.commitAsync();
            recordList.clear();
        }
    }

    @Override
    public String name() {
        return null;
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }
    
	public static void main(String[] args) {
		ConsumerManualCommit consumerThread = new ConsumerManualCommit(KafkaProperties.TOPIC);
		consumerThread.start();

	}

}
