package cn.wjwagent.ai.ConcurrentHashMapTest;


import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

@RestController
@Slf4j
public class ConcurrentHashMapWrongController {

    // 线程个数
    private static int THREAD_COUNT = 10;
    // 总元素数量
    private static int ITEM_COUNT = 1000;

    // 帮助方法：生成指定数量的 ConcurrentHashMap 模拟数据
    private ConcurrentHashMap<String, Long> getData(int count) {
        // 边界处理：避免传入负数导致异常
        if (count <= 0) {
            return new ConcurrentHashMap<>();
        }
        return LongStream.rangeClosed(1, count)
                .boxed()
                .collect(Collectors.toConcurrentMap(
                        i -> UUID.randomUUID().toString(), // Key：唯一UUID
                        Function.identity(), // Value：数字本身
                        (o1, o2) -> o1, // Key冲突时保留第一个（此处UUID唯一，不会触发）
                        ConcurrentHashMap::new // 指定结果容器为 ConcurrentHashMap
                ));
    }

    @GetMapping("wrong/concurrenthashmap")
    public String wrong() throws InterruptedException {
        // 初始化：先放入 900 个元素（1000-100）
        ConcurrentHashMap<String, Long> concurrentHashMap = getData(ITEM_COUNT - 100);
        // 打印初始大小（预期：900）
        log.info("init size:{}", concurrentHashMap.size());

        // 创建包含 10 个线程的 ForkJoinPool 线程池
        ForkJoinPool forkJoinPool = new ForkJoinPool(THREAD_COUNT);

        // 提交并发任务：10个并行任务补充元素
        forkJoinPool.execute(() -> IntStream.rangeClosed(1, 10).parallel().forEach(i -> {
            // 计算需要补充的元素数量（非原子操作，多线程会重复计算）
            int gap = ITEM_COUNT - concurrentHashMap.size();
            log.info("线程{} - gap size:{}", Thread.currentThread().getName(), gap);
            // 补充 gap 个元素到 Map 中
            concurrentHashMap.putAll(getData(gap));
        }));

        // 等待所有任务完成（最多等1小时，实际几秒就完成）
        forkJoinPool.shutdown();
        boolean isTerminated = forkJoinPool.awaitTermination(1, TimeUnit.HOURS);
        log.info("线程池任务是否完成：{}", isTerminated);

        // 打印最终元素数量（预期：远大于1000）
        log.info("finish size:{}", concurrentHashMap.size());
        return "OK";
    }


    @GetMapping("correct/concurrenthashmap")
    public String correct() throws InterruptedException {
        ConcurrentHashMap<String, Long> concurrentHashMap = getData(ITEM_COUNT - 100);
        log.info("init size:{}", concurrentHashMap.size());

        ForkJoinPool forkJoinPool = new ForkJoinPool(THREAD_COUNT);
        forkJoinPool.execute(() -> IntStream.rangeClosed(1, 10).parallel().forEach(i -> {
            // 加锁保证「计算缺口 + 补充元素」是原子操作
            synchronized (concurrentHashMap) {
                int gap = ITEM_COUNT - concurrentHashMap.size();
                log.info("线程{} - gap size:{}", Thread.currentThread().getName(), gap);
                if (gap > 0) {
                    concurrentHashMap.putAll(getData(gap));
                }
            }
        }));

        forkJoinPool.shutdown();
        forkJoinPool.awaitTermination(1, TimeUnit.HOURS);
        log.info("finish size:{}", concurrentHashMap.size()); // 最终稳定为1000
        return "OK";
    }

}
