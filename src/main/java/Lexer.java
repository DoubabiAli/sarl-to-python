import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Lexer {

    /** Table des mots réservés. */
    private static final Map<String, Tag> KEYWORDS = Map.of(
            "agent", Tag.AGENT, "uses", Tag.USES, "on", Tag.ON,
            "Initialize", Tag.INITIALIZE, "Destroy", Tag.DESTROY,
            "spawn", Tag.SPAWN, "typeof", Tag.TYPEOF, "killMe", Tag.KILLME, "info", Tag.INFO);

    /** Valeur spéciale : une erreur a déjà été signalée et le pointeur a déjà avancé. */
    private static final Token SKIPPED = new Token(Tag.EOF, "", 0, 0);

    private final String src;
    private final Diagnostics diag;
    private final List<Token> tokens = new ArrayList<>();
    private int lexemeBegin = 0;
    private int forward = 0;
    private int line = 1;
    private int lineStart = 0; // indice du premier caractère de la ligne courante

    public Lexer(String source, Diagnostics diag) {
        this.src = source;
        this.diag = diag;
    }

    /** Retourne tous les tokens ; la liste se termine toujours par un token EOF. */
    public List<Token> tokenize() {
        while (true) {
            skipBlanksAndComments();
            lexemeBegin = forward;
            if (forward >= src.length()) {
                tokens.add(new Token(Tag.EOF, "", line, column()));
                return tokens;
            }
            Token token = getIdentifier();
            if (token == null) token = getNumber();
            if (token == null) token = getString();
            if (token == null) token = getSymbol();
            if (token == SKIPPED) {
                continue;
            }
            if (token != null) {
                tokens.add(token);
            } else {
                // aucun diagramme ne reconnaît ce caractère : mode panique
                diag.report(Diagnostics.Code.UNEXPECTED_CHARACTER, line, column(),
                        "caractère inattendu '" + src.charAt(lexemeBegin) + "'");
                forward = lexemeBegin + 1;
            }
        }
    }

    // ------------------------------------------------------------------ diagrammes de transition

    /** Identifiants et mots réservés : lettre (lettre | chiffre)*  (diagramme de transition). */
    private Token getIdentifier() {
        int state = 0;
        while (true) {
            char c;
            switch (state) {
                case 0:
                    c = nextChar();
                    if (isLetter(c)) {
                        state = 1;
                    } else {
                        forward = lexemeBegin;
                        return null; // ce n'est pas un identifiant
                    }
                    break;
                case 1:
                    c = nextChar();
                    state = (isLetter(c) || Character.isDigit(c)) ? 1 : 2;
                    break;
                case 2:
                    retract(); // le dernier caractère lu n'appartient pas au lexème
                    String lexeme = lexeme();
                    return new Token(KEYWORDS.getOrDefault(lexeme, Tag.ID), lexeme, line, column());
                default:
                    return null;
            }
        }
    }

    /** Nombres : chiffre+ ( . chiffre+ )? */
    private Token getNumber() {
        int state = 0;
        while (true) {
            char c;
            switch (state) {
                case 0:
                    c = nextChar();
                    if (Character.isDigit(c)) {
                        state = 1;
                    } else {
                        forward = lexemeBegin;
                        return null;
                    }
                    break;
                case 1:
                    c = nextChar();
                    if (Character.isDigit(c)) state = 1;
                    else if (c == '.') state = 2;
                    else state = 4;
                    break;
                case 2:
                    c = nextChar();
                    if (Character.isDigit(c)) {
                        state = 3;
                    } else {
                        forward -= 2; // "12." : le point n'appartient pas au nombre
                        return new Token(Tag.NUMBER, lexeme(), line, column());
                    }
                    break;
                case 3:
                    c = nextChar();
                    state = Character.isDigit(c) ? 3 : 4;
                    break;
                case 4:
                    retract();
                    return new Token(Tag.NUMBER, lexeme(), line, column());
                default:
                    return null;
            }
        }
    }

    /** Chaînes : "..." ou '...' avec \n \t \r \\ \" comme échappements. */
    private Token getString() {
        int state = 0;
        char quote = 0;
        StringBuilder value = new StringBuilder();
        while (true) {
            char c;
            switch (state) {
                case 0:
                    c = nextChar();
                    if (c == '"' || c == '\'') {
                        quote = c;
                        state = 1;
                    } else {
                        forward = lexemeBegin;
                        return null;
                    }
                    break;
                case 1:
                    c = nextChar();
                    if (forward > src.length() || c == '\n') return unterminatedString();
                    if (c == quote) state = 3;
                    else if (c == '\\') state = 2;
                    else value.append(c);
                    break;
                case 2:
                    c = nextChar();
                    if (forward > src.length() || c == '\n') return unterminatedString();
                    value.append(c == 'n' ? '\n' : c == 't' ? '\t' : c == 'r' ? '\r' : c);
                    state = 1;
                    break;
                case 3:
                    return new Token(Tag.STRING, value.toString(), line, column());
                default:
                    return null;
            }
        }
    }

    private Token unterminatedString() {
        diag.report(Diagnostics.Code.SYNTAX_ERROR, line, column(), "chaîne de caractères non terminée");
        if (forward > src.length()) {
            forward = src.length();
        } else if (src.charAt(forward - 1) == '\n') {
            forward--;
        }
        while (forward < src.length() && src.charAt(forward) != '\n') forward++; // mode panique
        return SKIPPED;
    }

    /** Ponctuation : un seul caractère. */
    private Token getSymbol() {
        char c = nextChar();
        Tag tag = switch (c) {
            case '{' -> Tag.LBRACE;
            case '}' -> Tag.RBRACE;
            case '(' -> Tag.LPAREN;
            case ')' -> Tag.RPAREN;
            case '[' -> Tag.LBRACKET;
            case ']' -> Tag.RBRACKET;
            case ',' -> Tag.COMMA;
            case ';' -> Tag.SEMICOLON;
            default -> null;
        };
        if (tag == null) {
            forward = lexemeBegin;
            return null;
        }
        return new Token(tag, String.valueOf(c), line, column());
    }

    // ------------------------------------------------------------------ espaces et commentaires

    private void skipBlanksAndComments() {
        while (forward < src.length()) {
            char c = src.charAt(forward);
            if (c == '\n') {
                forward++;
                line++;
                lineStart = forward;
            } else if (Character.isWhitespace(c)) {
                forward++;
            } else if (c == '/' && charAt(forward + 1) == '/') {
                while (forward < src.length() && src.charAt(forward) != '\n') forward++;
            } else if (c == '/' && charAt(forward + 1) == '*') {
                skipBlockComment();
            } else {
                return;
            }
        }
    }

    private void skipBlockComment() {
        int startLine = line;
        int startColumn = forward - lineStart + 1;
        forward += 2;
        while (forward < src.length() && !(charAt(forward) == '*' && charAt(forward + 1) == '/')) {
            if (src.charAt(forward) == '\n') {
                line++;
                lineStart = forward + 1;
            }
            forward++;
        }
        if (forward >= src.length()) {
            diag.report(Diagnostics.Code.SYNTAX_ERROR, startLine, startColumn, "commentaire non terminé");
        } else {
            forward += 2;
        }
    }

    // ------------------------------------------------------------------ outils de lecture

    /** Lit le caractère suivant et avance forward. Retourne '\0' à la fin du fichier. */
    private char nextChar() {
        char c = charAt(forward);
        forward++;
        return c;
    }

    /** Recule forward d'un caractère. */
    private void retract() {
        forward--;
    }

    /** Le lexème courant : entre lexemeBegin et forward. */
    private String lexeme() {
        return src.substring(lexemeBegin, forward);
    }

    private int column() {
        return lexemeBegin - lineStart + 1;
    }

    private char charAt(int i) {
        return i < src.length() ? src.charAt(i) : '\0';
    }

    private static boolean isLetter(char c) {
        return Character.isLetter(c) || c == '_' || c == '$';
    }
}
