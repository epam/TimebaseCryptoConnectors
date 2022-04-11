package com.epam.deltix.data.connectors.commons;

public class DataConnectorSettings {

    private String tbUrl = "dxtick://localhost:8011";
    private String tbUser;
    private String tbPassword;
    private String stream;

    public DataConnectorSettings() {
    }

    public DataConnectorSettings(String tbUrl, String stream) {
        this.tbUrl = tbUrl;
        this.stream = stream;
    }

    public String getTbUrl() {
        return tbUrl;
    }

    public void setTbUrl(String tbUrl) {
        this.tbUrl = tbUrl;
    }

    public String getTbUser() {
        return tbUser;
    }

    public void setTbUser(String tbUser) {
        this.tbUser = tbUser;
    }

    public String getTbPassword() {
        return tbPassword;
    }

    public void setTbPassword(String tbPassword) {
        this.tbPassword = tbPassword;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

}
