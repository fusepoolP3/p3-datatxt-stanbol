package eu.spaziodati.datatxt.stanbol.enhancer.engines.client;

public class UnmanagedLanguageException extends DatatxtException {

    private static final long serialVersionUID = -7071074265611204111L;
    
    private final String language;

    public UnmanagedLanguageException(String language) {
        super(String.format("Unmanaged language'%s'", language), null);
        this.language = language;
    }

    public String getLanguage() {
        return language;
    }
}
