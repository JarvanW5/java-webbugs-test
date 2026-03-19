package cn.wjwagent.ai.ConcurrentHashMapTest;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * 原始代码（仅新增耗时统计）- 可直接运行
 */
public class ConcurrentCounterOriginal {
    // 循环次数
    private static int LOOP_COUNT = 10000000;
    // 线程数量
    private static int THREAD_COUNT = 10;
    // 元素数量
    private static int ITEM_COUNT = 10;

    private Map<String, Long> normaluse() throws InterruptedException {
        // 记录方法开始时间（毫秒）
        long startTime = System.currentTimeMillis();

        ConcurrentHashMap<String, Long> freqs = new ConcurrentHashMap<>(ITEM_COUNT);
        ForkJoinPool forkJoinPool = new ForkJoinPool(THREAD_COUNT);

        forkJoinPool.execute(() -> IntStream.rangeClosed(1, LOOP_COUNT).parallel().forEach(i -> {
            // 获得一个随机的Key
            String key = "item" + ThreadLocalRandom.current().nextInt(ITEM_COUNT);
            synchronized (freqs) {
                if (freqs.containsKey(key)) {
                    // Key存在则+1
                    freqs.put(key, freqs.get(key) + 1);
                } else {
                    // Key不存在则初始化为1
                    freqs.put(key, 1L);
                }
            }
        }));

        forkJoinPool.shutdown();
        forkJoinPool.awaitTermination(1, TimeUnit.HOURS);

        // 计算总耗时并打印
        long endTime = System.currentTimeMillis();
        long costTime = endTime - startTime;
        System.out.println("=== 原始代码执行耗时统计 ===");
        System.out.println("循环次数：" + LOOP_COUNT);
        System.out.println("线程数量：" + THREAD_COUNT);
        System.out.println("总耗时：" + costTime + " 毫秒（" + (costTime / 1000.0) + " 秒）");

        return freqs;
    }

    // 主方法：程序入口，可直接运行
    public static void main(String[] args) throws InterruptedException {
        ConcurrentCounterOriginal counter = new ConcurrentCounterOriginal();
        Map<String, Long> result = counter.normaluse();

        // 打印统计结果并校验总数（验证计数是否准确）
        System.out.println("\n=== 统计结果 ===");
        long total = 0;
        for (Map.Entry<String, Long> entry : result.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
            total += entry.getValue();
        }
        System.out.println("统计总数：" + total);
        System.out.println("预期总数：" + LOOP_COUNT);
        System.out.println("计数是否准确：" + (total == LOOP_COUNT));
    }
}