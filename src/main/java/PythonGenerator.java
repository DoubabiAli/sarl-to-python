import java.util.List;

/**
 * Générateur de code : parcourt l'arbre syntaxique et écrit le Python.
 * Les parseurs n'écrivent JAMAIS de Python ; ce fichier ne lit JAMAIS de tokens.
 *
 * Écrire le Python UNIQUEMENT avec line(...) : l'indentation est gérée par indent/dedent.
 * Les méthodes marquées « MEMBRE n » sont à écrire par ce membre.
 */
public final class PythonGenerator {

    private final Diagnostics diag;
    private final StringBuilder out = new StringBuilder();
    private int indent = 0;

    public PythonGenerator(Diagnostics diag) {
        this.diag = diag;
    }

    public String generate(SyntaxTree.ProgramNode program) {
        line("# Genere par le transpileur SARL -> Python. Ne pas modifier a la main.");
        for (SyntaxTree.AgentNode agent : program.agents()) {
            generateAgent(agent);
            line("");
            line("");
        }
        return out.toString();
    }

    // =====================================================================================
    //  COMMUN / MEMBRE 1 : classe d'un agent
    // =====================================================================================

    private void generateAgent(SyntaxTree.AgentNode agent) {
        line("class " + agent.name() + ":");
        indent++;
        int bodyStart = out.length();
        for (SyntaxTree.Node member : agent.members()) {
            int position = out.length();
            generateMember(member);
            boolean produced = out.length() > position;
            if (produced && position > bodyStart) {
                out.insert(position, "\n"); // ligne vide entre deux méthodes
            }
        }
        if (out.length() == bodyStart) {
            line("pass"); // agent vide
        }
        indent--;
    }

    private void generateMember(SyntaxTree.Node member) {
        if (member instanceof SyntaxTree.UsesNode) {
            // « uses Lifecycle » ne produit pas de code Python
        } else if (member instanceof SyntaxTree.InitializeNode node) {
            generateInitialize(node);
        } else if (member instanceof SyntaxTree.DestroyNode node) {
            generateDestroy(node);
        } else {
            diag.report(Diagnostics.Code.UNSUPPORTED_FEATURE, member.line(), member.column(),
                    "pas de génération Python pour " + member.getClass().getSimpleName());
        }
    }

    // =====================================================================================
    //  MEMBRE 3 : def on_initialize(self, occurrence):
    // =====================================================================================

    /** Doit écrire « def on_initialize(self, occurrence): » puis le corps (voir generateBlock). */
    private void generateInitialize(SyntaxTree.InitializeNode node) {
        diag.report(Diagnostics.Code.UNSUPPORTED_FEATURE, node.line(), node.column(),
                "génération de on Initialize non implémentée [TODO Membre 3]");
    }

    // =====================================================================================
    //  MEMBRE 4 : def on_destroy(self, occurrence):  et  killMe
    // =====================================================================================

    private void generateDestroy(SyntaxTree.DestroyNode node) {
        diag.report(Diagnostics.Code.UNSUPPORTED_FEATURE, node.line(), node.column(),
                "génération de on Destroy non implémentée [TODO Membre 4]");
    }

    /** killMe -> self.runtime.stop(self) */
    private void generateKillMe(SyntaxTree.KillMeStatement statement) {
        diag.report(Diagnostics.Code.UNSUPPORTED_FEATURE, statement.line(), statement.column(),
                "génération de killMe non implémentée [TODO Membre 4]");
    }

    // =====================================================================================
    //  MEMBRE 2 : spawn -> self.runtime.spawn(ChildAgent, *args, spawner=self)
    // =====================================================================================

    private void generateSpawn(SyntaxTree.SpawnStatement statement) {
        diag.report(Diagnostics.Code.UNSUPPORTED_FEATURE, statement.line(), statement.column(),
                "génération de spawn non implémentée [TODO Membre 2]");
    }

    // =====================================================================================
    //  COMMUN : blocs, instructions, expressions
    // =====================================================================================

    /** Écrit les instructions d'un corps de méthode ; un corps vide devient « pass ». */
    private void generateBlock(List<SyntaxTree.Statement> body) {
        if (body.isEmpty()) {
            line("pass");
            return;
        }
        for (SyntaxTree.Statement statement : body) {
            generateStatement(statement);
        }
    }

    private void generateStatement(SyntaxTree.Statement statement) {
        if (statement instanceof SyntaxTree.InfoStatement info) {
            line("self.info(" + expression(info.message()) + ")");
        } else if (statement instanceof SyntaxTree.SpawnStatement spawn) {
            generateSpawn(spawn);
        } else if (statement instanceof SyntaxTree.KillMeStatement killMe) {
            generateKillMe(killMe);
        } else {
            diag.report(Diagnostics.Code.UNSUPPORTED_FEATURE, statement.line(), statement.column(),
                    "pas de génération Python pour " + statement.getClass().getSimpleName());
        }
    }

    /** Retourne le texte Python d'une expression. */
    private String expression(SyntaxTree.Expression expression) {
        if (expression instanceof SyntaxTree.StringLiteral s) return pythonString(s.value());
        if (expression instanceof SyntaxTree.NumberLiteral n) return n.text();
        if (expression instanceof SyntaxTree.VariableRef v) return v.name();
        diag.report(Diagnostics.Code.UNSUPPORTED_FEATURE, expression.line(), expression.column(),
                "pas de génération Python pour " + expression.getClass().getSimpleName());
        return "None";
    }

    private static String pythonString(String value) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    /** Écrit une ligne de Python à l'indentation courante (4 espaces par niveau). */
    private void line(String text) {
        if (!text.isEmpty()) {
            out.append("    ".repeat(indent)).append(text);
        }
        out.append('\n');
    }
}
