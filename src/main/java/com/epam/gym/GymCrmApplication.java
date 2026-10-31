package com.epam.gym;

import com.epam.gym.config.AppConfig;
import com.epam.gym.web.config.WebConfig;
import com.epam.gym.web.logging.RestCallLoggingFilter;
import com.epam.gym.web.logging.TransactionIdFilter;
import jakarta.servlet.Filter;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import java.io.IOException;
import java.nio.file.Files;

public class GymCrmApplication {
    private static final Logger log = LoggerFactory.getLogger(GymCrmApplication.class);

    public static void main(String[] args) throws LifecycleException, IOException {
        AnnotationConfigApplicationContext rootContext = new AnnotationConfigApplicationContext(AppConfig.class);
        rootContext.registerShutdownHook();
        Environment environment = rootContext.getEnvironment();
        int port = environment.getRequiredProperty("server.port", Integer.class);
        int maxBodyLength = environment.getRequiredProperty("logging.rest.max-body-length", Integer.class);

        AnnotationConfigWebApplicationContext webContext = new AnnotationConfigWebApplicationContext();
        webContext.setParent(rootContext);
        webContext.register(WebConfig.class);

        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(Files.createTempDirectory("gym-crm-tomcat").toString());
        tomcat.setPort(port);
        tomcat.getConnector();

        Context servletContext = tomcat.addContext("", null);
        servletContext.setParentClassLoader(GymCrmApplication.class.getClassLoader());
        addFilter(servletContext, "transactionIdFilter", new TransactionIdFilter(), "/*");
        addFilter(servletContext, "restCallLoggingFilter", new RestCallLoggingFilter(maxBodyLength), "/api/*");
        Tomcat.addServlet(servletContext, "dispatcher", new DispatcherServlet(webContext)).setLoadOnStartup(1);
        servletContext.addServletMappingDecoded("/", "dispatcher");

        tomcat.start();
        log.info("Gym CRM REST API started on http://localhost:{}", port);
        tomcat.getServer().await();
    }

    private static void addFilter(Context servletContext, String name, Filter filter, String urlPattern) {
        FilterDef definition = new FilterDef();
        definition.setFilterName(name);
        definition.setFilter(filter);
        servletContext.addFilterDef(definition);

        FilterMap mapping = new FilterMap();
        mapping.setFilterName(name);
        mapping.addURLPattern(urlPattern);
        servletContext.addFilterMap(mapping);
    }
}
