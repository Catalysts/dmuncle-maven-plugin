package io.dmuncle.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;

import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author Cristian Ilca, Catalysts Romania on 07-Dec-15.
 */
@Mojo(name = "dmuncle-send", defaultPhase = LifecyclePhase.PACKAGE, aggregator = true)
public class DmUncleSendMojo extends AbstractMojo {
    private static final Logger LOG = getLogger(DmUncleSendMojo.class);
    public static final String JSON_PACKAGE_FILENAME = "dmuncle-package.json";

    @Parameter(required = true)
    private String serverAddress;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    public void execute() throws MojoExecutionException, MojoFailureException {
        String url = serverAddress + "/import";
        LOG.info("Sending POST request to server " + url);
        Path jsonFilePath = Paths.get(JSON_PACKAGE_FILENAME);
        addProjectNameToJSONPackage();
        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);
            OutputStream os = con.getOutputStream();
            os.write(Files.readAllBytes(jsonFilePath));
            os.flush();

            if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + con.getResponseCode());

            } else {
                LOG.info("Successfully sent data.");
            }

            con.disconnect();
            Files.delete(jsonFilePath);
            Files.delete(Paths.get("dmuncle-buffer-file.txt"));
        } catch (MalformedURLException e) {
            LOG.error("Error encountered with server URL: " + e.getMessage(), e);
        } catch (ProtocolException e) {
            LOG.error("Error encountered on sending data: " + e.getMessage(), e);
        } catch (IOException e) {
            LOG.error("Error encountered on finding json file: " + e.getMessage(), e);
        }
    }

    private void addProjectNameToJSONPackage() {
        try {
            JSONParser parser = new JSONParser();
            FileReader fileReader = new FileReader(JSON_PACKAGE_FILENAME);
            JSONObject jsonPackage = (JSONObject) parser.parse(fileReader);
            jsonPackage.put("projectName", project.getName());
            FileUtils.fileWrite(JSON_PACKAGE_FILENAME, jsonPackage.toString());
            fileReader.close();
        } catch (IOException e) {
            LOG.error("Error encountered on finding json file: " + e.getMessage(), e);
        } catch (ParseException e) {
            LOG.error("Error encountered on parsing json file: " + e.getMessage(), e);
        }
    }
}
