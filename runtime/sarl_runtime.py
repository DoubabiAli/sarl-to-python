"""Runtime Python minimal partagé (stub du Membre 1).

Le code généré commence par « from sarl_runtime import Agent » et chaque agent SARL devient
« class Nom(Agent): ». Ce fichier fixe le contrat commun entre les membres :
  - id      : identifiant unique de l'agent (assigné ici, à la création)
  - state   : CREATED -> RUNNING -> STOPPING -> STOPPED
  - runtime : objet fourni par le runtime (spawn / stop), None tant qu'il n'est pas attaché
Les méthodes spawn / stop du runtime sont à la charge des Membres 2 et 4.
"""
import uuid


class AgentState:
    CREATED = "CREATED"
    RUNNING = "RUNNING"
    STOPPING = "STOPPING"
    STOPPED = "STOPPED"


class Agent:
    """Classe de base de tous les agents générés."""

    def __init__(self, runtime=None):
        self.id = uuid.uuid4()
        self.state = AgentState.CREATED
        self.runtime = runtime

    def info(self, message):
        """Traduction de info(...) de SARL."""
        print(message)
