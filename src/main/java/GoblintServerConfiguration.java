import magpiebridge.core.LanguageExtensionHandler;
import magpiebridge.core.ServerConfiguration;

/**
 * ServerConfiguration subclass that allows setting a custom language extension handler.
 */
public class GoblintServerConfiguration extends ServerConfiguration {

    private LanguageExtensionHandler customLanguageExtensionHandler = null;

    public void setLanguageExtensionHandler(LanguageExtensionHandler languageExtensionHandler) {
        customLanguageExtensionHandler = languageExtensionHandler;
    }

    @Override
    public LanguageExtensionHandler getLanguageExtensionHandler() {
        return customLanguageExtensionHandler != null ? customLanguageExtensionHandler : super.getLanguageExtensionHandler();
    }

}
