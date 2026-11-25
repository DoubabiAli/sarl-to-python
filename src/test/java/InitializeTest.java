import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests du MEMBRE 3 (on Initialize).
 * IA : écrits avec l'aide de Claude, relus et expliqués par Ali.
 */
class InitializeTest {

    @Test
    void initializeHandlerBecomesAMethod() {
        Transpiler.Result r = Transpiler.transpile("agent A { on Initialize { info(\"hi\") } }");
        assertTrue(r.success());
        assertTrue(r.python().contains("def on_initialize(self, occurrence):\n        self.info(\"hi\")"));
    }

    @Test
    void emptyHandlerBodyBecomesPass() {
        Transpiler.Result r = Transpiler.transpile("agent A { on Initialize { } }");
        assertTrue(r.success());
        assertTrue(r.python().contains("def on_initialize(self, occurrence):\n        pass"));
    }

    @Test
    void agentWithoutHandlerStillWorks() {
        Transpiler.Result r = Transpiler.transpile("agent A { }");
        assertTrue(r.success());
        assertFalse(r.python().contains("on_initialize"));
    }

    @Test
    void guardedHandlerGetsADiagnostic() {
        Transpiler.Result r = Transpiler.transpile("agent A { on Initialize [x] { } }");
        assertFalse(r.success());
        assertEquals(Diagnostics.Code.UNSUPPORTED_FEATURE, r.errors().get(0).code());
    }

    @Test
    void twoInitializeHandlersGetADiagnostic() {
        Transpiler.Result r = Transpiler.transpile("agent A { on Initialize { } on Initialize { } }");
        assertFalse(r.success());
        assertEquals(Diagnostics.Code.DUPLICATE_HANDLER, r.errors().get(0).code());
    }
}
