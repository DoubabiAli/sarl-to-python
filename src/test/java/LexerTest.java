import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LexerTest {

    private static List<Token> lex(String source, Diagnostics diag) {
        return new Lexer(source, diag).tokenize();
    }

    private static List<Tag> tags(String source) {
        return lex(source, new Diagnostics()).stream().map(Token::tag).toList();
    }

    @Test
    void agentDeclaration() {
        assertEquals(List.of(Tag.AGENT, Tag.ID, Tag.LBRACE, Tag.RBRACE, Tag.EOF), tags("agent A {}"));
    }

    @Test
    void keywordsComeFromTheKeywordTable() {
        assertEquals(List.of(Tag.ON, Tag.INITIALIZE, Tag.DESTROY, Tag.SPAWN, Tag.TYPEOF, Tag.KILLME, Tag.INFO,
                Tag.USES, Tag.ID, Tag.EOF), tags("on Initialize Destroy spawn typeof killMe info uses Foo"));
    }

    @Test
    void positionsStartAtOne() {
        List<Token> tokens = lex("agent A {\n  on", new Diagnostics());
        Token on = tokens.get(3);
        assertEquals(Tag.ON, on.tag());
        assertEquals(2, on.line());
        assertEquals(3, on.column());
    }

    @Test
    void stringAttributeHasNoQuotesAndEscapesAreResolved() {
        Token t = lex("\"a\\nb\" 'x'", new Diagnostics()).get(0);
        assertEquals(Tag.STRING, t.tag());
        assertEquals("a\nb", t.lexeme());
    }

    @Test
    void numbers() {
        List<Token> tokens = lex("20 3.14", new Diagnostics());
        assertEquals("20", tokens.get(0).lexeme());
        assertEquals("3.14", tokens.get(1).lexeme());
    }

    @Test
    void commentsAreSkipped() {
        assertEquals(List.of(Tag.AGENT, Tag.EOF), tags("// ligne\n/* bloc\n sur 2 lignes */ agent"));
    }

    @Test
    void unexpectedCharacterIsReportedAndScanningContinues() {
        Diagnostics diag = new Diagnostics();
        List<Token> tokens = lex("agent # A", diag);
        assertTrue(diag.has(Diagnostics.Code.UNEXPECTED_CHARACTER));
        assertEquals(List.of(Tag.AGENT, Tag.ID, Tag.EOF), tokens.stream().map(Token::tag).toList());
    }

    @Test
    void unterminatedString() {
        Diagnostics diag = new Diagnostics();
        lex("\"abc", diag);
        assertTrue(diag.has(Diagnostics.Code.SYNTAX_ERROR));
    }
}
