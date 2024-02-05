package com.herfy.model;

public class SupportedCountry {
    public String applicationId;
    public String applicationName;
    public String applicationKey;
    public String applicationConcept;
    public String splashColor;
    public boolean splashLogo;
    public boolean splashBackground;
    public boolean splashVideo;
    public boolean onboardingEnabled;
    public String soloAppType;
    public boolean soloLogo;
    public boolean foodicsLogo;
    public boolean subscriptionEnabled;
    public String tapTest;
    public String tapLive;
    public String imageUrl;
    public String splashImage;
    public String googleSignInClient;

    @Override
    public String toString() {
        return "SupportedCountry{" +
                "applicationId='" + applicationId + '\'' +
                ", applicationName='" + applicationName + '\'' +
                ", applicationKey='" + applicationKey + '\'' +
                ", applicationConcept='" + applicationConcept + '\'' +
                ", splashColor='" + splashColor + '\'' +
                ", splashLogo=" + splashLogo +
                ", splashBackground=" + splashBackground +
                ", splashVideo=" + splashVideo +
                ", onboardingEnabled=" + onboardingEnabled +
                ", soloAppType='" + soloAppType + '\'' +
                ", soloLogo=" + soloLogo +
                ", foodicsLogo=" + foodicsLogo +
                ", subscriptionEnabled=" + subscriptionEnabled +
                ", tapTest='" + tapTest + '\'' +
                ", tapLive='" + tapLive + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", splashImage='" + splashImage + '\'' +
                ", googleSignInClient='" + googleSignInClient + '\'' +
                '}';
    }
}
