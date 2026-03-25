package cn.wjwagent.ai.SynchronizedTest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SynchronizedTest {

    volatile int a = 1;
    volatile int b = 1;

    public void add() {
        log.info("add start");
        for (int i = 0; i < 10000; i++) {
            a++; // 先加 a
            // 这里空转一小段时间，强行制造 a > b 的瞬间
            for (int k = 0; k < 1000; k++) {}
            b++; // 后加 b
        }
        log.info("add done");
    }

    public void compare() {
        log.info("compare start");
        // 无限循环，一直监控，一定能抓到
        while (true) {
            if (a != b) {
                log.info("发现不一致：a:{}, b:{}", a, b);
            }
        }
    }

    public static void main(String[] args) {
        SynchronizedTest test = new SynchronizedTest();
        new Thread(test::add, "线程-add").start();
        new Thread(test::compare, "线程-compare").start();
    }
}