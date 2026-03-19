package cn.wjwagent.ai.ThreadLocalTest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class ThreadLocalTestController {

    // 定义 ThreadLocal 变量，初始值为 null
    private static final ThreadLocal<Integer> currentUser = ThreadLocal.withInitial(() -> null);

    @GetMapping("wrong/threadLocal")
    public Map<String, String> wrong(@RequestParam("userId") Integer userId) {

        try {
            // 设置用户信息之前先查询一次 ThreadLocal 中的用户信息
            String before = Thread.currentThread().getName() + ":" + currentUser.get();
            // 设置用户信息到 ThreadLocal
            currentUser.set(userId);
            // 设置用户信息之后再查询一次 ThreadLocal 中的用户信息
            String after = Thread.currentThread().getName() + ":" + currentUser.get();
            // 汇总输出两次查询结果
            Map<String, String> result = new HashMap<>();
            result.put("before", before);
            result.put("after", after);
            return result;
        } finally {
            currentUser.remove();
        }
    }
}
