package lk.vish.novabooks;

import jakarta.servlet.MultipartConfigElement;
import lk.vish.novabooks.config.AppConfig;
import lk.vish.novabooks.listener.ContextPathListener;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import java.io.File;

public class Main {

    private static final int SERVER_PORT = 8080;
    private static final String CONTEXT_PATH = "/novabooks";

    public static void main(String[] args) {
        try {
            Tomcat tomcat = new Tomcat();
            tomcat.setPort(SERVER_PORT);
            tomcat.getConnector();

            Context context = tomcat.addWebapp(CONTEXT_PATH, new File("src/main/webapp").getAbsolutePath());
//            Tomcat.addServlet(context, "JerseyServlet", new ServletContainer(new AppConfig()));
//            context.addServletMappingDecoded("/api/*", "JerseyServlet");

            context.addApplicationListener(ContextPathListener.class.getName());


            Wrapper servlet = Tomcat.addServlet(context, "JerseyServlet", new ServletContainer(new ResourceConfig().packages("lk.vish.novabooks")));
            servlet.setLoadOnStartup(1);
            servlet.addMapping("/api/*");

            servlet.setMultipartConfigElement(new MultipartConfigElement(
                    System.getProperty("java.io.tmpdir"), // Temporary storage directory
                    10485760L,  // Max File Size: 10MB
                    52428800L,  // Max Request Size: 50MB
                    2097152     // Threshold: 2MB
            ));

            tomcat.start();
            System.out.println("App URL: http://localhost:" + SERVER_PORT + CONTEXT_PATH);
            tomcat.getServer().await();
        } catch (LifecycleException e) {
            throw new RuntimeException("Tomcat Embedded Server loading failed: " + e.getMessage());
        }
    }
}
