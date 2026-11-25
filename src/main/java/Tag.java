/**
 * Nom d'un token.
 * Seuls les mots réservés, les classes de tokens (id, string, number) et la ponctuation ont un nom.
 */
public enum Tag {
    // mots réservés (keywords) : reconnus grâce à la table des mots-clés du Lexer
    AGENT, USES, ON, INITIALIZE, DESTROY, SPAWN, TYPEOF, KILLME, INFO,

    // classes de tokens avec attribut (le lexème)
    ID, STRING, NUMBER,

    // ponctuation
    LBRACE, RBRACE, LPAREN, RPAREN, LBRACKET, RBRACKET, COMMA, SEMICOLON,

    //boolean
    TRUE, FALSE,

    EOF
}
