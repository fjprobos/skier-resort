import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import entity.LiftRide;
import entity.Season;
import entity.Year;

public class ResortConsumer {
    private static Map<String, Season> map = new ConcurrentHashMap<>();
    private static int parseInt(String s) {
        return Integer.parseInt(s);
    }


    private static void retrieveYearFromServer() {
        ConnectionFactory factory = new ConnectionFactory();
//        factory.setHost("localhost");
        factory.setHost("54.225.153.29");
        factory.setUsername("test");
        factory.setPassword("test");
        factory.setPort(5672);
        try {
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            channel.queueDeclare("lab8", false, false, false, null);
            channel.basicConsume("lab8", true, (consumerTag, m) -> {
                final String[] message = new String(m.getBody(), "UTF-8").split("/");
                Year newYear = new Year(parseInt(message[0]),
                        parseInt(message[1]));
                ResortDao resortDao = new ResortDao();
                resortDao.createNewSeason(newYear);
                System.out.println("stored data to database!");
            }, consumerTag -> {
            });
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    public static void main (String[] args) {
        int numThreads = 10;
        if (args.length != 0) {
            numThreads = Integer.parseInt(args[0]);
        }
        for (int i = 0; i < numThreads; i++) {
            Runnable thread = () -> {
                retrieveYearFromServer();
//                retrieveMessageFromServer();
            };
            new Thread(thread).start();
        }
//        LiftRideDao liftRideDao = new LiftRideDao();
//        liftRideDao.createLiftRide(new LiftRide(10, 2, 3, 5, 500, 20));
    }
}
