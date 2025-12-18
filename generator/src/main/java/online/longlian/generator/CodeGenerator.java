package online.longlian.generator;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;


public class CodeGenerator {
    public static void main(String[] args) {
        FastAutoGenerator.create("url", "root", "123456")
                .globalConfig(builder -> {
                    builder.author("longlian") // 设置作者
                            .enableSwagger() // 开启 swagger 模式
                            .outputDir("app\\src\\main\\java"); // 指定输出目录
                })
                .packageConfig(builder ->
                        builder.parent("online.longlian") // 设置父包名
                                .moduleName("app") // 设置父包模块名
                )
                .strategyConfig(builder ->
                        builder.addExclude() // 设置需要生成的表名
                                .entityBuilder()
                                .enableLombok()
                                .enableTableFieldAnnotation()
                                .controllerBuilder()
                                .enableRestStyle()
                )
                .templateEngine(new FreemarkerTemplateEngine()) // 使用Freemarker引擎模板，默认的是Velocity引擎模板dd
                .execute();
    }
}
