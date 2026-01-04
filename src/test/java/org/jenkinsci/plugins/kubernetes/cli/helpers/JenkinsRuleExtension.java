package org.jenkinsci.plugins.kubernetes.cli.helpers;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.jvnet.hudson.test.JenkinsRule;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * JUnit 5 extension that bridges JenkinsRule (designed for JUnit 4) with JUnit
 * 5.
 *
 * <p>
 * This extension automatically finds JenkinsRule fields in test classes and
 * manages
 * their lifecycle (before/after each test). It sets up the test description
 * that
 * JenkinsRule expects from JUnit 4's test runner.
 *
 * <p>
 * Usage:
 *
 * <pre>
 * {@code @ExtendWith(JenkinsRuleExtension.class)}
 * public class MyTest {
 *     public final JenkinsRule r = new JenkinsRule();
 *     // ... tests ...
 * }
 * </pre>
 */
public class JenkinsRuleExtension implements BeforeEachCallback, AfterEachCallback {

    private static final Field TEST_DESCRIPTION_FIELD;

    static {
        Field field = null;
        try {
            field = JenkinsRule.class.getDeclaredField("testDescription");
            field.setAccessible(true);
        } catch (NoSuchFieldException | SecurityException e) {
            // Field doesn't exist or can't be accessed - will handle gracefully
        }
        TEST_DESCRIPTION_FIELD = field;
    }

    /**
     * Finds all JenkinsRule fields in the test instance.
     */
    private List<Field> findJenkinsRuleFields(Object testInstance) {
        List<Field> jenkinsRuleFields = new ArrayList<>();
        Class<?> clazz = testInstance.getClass();

        // Check all fields in the class hierarchy
        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getType() == JenkinsRule.class) {
                    field.setAccessible(true);
                    jenkinsRuleFields.add(field);
                }
            }
            clazz = clazz.getSuperclass();
        }

        return jenkinsRuleFields;
    }

    /**
     * Sets up the test description for JenkinsRule to work with JUnit 5.
     */
    private void setupTestDescription(JenkinsRule rule, Object testInstance, ExtensionContext context) {
        if (TEST_DESCRIPTION_FIELD == null) {
            return;
        }

        try {
            String testMethodName = context.getTestMethod()
                    .map(m -> m.getName())
                    .orElse("test");

            org.junit.runner.Description description = org.junit.runner.Description.createTestDescription(
                    testInstance.getClass(),
                    testMethodName);

            TEST_DESCRIPTION_FIELD.set(rule, description);
        } catch (IllegalAccessException e) {
            // If we can't set the description, continue without it
            // Some versions of JenkinsRule may not require it
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        Object testInstance = context.getRequiredTestInstance();
        List<Field> fields = findJenkinsRuleFields(testInstance);

        for (Field field : fields) {
            JenkinsRule rule = (JenkinsRule) field.get(testInstance);
            if (rule != null) {
                setupTestDescription(rule, testInstance, context);
                try {
                    rule.before();
                } catch (Error e) {
                    // Re-throw Errors as-is (they should not be caught)
                    throw e;
                } catch (Exception e) {
                    // Re-throw Exceptions as-is
                    throw e;
                } catch (Throwable t) {
                    // Wrap other Throwables (though unlikely)
                    throw new Exception("JenkinsRule.before() failed", t);
                }
            }
        }
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        Object testInstance = context.getRequiredTestInstance();
        List<Field> fields = findJenkinsRuleFields(testInstance);

        // Process in reverse order for cleanup
        for (int i = fields.size() - 1; i >= 0; i--) {
            Field field = fields.get(i);
            JenkinsRule rule = (JenkinsRule) field.get(testInstance);
            if (rule != null) {
                try {
                    rule.after();
                } catch (Error e) {
                    // Re-throw Errors as-is (they should not be caught)
                    throw e;
                } catch (Exception e) {
                    // Re-throw Exceptions as-is
                    throw e;
                } catch (Throwable t) {
                    // Wrap other Throwables (though unlikely)
                    throw new Exception("JenkinsRule.after() failed", t);
                }
            }
        }
    }
}
