import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.nio.file.Files;
import java.nio.file.Paths;


class Message {
    public String data;
    public long startTime;
    public int priority;

    public Message(String data, long startTime, int priority) {
        this.data = data;
        this.priority = priority;
        this.startTime = startTime;
        long timeToLive = 7 * 1000;    // Ms
        Timer timer = new Timer(true);

        // Schedule a task to remove the object when it expires
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                removeSelf();
            }
        }, timeToLive);
    }

    private void removeSelf() {
        ConcurrentQueue.removeMessage(this);
    }
}


class StatsResult {
    private int messageCount;
    private int totalMessageLength;

    StatsResult(int messageCount, int totalMessageLength) {
        this.messageCount = messageCount;
        this.totalMessageLength = totalMessageLength;
    }

    public void update(Message message, boolean increase) {
        if (increase) {
            this.messageCount++;
            this.totalMessageLength += message.data.length();
        } else {
            this.messageCount--;
            this.totalMessageLength -= message.data.length();
        }
    }

    public int getMessageCount() {
        return this.messageCount;
    }

    public int getTotalMessageLength() {
        return this.totalMessageLength;
    }

    public long getTotalProgramMemoryUsage() {
        return Runtime.getRuntime().totalMemory();
    }
}


class ConcurrentQueue {
    public static LinkedList<Message> queue = new LinkedList<>();
    private static Semaphore producer_lock;
    private static Semaphore consumer_lock1;
    private static Semaphore consumer_lock2;
    public static StatsResult stats;
    private int thread_counter;
    private int max_size;
    private int max_bulk;

    public ConcurrentQueue() {
        queue = new LinkedList<>();
        producer_lock = new Semaphore(1);
        consumer_lock1 = new Semaphore(0);
        consumer_lock2 = new Semaphore(1);
        stats = new StatsResult(0, 0);
        thread_counter = 0;
        max_size = Integer.MAX_VALUE;
        max_bulk = Integer.MAX_VALUE;
    }

    public ConcurrentQueue(int max_size) {
        this();
        this.max_size = max_size;
    }

    public ConcurrentQueue(LinkedList<Message> queue) {
        this();
        ConcurrentQueue.queue = queue;
    }

    public ConcurrentQueue(int max_size, int max_bulk) {
        this();
        this.max_size = max_size;
        this.max_bulk = max_bulk;
    }

    public static void removeMessage(Message message) {
        try {
            producer_lock.acquire();
            consumer_lock1.acquire();
            consumer_lock2.acquire();
            queue.remove(message);
            stats.update(message, false);
            consumer_lock2.release();
            consumer_lock1.release();
            producer_lock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void send_msg(Message message) {
        try {
            producer_lock.acquire();
            if (max_size != Integer.MAX_VALUE && queue.size() == max_size) {
                producer_lock.release();
            } else if (max_bulk != Integer.MAX_VALUE && (stats.getTotalMessageLength() + message.data.length() > max_bulk)) {
                producer_lock.release();
            } else {
                if (queue.size() == 0) {
                    queue.add(message);
                } else {
                    queue.add(message.priority % queue.size(), message);
                }
                stats.update(message, true);
                thread_counter++;
                consumer_lock1.release();
                producer_lock.release();
            }
        } catch (InterruptedException e) {
            System.out.println("exception in send_msg : " + e);
        }
    }

    public String get_msg() {
        try {
            consumer_lock2.acquire();
            if (thread_counter < 1) {
                System.out.println("I'm " + Thread.currentThread().getName() + " and waiting for a message !");
                consumer_lock1.acquire();
                thread_counter--;
            }
            System.out.println("I'm " + Thread.currentThread().getName() + " and receive a message !");
            Message message = queue.removeLast();
            stats.update(message, false);
            consumer_lock2.release();
            return message.data;

        } catch (InterruptedException e) {
            System.out.println("exception in get_msg catch 1 : " + e);
        }
        return null;
    }

    public String get_msg_nb() {
        if (!queue.isEmpty()) {
            return queue.removeLast().data;
        } else {
            return null;
        }
    }

    public Map<String, Object> stats() {
        try {
            consumer_lock1.acquire();
            consumer_lock2.acquire();
            producer_lock.acquire();
            Map<String, Object> statsMap = new HashMap<>();
            statsMap.put("message_count", stats.getMessageCount());
            statsMap.put("total_message_length", stats.getTotalMessageLength());
            statsMap.put("memory_usage", stats.getTotalProgramMemoryUsage() / (1024 * 1024) + " MB");
            producer_lock.release();
            consumer_lock2.release();
            consumer_lock1.release();
            return statsMap;
        } catch (InterruptedException e) {
            System.out.println("exception in 197 : " + e);
            return null;
        }
    }
}


class Producer_Consumer {
    public static void main(String[] args) throws InterruptedException {
        if (Files.exists(Paths.get("Data.txt"))) {
            try (BufferedReader reader = new BufferedReader(new FileReader("Data.txt"))) {
                String line;
                LinkedList<Message> queue = new LinkedList<>();
                while ((line = reader.readLine()) != null) {
                    // Assuming the format in the file is the same as MyObject.toString()
                    String[] parts = line.split(",");
                    String data = parts[0];
                    // start time because if I want to continue the thread exactly ...
                    // can write in second data
                    long startTime = Long.parseLong(parts[1]);
                    int priority = Integer.parseInt(parts[2]);
                    queue.add(new Message(data, System.currentTimeMillis(), priority));
                }
                ConcurrentQueue concurrentQueue = new ConcurrentQueue(queue);
                System.out.println("Linked list read from file successfully.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            // when any data does not exist
            ConcurrentQueue concurrentQueue = new ConcurrentQueue(3);
            Thread producer1 = new Thread(() -> concurrentQueue.send_msg(new Message("Message 1", System.currentTimeMillis(), 1)), "producer1");
            Thread producer2 = new Thread(() -> concurrentQueue.send_msg(new Message("Message 2", System.currentTimeMillis(), 0)), "producer2");
            Thread producer3 = new Thread(() -> concurrentQueue.send_msg(new Message("Message 3", System.currentTimeMillis(), 0)), "producer3");
            Thread producer4 = new Thread(() -> concurrentQueue.send_msg(new Message("Message 4", System.currentTimeMillis(), 0)), "producer4");
            Thread producer5 = new Thread(() -> concurrentQueue.send_msg(new Message("Message 5", System.currentTimeMillis(), 0)), "producer5");
            Thread producer6 = new Thread(() -> concurrentQueue.send_msg(new Message("Message 6", System.currentTimeMillis(), 0)), "producer6");

            Thread consumer1 = new Thread(() -> {
                String message = concurrentQueue.get_msg();
                System.out.println(Thread.currentThread().getName() + " received this message: " + message);
            }, "consumer1");
            Thread consumer2 = new Thread(() -> {
                String message = concurrentQueue.get_msg();
                System.out.println(Thread.currentThread().getName() + " received this message: " + message);
            }, "consumer2");
            Thread consumer3 = new Thread(() -> {
                String message = concurrentQueue.get_msg();
                System.out.println(Thread.currentThread().getName() + " received this message: " + message);
            }, "consumer3");



            producer1.start();
            TimeUnit.SECONDS.sleep(2);
//            producer2.start();
//            TimeUnit.SECONDS.sleep(1);
//            producer3.start();
//            TimeUnit.SECONDS.sleep(1);
//            System.out.println(concurrentQueue.stats());
//            producer4.start();
//            TimeUnit.SECONDS.sleep(1);
//            System.out.println(concurrentQueue.stats());
//            System.out.println("--------------------");
//            TimeUnit.SECONDS.sleep(2);
            consumer1.start();
//            producer4.start();
            consumer2.start();
//            producer5.start();
//            consumer3.start();
//            producer6.start();



            // Start the threads
//            producer1.start();
//            TimeUnit.SECONDS.sleep(1);
//            producer2.start();
//            TimeUnit.SECONDS.sleep(1);
//            producer3.start();
//            TimeUnit.SECONDS.sleep(1);
//            producer4.start();
//            TimeUnit.SECONDS.sleep(1);
//            producer5.start();
//            TimeUnit.SECONDS.sleep(1);
//            System.out.println(concurrentQueue.stats());
//            System.out.println("--------------------");
//            TimeUnit.SECONDS.sleep(4);
//            System.out.println(concurrentQueue.stats());
//        consumer1.start();
////        TimeUnit.SECONDS.sleep(1);
//        producer4.start();
//        consumer2.start();
////        TimeUnit.SECONDS.sleep(1);
//        producer5.start();
//        consumer3.start();
////        TimeUnit.SECONDS.sleep(1);
//        producer6.start();
//
//        // Wait for all threads to finish
        try {
            producer1.join();
            producer2.join();
            producer3.join();
            producer4.join();
            producer5.join();
            producer6.join();
            consumer1.join();
            consumer2.join();
            consumer3.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        }
        writeToDisk();
    }

    public static void writeToDisk() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("Data.txt"))) {
            LinkedList<Message> messages = ConcurrentQueue.queue;
            for (Message msg : messages) {
                writer.write(msg.data);
                writer.write(",");
                writer.write(String.valueOf(msg.startTime));
                writer.write(",");
                writer.write(String.valueOf(msg.priority));
                writer.newLine();
            }
            System.out.println("Linked list written to file successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
