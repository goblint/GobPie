package magpiebridge;

import magpiebridge.core.LanguageExtensionHandler;

import java.util.Set;

/**
 * Custom LanguageExtensionHandler for MagpieBridge that overrides the extensions associated with C to include .h and .i.
 * For other languages falls back to the provided fallback LanguageExtensionHandler.
 */
public class GoblintLanguageExtensionHandler implements LanguageExtensionHandler {

    private final LanguageExtensionHandler fallbackLanguageExtensionHandler;

    public GoblintLanguageExtensionHandler(LanguageExtensionHandler fallbackLanguageExtensionHandler) {
        this.fallbackLanguageExtensionHandler = fallbackLanguageExtensionHandler;
    }

    @Override
    public String getLanguageForExtension(String extension) {
        if (".c".equals(extension) || ".i".equals(extension) || ".h".equals(extension)) {
            return "c";
        }
        return fallbackLanguageExtensionHandler.getLanguageForExtension(extension);
    }

    @Override
    public Set<String> getExtensionsForLanguage(String language) {
        if ("c".equals(language)) {
            return Set.of(".c", ".i", ".h");
        }
        return fallbackLanguageExtensionHandler.getExtensionsForLanguage(language);
    }

}
