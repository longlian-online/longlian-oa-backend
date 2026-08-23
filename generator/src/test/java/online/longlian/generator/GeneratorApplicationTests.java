package online.longlian.generator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = CodeGenerator.class)
// skipcq: JAVA-W1088 — 该类含 contextLoads 测试，analyzer 误报
class GeneratorApplicationTests {

    @Test
    void contextLoads() {
    }

}
