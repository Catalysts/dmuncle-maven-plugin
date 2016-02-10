package io.dmuncle.maven;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.*;
import org.codehaus.plexus.util.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

@Mojo(name = "dmuncle-watch", defaultPhase = LifecyclePhase.PACKAGE, aggregator = true)
public class DmUncleMavenMojo extends AbstractMojo {
    private static final Logger LOG = getLogger(DmUncleMavenMojo.class);

    public static final String JSON_PACKAGE_FILENAME = "dmuncle-package.json";

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    private JSONObject JSONPackage;
    private JSONArray compileDeps;
    private JSONArray testCompileDeps;
    private JSONArray runtimeDeps;
    private JSONArray systemDeps;

    public void execute() throws MojoExecutionException, MojoFailureException {
        LOG.info("Gathering dependencies for project " + project.getName());
        runCommandToListDependencies();
        List<String> extractedOutputLines = processCommandLineOutput();
        List<Dependency> projectDependencies = mapExtractedOutputLinesToDependencies(extractedOutputLines);
        File JSONFile = new File(JSON_PACKAGE_FILENAME);
        if (JSONFile.exists()) {
            getExistingJSONPackage();
        } else {
            initializeJSONPackage();
        }
        for (Dependency dependency : projectDependencies) {
            addDependencyToJSONPackage(dependency, project.getGroupId() + ":" + project.getArtifactId());
        }
        saveJSONPackage(JSONPackage);
    }

    private void initializeJSONPackage() {
        JSONPackage = new JSONObject();
        compileDeps = new JSONArray();
        testCompileDeps = new JSONArray();
        runtimeDeps = new JSONArray();
        systemDeps = new JSONArray();

        JSONPackage.put("compileArtifacts", compileDeps);
        JSONPackage.put("testCompileArtifacts", testCompileDeps);
        JSONPackage.put("runtimeArtifacts", runtimeDeps);
        JSONPackage.put("testRuntimeArtifacts", systemDeps);
    }

    private void getExistingJSONPackage() {
        JSONParser parser = new JSONParser();
        try {
            JSONPackage = (JSONObject) parser.parse(new FileReader(JSON_PACKAGE_FILENAME));
            compileDeps = (JSONArray) JSONPackage.get("compileArtifacts");
            testCompileDeps = (JSONArray) JSONPackage.get("testCompileArtifacts");
            runtimeDeps = (JSONArray) JSONPackage.get("runtimeArtifacts");
            systemDeps = (JSONArray) JSONPackage.get("testRuntimeArtifacts");
        } catch (IOException e) {
            LOG.error("Error encountered on getting json file: " + e.getMessage(), e);
        } catch (ParseException e) {
            LOG.error("Error encountered on parsing json file: " + e.getMessage(), e);
        }
    }

    private void addDependencyToJSONPackage(Dependency dependency, String moduleName) {
        JSONObject jsonDep = new JSONObject();
        jsonDep.put("groupId", dependency.getGroupId());
        jsonDep.put("artifactId", dependency.getArtifactId());
        jsonDep.put("version", dependency.getVersion());
        jsonDep.put("moduleName", moduleName);
        if (dependency.getScope().equals("compile")) {
            compileDeps.add(jsonDep);
        }
        if (dependency.getScope().equals("runtime")) {
            runtimeDeps.add(jsonDep);
        }
        if (dependency.getScope().equals("system")) {
            systemDeps.add(jsonDep);
        }
        if (dependency.getScope().equals("test")) {
            testCompileDeps.add(jsonDep);
        }
    }

    void saveJSONPackage(JSONObject jsonPackage) {
        try {
            FileUtils.fileWrite(JSON_PACKAGE_FILENAME, jsonPackage.toString());
        } catch (IOException e) {
            LOG.error("Error encountered on saving json file: " + e.getMessage(), e);
        }
    }

    void runCommandToListDependencies() {
//        LOG.info("Executing: mvn dependency:list");
        PrintStream oldSystemOut = System.out;
        PrintStream out = null;
        try {
            out = new PrintStream(new FileOutputStream("dmuncle-buffer-file.txt"));
        } catch (FileNotFoundException e) {
//            LOG.error("Error encountered on opening the buffer file: " + e.getMessage(), e);
        }
        System.setOut(out);
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(new File("pom.xml"));
        request.setGoals(Arrays.asList("dependency:list"));
        Invoker invoker = new DefaultInvoker();
        try {
            invoker.execute(request);
        } catch (MavenInvocationException e) {
//            LOG.error("Error encountered on executing mvn dependency:list " + e.getMessage(), e);
        }
        System.out.flush();
        System.setOut(oldSystemOut);
        try {
            List<String> consoleOutputLines = Files.readAllLines(Paths.get("dmuncle-buffer-file.txt"));
            for (String outputLine : consoleOutputLines) {
                System.out.println(outputLine);
            }
        } catch (IOException e) {
//            LOG.error("Error encountered on reading from the buffer file: " + e.getMessage(), e);
        }
    }

    private List<String> processCommandLineOutput() {
//        LOG.info("Process command output");
        List<String> outputLines = null;
        List<String> dependencies = new ArrayList<String>();
        try {
            outputLines = Files.readAllLines(Paths.get("dmuncle-buffer-file.txt"));
        } catch (IOException e) {
//            LOG.error("Error encountered on reading the buffer file: " + e.getMessage(), e);
        }
        boolean dependencyBlock = false;
        for (int i = 0; i < outputLines.size(); i++) {
            String line = outputLines.get(i);
            if (line.contains("The following files have been resolved:")) {
                dependencyBlock = true;
            } else if (line.replace("[INFO]", "").equals(" ") && dependencyBlock) {
                dependencyBlock = false;
            }
            if (dependencyBlock) {
                String processedLine = line.replace("[INFO]", "").trim();
                if (!processedLine.contains("The following files have been resolved:") && !processedLine.equals("none")) {
                    dependencies.add(processedLine);
                }
            }
        }
        return dependencies;
    }

    private List<Dependency> mapExtractedOutputLinesToDependencies(List<String> lines) {
        List<Dependency> dependencies = new ArrayList<Dependency>();
        for (String line : lines) {
            String[] dependencyParts = line.split(":");
            Dependency dependency = new Dependency();
            dependency.setGroupId(dependencyParts[0]);
            dependency.setArtifactId(dependencyParts[1]);
            dependency.setType(dependencyParts[2]);
            dependency.setVersion(dependencyParts[3]);
            dependency.setScope(dependencyParts[4]);
            dependencies.add(dependency);
        }
        return dependencies;

    }
}
