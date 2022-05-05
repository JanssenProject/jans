package io.jans.plugin.demo.ext;

import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.Extension;

import org.slf4j.Logger;

@ApplicationScoped
public class DemoExtension implements Extension {
    
}
