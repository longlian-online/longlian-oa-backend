package online.longlian.app.common.annotation;

import online.longlian.app.common.constants.SecurityConstants;
import online.longlian.app.common.enumeration.OrganizationDeclaration;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserSessionMappingTest {

    @Test
    void everyUserTokenHandlerDeclaresOrganizationExactlyOnce() throws ClassNotFoundException {
        var scanner = new org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        scanner.addIncludeFilter(new AnnotationTypeFilter(Controller.class));
        List<String> failures = new ArrayList<>();

        for (var definition : scanner.findCandidateComponents("online.longlian.app.controller")) {
            Class<?> type = Class.forName(definition.getBeanClassName());
            RequestMapping typeMapping = AnnotatedElementUtils.findMergedAnnotation(type, RequestMapping.class);
            for (Method method : type.getDeclaredMethods()) {
                if (method.isSynthetic() || method.isBridge()) {
                    continue;
                }
                RequestMapping methodMapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                if (methodMapping == null) {
                    continue;
                }
                List<UserSession> declarations = UserSessionDeclarations.of(method);
                List<String> paths = combine(pathsOf(typeMapping), pathsOf(methodMapping));
                boolean admin = paths.stream().allMatch(UserSessionMappingTest::isAdminPath);
                boolean permitAll = isPermitAll(paths, verbsOf(methodMapping));
                String where = type.getSimpleName() + "." + method.getName() + " " + paths;

                if (admin) {
                    if (!declarations.isEmpty()) {
                        failures.add(where + " 是管理端接口，不能声明用户组织");
                    }
                    continue;
                }
                if (declarations.size() != 1) {
                    failures.add(where + " 必须正好有一个 @UserSession，实际 " + declarations.size());
                    continue;
                }
                if (permitAll && declarations.get(0).value() != OrganizationDeclaration.NONE) {
                    failures.add(where + " 是免鉴权接口，必须是 NONE");
                }
            }
        }

        assertThat(failures).isEmpty();
    }

    private static boolean isPermitAll(List<String> paths, List<RequestMethod> verbs) {
        for (String path : paths) {
            for (RequestMethod verb : verbs) {
                MockHttpServletRequest request = new MockHttpServletRequest(verb.name(), path);
                boolean permitted = SecurityConstants.getPermitAllMatchers().stream().anyMatch(matcher -> matcher.matches(request));
                if (permitted) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isAdminPath(String path) {
        return "/admin".equals(path) || path.startsWith("/admin/");
    }

    private static List<String> combine(List<String> typePaths, List<String> methodPaths) {
        List<String> combined = new ArrayList<>();
        for (String typePath : typePaths) {
            for (String methodPath : methodPaths) {
                combined.add(join(typePath, methodPath));
            }
        }
        return combined;
    }

    private static String join(String typePath, String methodPath) {
        if (typePath.isEmpty()) {
            return methodPath.isEmpty() ? "/" : methodPath;
        }
        if (methodPath.isEmpty()) {
            return typePath;
        }
        String left = typePath.endsWith("/") ? typePath.substring(0, typePath.length() - 1) : typePath;
        String right = methodPath.startsWith("/") ? methodPath : "/" + methodPath;
        return left + right;
    }

    private static List<String> pathsOf(RequestMapping mapping) {
        if (mapping == null) {
            return List.of("");
        }
        String[] paths = mapping.path().length > 0 ? mapping.path() : mapping.value();
        if (paths.length == 0) {
            return List.of("");
        }
        return List.of(paths);
    }

    private static List<RequestMethod> verbsOf(RequestMapping mapping) {
        if (mapping.method().length == 0) {
            return List.of(RequestMethod.values());
        }
        return List.of(mapping.method());
    }
}
