//import java.util.Optional;
//import java.util.concurrent.Semaphore;
//
//class StatsResult {
//    private int messageCount;
//    private int totalMessageLength;
//    private int totalProgramMemoryUsage;
//
//    StatsResult(int messageCount, int totalMessageLength, int totalProgramMemoryUsage) {
//        this.messageCount = messageCount;
//        this.totalMessageLength = totalMessageLength;
//        this.totalProgramMemoryUsage = totalProgramMemoryUsage;
//    }
//
//    public int getMessageCount() {
//        return this.messageCount;
//    }
//
//    public int getTotalMessageLength() {
//        return this.totalMessageLength;
//    }
//
//    public int getTotalProgramMemoryUsage() {
//        return this.totalProgramMemoryUsage;
//    }
//}
//
//class Queue {
//
//    public String get_msg() {
//        return "new message";
//    }
//
//    public Optional<String> get_msg_nb() {
//        if(1==1) { // value exists in queue
//            return Optional.of("new message exists");
//        } else { // value not exists in queue
//            return Optional.empty();
//        }
//    }
//
//    public boolean send_msg(String msg) {
//        return true; // success
//    }
//
//    public StatsResult stats() {
//        return new StatsResult(10, 11, 12);
//    }
//}
//
//public class Main {
//    public static void main(String[] args) {
//        // declare new queue;
//        Queue q = new Queue();
//
//        // send message to queue
//        q.send_msg("a");
//        q.send_msg("b");
//        q.send_msg("c");
//
//        // get oldest message in queue
//        System.out.println(q.get_msg());
//
//        // get message without blocking
//        Optional<String> result = q.get_msg_nb();
//        if(result.isPresent()) {
//            System.out.println(result.get());
//        } else {
//            System.out.println("no item in queue");
//        }
//
//        // get queue stats
//        StatsResult stat = q.stats();
//        System.out.println(stat.getMessageCount());
//        System.out.println(stat.getTotalMessageLength());
//    }
//}
