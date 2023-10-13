package io.jans.casa.plugins.credentials.extensions;

import io.jans.casa.extension.PreferredMethodFragment;
import org.pf4j.Extension;

/**
 * An extension class implementing the {@link PreferredMethodFragment} extension point. It allows to insert extra markup
 * when users have enabled 2fa in their accounts.
 * @author madhumita
 */
@Extension
public class SampleFragment implements PreferredMethodFragment 
{

    public String getUrl() {
        return "index.zul";
    }

}
