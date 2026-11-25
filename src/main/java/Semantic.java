/**
 * Analyse sémantique.
 * La syntaxe est correcte ; on vérifie maintenant le SENS : noms déclarés, doublons...
 * On le fait en 2 passes pour qu'un agent puisse en créer un autre déclaré plus bas dans le fichier.
 */
public final class Semantic {

    private final Diagnostics diag;

    public Semantic(Diagnostics diag) {
        this.diag = diag;
    }

    public void analyze(SyntaxTree.ProgramNode program) {
        SymbolTable global = new SymbolTable(null);

        // Passe 1 (MEMBRE 1) : déclarer tous les agents
        for (SyntaxTree.AgentNode agent : program.agents()) {
            if (global.get(agent.name()) != null) {
                diag.report(Diagnostics.Code.DUPLICATE_DECLARATION, agent.line(), agent.column(),
                        "l'agent " + agent.name() + " est déclaré plusieurs fois");
            } else {
                global.declare(agent.name(),
                        new SymbolTable.Symbol(agent.name(), "agent", agent.line(), agent.column()));
            }
        }

        // Passe 2 : vérifier le contenu de chaque agent
        for (SyntaxTree.AgentNode agent : program.agents()) {
            checkAgent(agent, global);
        }
    }

    private void checkAgent(SyntaxTree.AgentNode agent, SymbolTable global) {
        // MEMBRE 3 : au plus UN handler on Initialize par agent (sinon DUPLICATE_HANDLER)
        // MEMBRE 4 : au plus UN handler on Destroy par agent (sinon DUPLICATE_HANDLER)
        checkDestroy(agent);
        // MEMBRE 2 : dans chaque corps de handler, spawn(typeof(X)) doit viser un agent déclaré
        //            (sinon UNKNOWN_AGENT_TYPE) : utiliser global.get("X")
    }



    // member 4

    private void checkDestroy(SyntaxTree.AgentNode agent) {
        // Detect duplicate on Destroy handlers.
        boolean found = false;
        for (SyntaxTree.Node member : agent.members()) {
            if (member instanceof SyntaxTree.DestroyNode destroy) {
                if (found) {
                    diag.report(
                            Diagnostics.Code.DUPLICATE_HANDLER,
                            destroy.line(),
                            destroy.column(),
                            "on Destroy is declared more than once in agent "
                                    + agent.name()
                    );
                } else {
                    found = true;
                }
            }
        }
    }

}
