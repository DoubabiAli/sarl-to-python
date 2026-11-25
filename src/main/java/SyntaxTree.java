import java.util.List;

/**
 * L'arbre syntaxique : un petit objet par construction du langage.
 * Chaque nœud garde sa position (line, column) pour les messages d'erreur.
 *
 * Ce fichier est le CONTRAT du groupe : le Parser fabrique ces nœuds, le Semantic les vérifie,
 * le PythonGenerator les traduit. Si un membre doit changer un nœud, il prévient les autres.
 */
public final class SyntaxTree {

    private SyntaxTree() {
    }

    // ----- types de base
    public interface Node { int line(); int column(); }
    /** Une instruction dans un bloc. */
    public interface Statement extends Node { }
    /** Une valeur. */
    public interface Expression extends Node { }

    //  Represents a Boolean literal used in a guard.
    public record BooleanLiteral(
            boolean value,
            int line,
            int column
    ) implements Expression { }

    // ----- structure du fichier (Membre 1)
    public record ProgramNode(List<AgentNode> agents, int line, int column) implements Node { }

    /** members : dans l'ordre de déclaration (UsesNode, InitializeNode, DestroyNode...). */
    public record AgentNode(String name, List<Node> members, int line, int column) implements Node { }

    // ----- membres d'un agent
    /** uses Lifecycle, Logging  (Membre 2) */
    public record UsesNode(List<String> capabilities, int line, int column) implements Node { }
    /** on Initialize { ... }  (Membre 3) */
    public record InitializeNode(List<Statement> body, int line, int column) implements Node { }
    /** on Destroy { ... }  (Membre 4) */
    public record DestroyNode(  Expression guard,List<Statement> body, int line, int column) implements Node { }

    // ----- instructions
    /** info("...")  (commun) */
    public record InfoStatement(Expression message, int line, int column) implements Statement { }
    /** spawn(typeof(ChildAgent), arg1, arg2)  (Membre 2) : arguments dans l'ordre */
    public record SpawnStatement(String agentType, List<Expression> arguments, int line, int column)
            implements Statement { }
    /** killMe  (Membre 4) */

    public record KillMeStatement(int line, int column) implements Statement { }

    // ----- expressions
    public record StringLiteral(String value, int line, int column) implements Expression { }
    public record NumberLiteral(String text, int line, int column) implements Expression { }
    public record VariableRef(String name, int line, int column) implements Expression { }
}
