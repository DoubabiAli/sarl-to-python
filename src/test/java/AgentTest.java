import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests du MEMBRE 1 (déclarations d'agents). Ces tests servent aussi de MODÈLE
 * aux autres membres : transpiler une chaîne SARL et regarder le résultat.
 */
class AgentTest {

    @Test
    void emptyAgentBecomesAClassWithPass() {
        Transpiler.Result r = Transpiler.transpile("agent StudentAgent {}");
        assertTrue(r.success());
        assertTrue(r.python().contains("class StudentAgent:\n    pass\n"));
    }

    @Test
    void severalAgentsGiveDistinctClasses() {
        Transpiler.Result r = Transpiler.transpile("agent A {}\nagent B {}");
        assertTrue(r.success());
        assertTrue(r.python().contains("class A:"));
        assertTrue(r.python().contains("class B:"));
    }

    @Test
    void missingNameIsRejectedWithALocation() {
        Transpiler.Result r = Transpiler.transpile("agent {}");
        assertFalse(r.success());
        Diagnostics.Entry e = r.errors().get(0);
        assertEquals(Diagnostics.Code.MISSING_IDENTIFIER, e.code());
        assertEquals(1, e.line());
    }

    @Test
    void missingClosingBraceIsRejected() {
        Transpiler.Result r = Transpiler.transpile("agent A {");
        assertFalse(r.success());
        assertTrue(r.has(Diagnostics.Code.MISSING_BRACE));
    }

    @Test
    void unsupportedMemberIsReportedNotDropped() {
        Transpiler.Result r = Transpiler.transpile("agent A {\n  var x = 1\n}");
        assertFalse(r.success());
        assertTrue(r.has(Diagnostics.Code.UNSUPPORTED_FEATURE));
    }

    @Test
    void unsupportedTopLevelDeclarationIsReported() {
        Transpiler.Result r = Transpiler.transpile("event Ping\nagent A {}");
        assertFalse(r.success());
        assertTrue(r.has(Diagnostics.Code.UNSUPPORTED_FEATURE));
    }

    @Test
    void duplicateAgentNamesAreRejected() {
        Transpiler.Result r = Transpiler.transpile("agent A {}\nagent A {}");
        assertFalse(r.success());
        assertTrue(r.has(Diagnostics.Code.DUPLICATE_DECLARATION));
    }

    @Test
    void parsingContinuesAfterAnErrorAndFindsTheNextAgentError() {
        Transpiler.Result r = Transpiler.transpile("agent {}\nagent {}");
        assertEquals(2, r.errors().size()); // mode panique : on resynchronise sur le mot 'agent'
    }
}
