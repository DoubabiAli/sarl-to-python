"""Runtime Python du MEMBRE 3 : démarrage d'un agent et événement Initialize.

IA : écrit avec l'aide de Claude, relu et expliqué par Ali (Membre 3).

Simplification Level 1 (documentée) : tout est SYNCHRONE. start() appelle le handler
directement, dans le même thread, et ne rend la main qu'une fois on_initialize terminé.
Il n'y a ni file d'attente d'événements ni thread par agent.
"""


class AgentState:
    CREATED = "CREATED"
    RUNNING = "RUNNING"
    STOPPING = "STOPPING"
    STOPPED = "STOPPED"


class Occurrence:
    """Événement Initialize : paramètres (dans l'ordre) + identifiant du parent (spawner)."""

    def __init__(self, parameters, spawner):
        self.parameters = list(parameters)  # ordre conservé
        self.spawner = spawner              # UUID du parent, ou None pour l'agent racine


def start(agent, params, spawner):
    """Passe l'agent à RUNNING, crée l'Occurrence et appelle on_initialize UNE seule fois."""
    agent.state = AgentState.RUNNING
    occurrence = Occurrence(params, spawner)
    handler = getattr(agent, "on_initialize", None)
    if handler is not None:  # un agent sans handler est valable
        handler(occurrence)
    return occurrence
