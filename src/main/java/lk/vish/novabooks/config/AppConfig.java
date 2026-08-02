package lk.vish.novabooks.config;

import org.glassfish.jersey.server.ResourceConfig;

public class AppConfig extends ResourceConfig {
    public AppConfig(){
        packages("lk.vish.novabooks.controller");
        packages("lk.vish.novabooks.middleware");
        register(org.glassfish.jersey.media.multipart.MultiPartFeature.class);
    }
}
