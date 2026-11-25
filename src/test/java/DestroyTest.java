import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
/**
 * Tests du MEMBRE 4 (semaine 1 : on Destroy). Retire @Disabled au fur et à mesure.
 * killMe viendra plus tard. Modèle : AgentTest.
 */
class DestroyTest {

    // TEST 1: Basic on Destroy
    @Test
    void destroyHandlerBecomesAMethod() {
        Transpiler.Result r = Transpiler.transpile(
                "agent A { on Destroy { info(\"bye\") } }"
        );

        assertTrue(r.success());
        assertTrue(r.python().contains(
                "def on_destroy(self, occurrence):"
        ));
        assertTrue(r.python().contains(
                "self.info(\"bye\")"
        ));
    }

    // TEST 2: Empty body
    @Test
    void emptyHandlerBodyBecomesPass() {
        Transpiler.Result r = Transpiler.transpile(
                "agent A { on Destroy { } }"
        );

        assertTrue(r.success());
        assertTrue(r.python().contains(
                "def on_destroy(self, occurrence):\n        pass"
        ));
    }

    // TEST 3: Agent without Destroy
    @Test
    void agentWithoutHandlerStillWorks() {
        Transpiler.Result r = Transpiler.transpile(
                "agent A {}"
        );

        assertTrue(r.success());
        assertTrue(r.python().contains(
                "class A(Agent):\n    pass"
        ));
    }

    // TEST 4: Guard [true]
    @Test
    void trueGuardGeneratesPythonCondition() {
        Transpiler.Result r = Transpiler.transpile(
                "agent A { on Destroy [true] { info(\"bye\") } }"
        );

        assertTrue(r.success());
        assertTrue(r.python().contains("if True:"));
    }

    // TEST 5: Guard [false]
    @Test
    void falseGuardGeneratesPythonCondition() {
        Transpiler.Result r = Transpiler.transpile(
                "agent A { on Destroy [false] { info(\"bye\") } }"
        );

        assertTrue(r.success());
        assertTrue(r.python().contains("if False:"));
    }

    // TEST 6: Empty guard is invalid
    @Test
    void emptyGuardGetsADiagnostic() {
        Transpiler.Result r = Transpiler.transpile(
                "agent A { on Destroy [] { } }"
        );

        assertFalse(r.success());
        assertTrue(r.has(Diagnostics.Code.SYNTAX_ERROR));
    }

    // TEST 7: Duplicate Destroy handlers
    @Test
    void twoDestroyHandlersGetADiagnostic() {
        Transpiler.Result r = Transpiler.transpile(
                "agent A { on Destroy {} on Destroy {} }"
        );

        assertFalse(r.success());
        assertTrue(r.has(
                Diagnostics.Code.DUPLICATE_HANDLER
        ));
    }

    // TEST 8: killMe translation
    // This tests code generation only, not runtime execution.
    @Test
    void killMeGeneratesStopCall() {
        Transpiler.Result r = Transpiler.transpile(
                "agent A { on Destroy { killMe } }"
        );

        assertTrue(r.success());
        assertTrue(r.python().contains(
                "self.runtime.stop(self)"
        ));
    }

    // TEST 9: Handler order
    @Test
    void initializeAndDestroyKeepTheirOrder() {
        Transpiler.Result r = Transpiler.transpile(
                "agent A { on Initialize {} on Destroy {} }"
        );

        assertTrue(r.success());

        assertTrue(
                r.python().indexOf("on_initialize")
                        < r.python().indexOf("on_destroy")
        );
    }
}

