package cn.wjwagent.ai.CopyOnWriteArrayListTest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 优化版：CopyOnWriteArrayList 与 SynchronizedList 并发性能对比
 */
@RestController
@Slf4j
public class ConcurrentOptimizedTest {

    // 常量定义，避免魔法值
    private static final int WRITE_TIMES = 50000;
    private static final int READ_TIMES = 500000;
    private static final int DATA_SIZE = 500000;
    // 固定线程池，控制并发度，避免公共线程池干扰
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    /**
     * 优化：独立测试写性能，预热 + 独立环境 + 多次执行
     */
    @GetMapping("optimized/write")
    public Map<String, Object> testOptimizedWrite() {
        // 预热 JVM
        warmUp();

        // 测试 CopyOnWriteArrayList
        List<Integer> cowa = new CopyOnWriteArrayList<>();
        StopWatch sw1 = new StopWatch();
        sw1.start("CopyOnWriteArrayList-写");
        executeConcurrentWrite(cowa);
        sw1.stop();

        // 测试 SynchronizedList
        List<Integer> sl = Collections.synchronizedList(new ArrayList<>());
        StopWatch sw2 = new StopWatch();
        sw2.start("SynchronizedList-写");
        executeConcurrentWrite(sl);
        sw2.stop();

        log.info("===== 写性能测试结果 =====");
        log.info(sw1.prettyPrint());
        log.info(sw2.prettyPrint());

        Map<String, Object> result = new HashMap<>();
        result.put("cowa_write_size", cowa.size());
        result.put("cowa_write_time_ms", sw1.getTotalTimeMillis());
        result.put("sl_write_size", sl.size());
        result.put("sl_write_time_ms", sw2.getTotalTimeMillis());
        return result;
    }

    /**
     * 优化：独立测试读性能
     */
    @GetMapping("optimized/read")
    public Map<String, Object> testOptimizedRead() {
        warmUp();

        // 初始化数据
        List<Integer> cowa = new CopyOnWriteArrayList<>();
        List<Integer> sl = Collections.synchronizedList(new ArrayList<>());
        initData(cowa);
        initData(sl);

        // 读测试
        StopWatch sw1 = new StopWatch();
        sw1.start("CopyOnWriteArrayList-读");
        executeConcurrentRead(cowa);
        sw1.stop();

        StopWatch sw2 = new StopWatch();
        sw2.start("SynchronizedList-读");
        executeConcurrentRead(sl);
        sw2.stop();

        log.info("===== 读性能测试结果 =====");
        log.info(sw1.prettyPrint());
        log.info(sw2.prettyPrint());

        Map<String, Object> result = new HashMap<>();
        result.put("cowa_read_time_ms", sw1.getTotalTimeMillis());
        result.put("sl_read_time_ms", sw2.getTotalTimeMillis());
        return result;
    }

    // JVM 预热：消除编译、加载影响
    private void warmUp() {
        List<Integer> temp = new ArrayList<>();
        IntStream.range(0, 10000).parallel().forEach(i -> temp.add(i));
        temp.clear();
        log.info("JVM 预热完成");
    }

    // 统一写逻辑
    private void executeConcurrentWrite(List<Integer> list) {
        IntStream.range(0, WRITE_TIMES).parallel().forEach(
                i -> list.add(ThreadLocalRandom.current().nextInt(WRITE_TIMES))
        );
    }

    // 统一读逻辑
    private void executeConcurrentRead(List<Integer> list) {
        int size = list.size();
        IntStream.range(0, READ_TIMES).parallel().forEach(
                i -> list.get(ThreadLocalRandom.current().nextInt(size))
        );
    }

    // 初始化数据
    private void initData(List<Integer> list) {
        list.addAll(IntStream.rangeClosed(1, DATA_SIZE).boxed().collect(Collectors.toList()));
    }
}