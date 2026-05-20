package com.projectestimation.backend.proposal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.pandoc")
public class PandocProperties {

    private String executable = "pandoc";
    private String tempDir = System.getProperty("java.io.tmpdir") + "/proposal-conversions";

    public String getExecutable() {
        return executable;
    }

    public void setExecutable(String executable) {
        this.executable = executable;
    }

    public String getTempDir() {
        return tempDir;
    }

    public void setTempDir(String tempDir) {
        this.tempDir = tempDir;
    }
}
