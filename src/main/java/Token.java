/**
 * Un token = un nom (tag) + un attribut (lexeme).
 * Pour un STRING, le lexeme est le contenu SANS les guillemets (échappements déjà traités).
 * line et column commencent à 1 et servent aux messages d'erreur.
 */
public record Token(Tag tag, String lexeme, int line, int column) {
}
