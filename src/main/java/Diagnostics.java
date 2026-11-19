import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Collecte toutes les erreurs de la transpilation. Une seule instance, partagée par toutes les étapes. */
public final class Diagnostics {

    /** Codes d'erreur communs (codes partagés par le groupe). On n'en invente pas d'autres sans prévenir le groupe. */
    public enum Code {
        SYNTAX_ERROR, UNEXPECTED_CHARACTER, MISSING_IDENTIFIER, MISSING_BRACE,
        UNSUPPORTED_FEATURE, DUPLICATE_DECLARATION, DUPLICATE_HANDLER, INVALID_HANDLER,
        MALFORMED_SPAWN, UNKNOWN_AGENT_TYPE
    }

    public record Entry(Code code, int line, int column, String message) {
        public String format() {
            return "ERREUR [" + code + "] ligne " + line + ", colonne " + column + " : " + message;
        }
    }

    private final List<Entry> entries = new ArrayList<>();

    public void report(Code code, int line, int column, String message) {
        entries.add(new Entry(code, line, column, message));
    }

    public boolean hasErrors() {
        return !entries.isEmpty();
    }

    public boolean has(Code code) {
        return entries.stream().anyMatch(e -> e.code() == code);
    }

    public List<Entry> all() {
        return Collections.unmodifiableList(entries);
    }
}
