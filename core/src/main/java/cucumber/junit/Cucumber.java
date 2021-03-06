package cucumber.junit;

import cucumber.classpath.Classpath;
import cucumber.classpath.Consumer;
import cucumber.io.Resource;
import cucumber.runtime.Runtime;
import gherkin.I18n;
import gherkin.formatter.model.Feature;
import gherkin.parser.Parser;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;

import java.util.ArrayList;
import java.util.List;

public class Cucumber extends ParentRunner<ScenarioRunner> {
    private Feature feature;
    private final String pathName;
    private final List<ScenarioRunner> children = new ArrayList<ScenarioRunner>();
    private final RunnerBuilder builder;
    private I18n i18n;

    private static Runtime runtime(Class testClass) {
        String packageName = testClass.getName().substring(0, testClass.getName().lastIndexOf("."));
        final Runtime runtime = new Runtime(packageName);
        java.lang.Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                for (String snippet : runtime.getSnippets()) {
                    System.out.println(snippet);
                }
            }
        });
        return runtime;
    }

    /**
     * Constructor called by JUnit.
     */
    public Cucumber(Class featureClass) throws InitializationError {
        this(featureClass, runtime(featureClass));
    }

    public Cucumber(Class featureClass, final Runtime runtime) throws InitializationError {
        // Why aren't we passing the class to super? I don't remember, but there is probably a good reason.
        super(null);
        cucumber.junit.Feature featureAnnotation = (cucumber.junit.Feature) featureClass.getAnnotation(cucumber.junit.Feature.class);
        if (featureAnnotation != null) {
            pathName = featureAnnotation.value();
        } else {
            pathName = featureClass.getName().replace('.', '/') + ".feature";
        }
        builder = new RunnerBuilder(runtime, children);
        parseFeature();
    }

    @Override
    public String getName() {
        return feature.getKeyword() + ": " + feature.getName();
    }

    @Override
    protected List<ScenarioRunner> getChildren() {
        return children;
    }

    @Override
    protected Description describeChild(ScenarioRunner child) {
        return child.getDescription();
    }

    @Override
    protected void runChild(ScenarioRunner runner, RunNotifier notifier) {
        runner.setLocale(i18n.getLocale());
        runner.run(notifier);
    }

    private void parseFeature() {
        final String[] gherkin = new String[1];
        Classpath.scan(pathName, new Consumer() {
            public void consume(Resource resource) {
                gherkin[0] = resource.getString();
            }
        });
        Parser parser = new Parser(builder);
        parser.parse(gherkin[0], pathName, 0);
        i18n = parser.getI18nLanguage();
        feature = builder.getFeature();
    }
}
