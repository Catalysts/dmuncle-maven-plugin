package io.dmuncle.maven;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static org.slf4j.LoggerFactory.getLogger;

@Mojo(name = "dmuncle-watch", defaultPhase = LifecyclePhase.PACKAGE)
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
        File JSONFile = new File(JSON_PACKAGE_FILENAME);
        if (JSONFile.exists()) {
            getExistingJSONPackage();
        } else {
            initializeJSONPackage();
        }
        for (int i = 0; i < project.getDependencies().size(); i++) {
            Dependency dependency = (Dependency) project.getDependencies().get(i);
            addDependencyToJSONPackage(dependency, project.getName());
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

    private void addDependencyToJSONPackage(Dependency dependencym, String moduleName) {
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
}
