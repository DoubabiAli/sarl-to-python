import java.util.ArrayList;
import java.util.List;

/**
 * Analyseur syntaxique par DESCENTE RÉCURSIVE :
 * une méthode par non-terminal de la grammaire (voir docs/GRAMMAIRE.md).
 * Chaque méthode lit ses tokens et RETOURNE le nœud de l'arbre correspondant
 *
 * Récupération d'erreurs : MODE PANIQUE. Après une erreur, on jette les
 * tokens jusqu'à un token de synchronisation = FIRST(A) ∪ FOLLOW(A) (voir synchronize()).
 *
 * Répartition : les méthodes marquées « MEMBRE n » sont à écrire par ce membre ;
 * le reste est commun. Ne touchez pas aux méthodes des autres.
 */
public final class Parser {

    /** Exception interne : l'erreur est DÉJÀ dans Diagnostics, elle sert juste à sortir de la méthode. */
    private static final class SyntaxError extends RuntimeException {
        private static final long serialVersionUID = 1L;

        SyntaxError() {
            super(null, null, false, false);
        }
    }

    private final List<Token> tokens;
    private final Diagnostics diag;
    private int pos = 0;

    public Parser(List<Token> tokens, Diagnostics diag) {
        this.tokens = tokens;
        this.diag = diag;
    }

    // =====================================================================================
    //  COMMUN / MEMBRE 1 : fichier et déclarations d'agents
    //  <program> → <agent> <program> | ε
    //  <agent>   → agent id { <members> }
    // =====================================================================================

    public SyntaxTree.ProgramNode parse() {
        Token first = peek();
        List<SyntaxTree.AgentNode> agents = new ArrayList<>();
        while (!check(Tag.EOF)) {
            if (check(Tag.AGENT)) {
                try {
                    agents.add(parseAgent());
                } catch (SyntaxError e) {
                    synchronize(Tag.AGENT); // FIRST(agent) ∪ FOLLOW(agent) = { agent, EOF }
                }
            } else {
                Token t = advance();
                diag.report(Diagnostics.Code.UNSUPPORTED_FEATURE, t.line(), t.column(),
                        "seules les déclarations 'agent' sont supportées au niveau du fichier (trouvé "
                                + describe(t) + ")");
                synchronize(Tag.AGENT);
            }
        }
        return new SyntaxTree.ProgramNode(agents, first.line(), first.column());
    }

    private SyntaxTree.AgentNode parseAgent() {
        Token keyword = expect(Tag.AGENT, Diagnostics.Code.SYNTAX_ERROR, "'agent' attendu");
        Token name = expect(Tag.ID, Diagnostics.Code.MISSING_IDENTIFIER,
                "nom d'agent attendu après 'agent'");
        Token open = expect(Tag.LBRACE, Diagnostics.Code.MISSING_BRACE,
                "'{' attendu après le nom de l'agent");

        List<SyntaxTree.Node> members = new ArrayList<>();
        while (!check(Tag.RBRACE) && !check(Tag.EOF)) {
            int start = pos;
            try {
                members.add(parseMember());
            } catch (SyntaxError e) {
                if (pos == start) advance(); // garantit qu'on avance (pas de boucle infinie)
                synchronize(Tag.ON, Tag.USES, Tag.RBRACE); // FIRST(member) ∪ FOLLOW(members)
            }
        }
        if (!match(Tag.RBRACE)) {
            diag.report(Diagnostics.Code.MISSING_BRACE, open.line(), open.column(),
                    "l'accolade '{' de l'agent " + name.lexeme() + " n'est jamais fermée");
            throw new SyntaxError();
        }
        return new SyntaxTree.AgentNode(name.lexeme(), members, keyword.line(), keyword.column());
    }

    // =====================================================================================
    //  COMMUN : aiguillage des membres d'un agent
    //  <member>  → uses id <ids> | on <handler>        (factorisation à gauche du « on »)
    //  <handler> → Initialize <block> | Destroy <block>
    // =====================================================================================

    private SyntaxTree.Node parseMember() {
        if (check(Tag.USES)) {
            return parseUses();
        }
        if (check(Tag.ON)) {
            Token on = advance();
            if (check(Tag.INITIALIZE)) {
                return parseInitialize(on);
            }
            if (check(Tag.DESTROY)) {
                return parseDestroy(on);
            }
            throw error(Diagnostics.Code.UNSUPPORTED_FEATURE,
                    "seuls les handlers 'on Initialize' et 'on Destroy' sont supportés");
        }
        throw error(Diagnostics.Code.UNSUPPORTED_FEATURE, "membre d'agent non supporté");
    }

    // =====================================================================================
    //  MEMBRE 2 : uses Lifecycle  et  spawn(...)
    //  <uses> → uses id <ids>            <ids>  → , id <ids> | ε
    //  <spawn> → spawn ( typeof ( id ) <args> )     <args> → , <expr> <args> | ε
    // =====================================================================================

    /** Le token courant est USES. Doit retourner un UsesNode. */
    private SyntaxTree.Node parseUses() {
        throw notImplemented("Membre 2", "uses Lifecycle");
    }

    /** Le token courant est SPAWN. Doit retourner un SpawnStatement. */
    private SyntaxTree.Statement parseSpawn() {
        throw notImplemented("Membre 2", "spawn(typeof(Agent), args...)");
    }

    // =====================================================================================
    //  MEMBRE 3 : on Initialize { ... }
    //  <handler> → Initialize <block>
    // =====================================================================================

    /**
     * Le token « on » vient d'être consommé ; le token courant est INITIALIZE.
     * Doit retourner un InitializeNode. Indices : consommer INITIALIZE, refuser un '[' (garde),
     * lire le corps avec parseBlock(), construire le nœud avec la position de « on ».
     */
    private SyntaxTree.Node parseInitialize(Token on) {
        // IA : écrit avec l'aide de Claude, relu et expliqué par Ali (Membre 3)
        advance(); // consomme INITIALIZE
        if (check(Tag.LBRACKET)) {
            // Level 1 : pas de garde « [condition] » sur on Initialize
            throw error(Diagnostics.Code.UNSUPPORTED_FEATURE,
                    "les handlers gardés [...] ne sont pas supportés");
        }
        List<SyntaxTree.Statement> body = parseBlock();
        return new SyntaxTree.InitializeNode(body, on.line(), on.column());
    }

    // =====================================================================================
    //  MEMBRE 4 : on Destroy { ... }  et  killMe
    //  <handler> → Destroy <block>        <killme> → killMe
    // =====================================================================================

    /** Le token « on » vient d'être consommé ; le token courant est DESTROY. */


// Parse a Boolean literal inside a guard.

    private SyntaxTree.Expression parseBooleanExpression() {

        if (check(Tag.TRUE)) {
            Token token = advance();
            return new SyntaxTree.BooleanLiteral(
                    true,
                    token.line(),
                    token.column()
            );
        }

        if (check(Tag.FALSE)) {
            Token token = advance();
            return new SyntaxTree.BooleanLiteral(
                    false,
                    token.line(),
                    token.column()
            );
        }

        throw error(
                Diagnostics.Code.SYNTAX_ERROR,
                "Expected a Boolean expression"
        );
    }



    private SyntaxTree.Node parseDestroy(Token on) {

        // The token "on" has already been consumed.
        // The current token is DESTROY.
        advance();

        // No guard by default
        SyntaxTree.Expression guard = null;

        // Optional guard: [condition]
        if (check(Tag.LBRACKET)) {

            advance(); // Consume [

            // Check for an empty guard []
            if (check(Tag.RBRACKET)) {
                throw error(
                        Diagnostics.Code.SYNTAX_ERROR,
                        "The guard cannot be empty"
                );
            }

            // Read the condition inside [ ... ]
            guard = parseBooleanExpression();

            // Require the closing ]
            if (!check(Tag.RBRACKET)) {
                throw error(
                        Diagnostics.Code.SYNTAX_ERROR,
                        "Missing ] after the guard"
                );
            }

            advance(); // Consume ]
        }

        // The handler body must begin with {
        if (!check(Tag.LBRACE)) {
            throw error(
                    Diagnostics.Code.SYNTAX_ERROR,
                    "Missing { after on Destroy"
            );
        }

        // Read the instructions between { and }
        List<SyntaxTree.Statement> body = parseBlock();

        // Construct the AST node
        return new SyntaxTree.DestroyNode(
                guard,
                body,
                on.line(),
                on.column()
        );
    }


    /** Le token courant est KILLME. Doit retourner un KillMeStatement. */

    private SyntaxTree.Statement parseKillMe() {

        // Consume the killMe keyword
        Token token = advance();

        // Optional semicolon
        if (check(Tag.SEMICOLON)) {
            advance();
        }

        // Create the AST node
        return new SyntaxTree.KillMeStatement(
                token.line(),
                token.column()
        );
    }


    // =====================================================================================
    //  COMMUN : blocs, instructions, expressions
    //  <block>      → { <statements> }
    //  <statements> → <statement> <statements> | ε
    //  <statement>  → <info> | <spawn> | <killme>
    //  <info>       → info ( <expr> )
    //  <expr>       → string | number | id
    // =====================================================================================

    /** Lit « { instruction* } » et retourne les instructions dans l'ordre. À utiliser pour tout corps de handler. */
    private List<SyntaxTree.Statement> parseBlock() {
        Token open = expect(Tag.LBRACE, Diagnostics.Code.MISSING_BRACE, "'{' attendu pour ouvrir un bloc");
        List<SyntaxTree.Statement> body = new ArrayList<>();
        while (!check(Tag.RBRACE) && !check(Tag.EOF)) {
            int start = pos;
            try {
                body.add(parseStatement());
            } catch (SyntaxError e) {
                if (pos == start) advance();
                synchronize(Tag.INFO, Tag.SPAWN, Tag.KILLME, Tag.RBRACE); // FIRST(statement) ∪ FOLLOW(statements)
            }
        }
        if (!match(Tag.RBRACE)) {
            diag.report(Diagnostics.Code.MISSING_BRACE, open.line(), open.column(),
                    "l'accolade '{' de ce bloc n'est jamais fermée");
            throw new SyntaxError();
        }
        return body;
    }

    private SyntaxTree.Statement parseStatement() {
        if (check(Tag.INFO)) return parseInfo();
        if (check(Tag.SPAWN)) return parseSpawn();
        if (check(Tag.KILLME)) return parseKillMe();
        throw error(Diagnostics.Code.UNSUPPORTED_FEATURE, "instruction non supportée");
    }

    private SyntaxTree.Statement parseInfo() {
        Token keyword = advance(); // info
        expect(Tag.LPAREN, Diagnostics.Code.SYNTAX_ERROR, "'(' attendu après 'info'");
        SyntaxTree.Expression message = parseExpression();
        expect(Tag.RPAREN, Diagnostics.Code.SYNTAX_ERROR, "')' attendu");
        match(Tag.SEMICOLON); // le ';' est facultatif en SARL
        return new SyntaxTree.InfoStatement(message, keyword.line(), keyword.column());
    }

    private SyntaxTree.Expression parseExpression() {
        Token t = peek();
        if (match(Tag.STRING)) return new SyntaxTree.StringLiteral(t.lexeme(), t.line(), t.column());
        if (match(Tag.NUMBER)) return new SyntaxTree.NumberLiteral(t.lexeme(), t.line(), t.column());
        if (match(Tag.ID)) return new SyntaxTree.VariableRef(t.lexeme(), t.line(), t.column());
        throw error(Diagnostics.Code.SYNTAX_ERROR, "expression attendue");
    }

    // =====================================================================================
    //  OUTILS COMMUNS (lecture des tokens, erreurs, mode panique)
    // =====================================================================================

    private Token peek() {
        return tokens.get(pos);
    }

    private Token advance() {
        Token t = tokens.get(pos);
        if (t.tag() != Tag.EOF) pos++;
        return t;
    }

    private boolean check(Tag tag) {
        return peek().tag() == tag;
    }

    private boolean match(Tag tag) {
        if (check(tag)) {
            advance();
            return true;
        }
        return false;
    }

    /** Consomme un token du type voulu, sinon signale l'erreur et sort de la méthode. */
    private Token expect(Tag tag, Diagnostics.Code code, String message) {
        if (check(tag)) return advance();
        throw error(code, message);
    }

    /** Signale une erreur à la position du token courant. Utilisation : throw error(...). */
    private SyntaxError error(Diagnostics.Code code, String message) {
        Token t = peek();
        diag.report(code, t.line(), t.column(), message + " (trouvé " + describe(t) + ")");
        return new SyntaxError();
    }

    private SyntaxError notImplemented(String who, String what) {
        return error(Diagnostics.Code.UNSUPPORTED_FEATURE, "pas encore implémenté : " + what + " [TODO " + who + "]");
    }

    /**
     * MODE PANIQUE : jette les tokens jusqu'à un token de l'ensemble de
     * synchronisation. Les blocs { ... } sont sautés d'un seul coup pour ne pas s'arrêter
     * sur une accolade fermante interne.
     */
    private void synchronize(Tag... syncSet) {
        while (!check(Tag.EOF)) {
            for (Tag tag : syncSet) {
                if (check(tag)) return;
            }
            if (check(Tag.LBRACE)) skipBlock();
            else advance();
        }
    }

    private void skipBlock() {
        advance(); // {
        int depth = 1;
        while (depth > 0 && !check(Tag.EOF)) {
            Token t = advance();
            if (t.tag() == Tag.LBRACE) depth++;
            else if (t.tag() == Tag.RBRACE) depth--;
        }
    }

    private static String describe(Token t) {
        return t.tag() == Tag.EOF ? "fin de fichier" : "'" + t.lexeme() + "'";
    }
}
