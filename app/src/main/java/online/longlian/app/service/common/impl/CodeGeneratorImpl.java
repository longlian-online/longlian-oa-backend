package online.longlian.app.service.common.impl;

import online.longlian.app.common.util.RandomCodeUtil;
import online.longlian.app.service.common.CodeGenerator;
import org.springframework.stereotype.Component;

@Component
public class CodeGeneratorImpl implements CodeGenerator {

    @Override
    public String generate(int length) {
        return RandomCodeUtil.generateCode(length);
    }
}
