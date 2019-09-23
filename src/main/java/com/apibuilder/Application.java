package com.apibuilder;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.apibuilder.security.KeyStoreProps;
import com.apibuilder.storage.AwsSystemsManagerClient;
import com.apibuilder.storage.StorageProperties;
import com.apibuilder.storage.StorageService;

@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    CommandLineRunner init(StorageService storageService) {
        return (args) -> {
            storageService.deleteAll();
            storageService.init();
        };
    }
    
    private int maxUploadSizeInMb = 10 * 1024 * 1024; // 10 MB
    private AwsSystemsManagerClient ssm=new AwsSystemsManagerClient();
    
    @Bean
    public TomcatEmbeddedServletContainerFactory tomcatEmbedded() {

        TomcatEmbeddedServletContainerFactory tomcat = new TomcatEmbeddedServletContainerFactory() { @Override
			protected void postProcessContext(Context context) {
				SecurityConstraint securityConstraint = new SecurityConstraint();
				securityConstraint.setUserConstraint("CONFIDENTIAL");
				SecurityCollection collection = new SecurityCollection();
				collection.addPattern("/*");
				securityConstraint.addCollection(collection);
				context.addConstraint(securityConstraint);
			}
        };

        tomcat.addConnectorCustomizers((TomcatConnectorCustomizer) connector -> {

            // configure SSL settings
            if ((connector.getProtocolHandler() instanceof AbstractHttp11Protocol<?>)) {
            	
                Http11NioProtocol proto = (Http11NioProtocol) connector.getProtocolHandler();
                proto.setSSLEnabled(true);
                connector.setScheme("https");
                connector.setSecure(true);
        		proto.setKeystoreFile(KeyStoreProps.keystorePath);
        		proto.setKeystorePass(ssm.getParameter(KeyStoreProps.keystorePasswordPath, true));
        		proto.setKeystoreType(KeyStoreProps.keystoreType);
        		//proto.setProperty("keystoreProvider", keystoreProvider);
        		proto.setKeyAlias(KeyStoreProps.keyAlias);
        		
                // maxSwallowSize --> -1 means unlimited, accept bytes
                ((AbstractHttp11Protocol<?>) connector.getProtocolHandler()).setMaxSwallowSize(-1);
            }

        });
        
        tomcat.addAdditionalTomcatConnectors(redirectConnector());

        return tomcat;
    }
    
    private Connector redirectConnector() {
        Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
        connector.setScheme("http");
        connector.setPort(8080);
        connector.setSecure(false);
        connector.setRedirectPort(8443);
        return connector;
    }
}
