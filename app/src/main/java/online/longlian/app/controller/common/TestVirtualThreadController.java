package online.longlian.app.controller.common;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.locks.LockSupport;


@Slf4j
@Tag(name = "测试接口", description = "仅开发时测试使用")
@RequestMapping("/_test/virtual_thread")
@RestController
public class TestVirtualThreadController {
    @GetMapping("")
    public String VirtualThreadTest() {
        var thread = Thread.currentThread();
        System.out.println("线程名称：" + thread.getName());
        return "是否虚拟线程：" + thread.isVirtual();
    }

    @GetMapping("/cpu")
    public int cpu() {
        int sum = 0;
        for (int i = 0; i < 1000000; i++) sum++;
        return sum;
    }

    @GetMapping("/io")
    public String io() throws InterruptedException {
        LockSupport.parkNanos(50_000_000); // 50ms
        LockSupport.parkNanos(50_000_000); // 50ms
        LockSupport.parkNanos(50_000_000); // 50ms
        LockSupport.parkNanos(50_000_000); // 50ms
        return "ok";
    }

    @GetMapping("/mix")
    public String mix() throws InterruptedException {
        // 计算 5ms
        int sum = 0;
        for (int i = 0; i < 50000; i++) sum++;
        // IO 阻塞 80ms
        Thread.sleep(80);
        return "mix";
    }
}
