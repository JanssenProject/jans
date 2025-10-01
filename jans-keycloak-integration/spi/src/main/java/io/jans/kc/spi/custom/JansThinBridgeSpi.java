package io.jans.kc.spi.custom;

import org.keycloak.provider.*;

public class JansThinBridgeSpi  implements Spi {
    
    private static final String SPI_NAME = "kc-jans-thin-bridge";
    @Override
    public boolean isInternal() {

        return false;
    }

    @Override
    public String getName() {

        return SPI_NAME;
    }

    @Override
    public Class<? extends Provider> getProviderClass() {

        return JansThinBridgeProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {

        return JansThinBridgeProviderFactory.class;
    }
}
