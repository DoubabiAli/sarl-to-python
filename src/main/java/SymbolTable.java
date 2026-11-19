import java.util.Map;
import java.util.TreeMap;

/**
 * Table des symboles en chaîne de portées.
 * Une table par portée ; si un nom n'est pas trouvé ici, on cherche dans la portée englobante.
 * Ici : une portée globale qui contient les noms d'agents.
 */
public final class SymbolTable {

    public record Symbol(String lexeme, String kind, int line, int column) {
    }

    private final Map<String, Symbol> table = new TreeMap<>();
    private final SymbolTable enclosingEnvironment;

    public SymbolTable(SymbolTable enclosingEnvironment) {
        this.enclosingEnvironment = enclosingEnvironment;
    }

    public void declare(String identifier, Symbol symbol) {
        table.put(identifier, symbol);
    }

    /** Cherche le symbole dans cette portée puis dans les portées englobantes ; null si absent. */
    public Symbol get(String identifier) {
        for (SymbolTable e = this; e != null; e = e.enclosingEnvironment) {
            Symbol symbol = e.table.get(identifier);
            if (symbol != null) return symbol;
        }
        return null;
    }

    public SymbolTable openContext() {
        return new SymbolTable(this);
    }

    public SymbolTable closeContext() {
        return enclosingEnvironment != null ? enclosingEnvironment : this;
    }
}
