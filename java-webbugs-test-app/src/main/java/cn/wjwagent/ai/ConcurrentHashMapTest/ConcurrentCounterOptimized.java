package cn.wjwagent.ai.ConcurrentHashMapTest;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * 优化后的并发计数代码 - 保留耗时统计，方便对比
 */
public class ConcurrentCounterOptimized {
    // 循环次数
    private static int LOOP_COUNT = 10000000;
    // 线程数量
    private static int THREAD_COUNT = 10;
    // 元素数量
    private static int ITEM_COUNT = 10;

    private Map<String, Long> optimizedUse() throws InterruptedException {
        // 记录方法开始时间（毫秒）
        long startTime = System.currentTimeMillis();

        ConcurrentHashMap<String, Long> freqs = new ConcurrentHashMap<>(ITEM_COUNT);
        ForkJoinPool forkJoinPool = new ForkJoinPool(THREAD_COUNT);

        try {
            forkJoinPool.execute(() -> IntStream.rangeClosed(1, LOOP_COUNT)
                    .parallel()
                    .forEach(i -> {
                        // 获得一个随机的Key
                        String key = "item" + ThreadLocalRandom.current().nextInt(ITEM_COUNT);
                        // 核心优化：使用ConcurrentHashMap原子方法替代全局synchronized锁
                        // compute方法本身是线程安全的，且锁粒度仅针对单个key
                        freqs.compute(key, (k, v) -> v == null ? 1L : v + 1);
                    }));
        } finally {
            // 确保线程池一定会关闭，避免资源泄漏
            forkJoinPool.shutdown();
            forkJoinPool.awaitTermination(1, TimeUnit.HOURS);
        }

        // 计算总耗时并打印
        long endTime = System.currentTimeMillis();
        long costTime = endTime - startTime;
        System.out.println("=== 优化后代码执行耗时统计 ===");
        System.out.println("循环次数：" + LOOP_COUNT);
        System.out.println("线程数量：" + THREAD_COUNT);
        System.out.println("总耗时：" + costTime + " 毫秒（" + (costTime / 1000.0) + " 秒）");

        return freqs;
    }

    // 主方法：程序入口，可直接运行
    public static void main(String[] args) throws InterruptedException {
        ConcurrentCounterOptimized counter = new ConcurrentCounterOptimized();
        Map<String, Long> result = counter.optimizedUse();

        // 打印统计结果并校验总数（验证计数准确性）
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