import magpiebridge.core.LanguageExtensionHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;

/**
 * Custom LanguageExtensionHandler for MagpieBridge that overrides the extensions associated with C to include .h and .i.
 * For other languages falls back to the provided fallback LanguageExtensionHandler.
 */
public class GoblintLanguageExtensionHandler implements LanguageExtensionHandler {

    private static final Logger log = LogManager.getLogger(GoblintLanguageExtensionHandler.class);

    private final LanguageExtensionHandler fallbackLanguageExtensionHandler;

    public GoblintLanguageExtensionHandler(LanguageExtensionHandler fallbackLanguageExtensionHandler) {
        this.fallbackLanguageExtensionHandler = fallbackLanguageExtensionHandler;
    }

    @Override
    public String getLanguageForExtension(String extension) {
        log.info("Requested language for " + extension);
        if (".c".equals(extension) || ".i".equals(extension) || ".h".equals(extension)) {
            return "c";
        }
        return fallbackLanguageExtensionHandler.getLanguageForExtension(extension);
    }

    @Override
    public Set<String> getExtensionsForLanguage(String language) {
        log.info("Requested extensions for " + language);
        if ("c".equals(language)) {
            return Set.of(".c", ".i", ".h");
        }
        return fallbackLanguageExtensionHandler.getExtensionsForLanguage(language);
    }

}
