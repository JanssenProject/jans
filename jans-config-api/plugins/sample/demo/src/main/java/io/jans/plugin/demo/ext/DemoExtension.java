package io.jans.plugin.demo.ext;

import javax.inject.Inject;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Extension;

import org.slf4j.Logger;

@ApplicationScoped
public class DemoExtension implements Extension {
    
}
