package org.graalvm.buildtools.maven.config;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class NativeConfiguration extends AbstractMojo {
    @Parameter(defaultValue = "${project.build.directory}", property = "outputDir", required = true)
    public File outputDirectory;

    @Parameter(property = "mainClass")
    public String mainClass;

    @Parameter(property = "imageName", defaultValue = "${project.artifactId}")
    public String imageName;

    @Parameter(property = "classpath")
    public List<String> classpath;

    @Parameter(property = "classesDirectory")
    public File classesDirectory;

    @Parameter(defaultValue = "${project.build.outputDirectory}", readonly = true, required = true)
    public File defaultClassesDirectory;

    @Parameter(property = "debug", defaultValue = "false")
    public boolean debug;

    @Parameter(property = "fallback", defaultValue = "false")
    public boolean fallback;

    @Parameter(property = "verbose", defaultValue = "false")
    public boolean verbose;

    @Parameter(property = "sharedLibrary", defaultValue = "false")
    public boolean sharedLibrary;

    @Parameter(property = "quickBuild", defaultValue = "false")
    public boolean quickBuild;

    @Parameter(property = "useArgFile")
    public Boolean useArgFile;

    @Parameter(property = "buildArgs")
    public List<String> buildArgs;

    @Parameter(defaultValue = "${project.build.directory}/native/generated", property = "resourcesConfigDirectory", required = true)
    public File resourcesConfigDirectory;

    @Parameter(property = "agentResourceDirectory")
    public File agentResourceDirectory;

    @Parameter(property = "excludeConfig")
    public List<ExcludeConfigConfiguration> excludeConfig;

    @Parameter(property = "environmentVariables")
    public Map<String, String> environment;

    @Parameter(property = "systemPropertyVariables")
    public Map<String, String> systemProperties;

    @Parameter(property = "configurationFileDirectories")
    public List<String> configFiles;

    @Parameter(property = "jvmArgs")
    public List<String> jvmArgs;

    @Parameter(alias = "metadataRepository")
    public MetadataRepositoryConfiguration metadataRepositoryConfiguration;

    @Parameter(property = "nativeDryRun", defaultValue = "false")
    public boolean dryRun;

    /**
     * NativeExtension is being run in a phase where Mojo parameters aren't yet parsed and injected.
     * So, in order to prevent us needing to crawl XML configuration tree manually (which is a painful,
     * tedious and error-prone way of fetching data), we implement a crude version of data-binding here.
     * @param configuration
     * @return read-only instance of NativeConfiguration object.
     */
    public static NativeConfiguration fromXML(Xpp3Dom configuration) {
        Stream.of(NativeConfiguration.class.getDeclaredFields()).forEach(field -> {
                    Parameter annotation = field.getAnnotation(Parameter.class);
                    if (annotation == null) {
                        return;
                    }

                    String property = annotation.property().isEmpty() ? field.getName() : annotation.property();
                    String alias = annotation.alias();
                    Xpp3Dom element = configuration.getChild(property);
                    if (element == null && alias != null) {
                        element = configuration.getChild(alias);
                    }
                    if (element == null) {
                        return;
                    }
                    String value = element.getValue();
                    switch (field.getType().getSimpleName()) {

                    }
                });
    }

    @Override
    public void execute() throws MojoExecutionException {
        throw new UnsupportedOperationException();
    }
}
