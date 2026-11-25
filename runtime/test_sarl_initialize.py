"""Tests Python du Membre 3. Lancer : python3 -m unittest discover -s runtime
IA : écrits avec l'aide de Claude, relus et expliqués par Ali."""
import unittest
import uuid

from sarl_initialize import AgentState, start


class Recorder:
    def __init__(self):
        self.calls = []
        self.state = AgentState.CREATED

    def on_initialize(self, occurrence):
        self.calls.append(occurrence)


class NoHandler:
    state = AgentState.CREATED


class StartTest(unittest.TestCase):
    def test_handler_called_exactly_once_and_agent_running(self):
        a = Recorder()
        start(a, [], None)
        self.assertEqual(len(a.calls), 1)
        self.assertEqual(a.state, AgentState.RUNNING)

    def test_parameters_keep_their_order(self):
        a = Recorder()
        start(a, ["Max", 20, "x"], None)
        self.assertEqual(a.calls[0].parameters, ["Max", 20, "x"])

    def test_spawner_id_is_passed(self):
        a, parent = Recorder(), uuid.uuid4()
        start(a, [], parent)
        self.assertEqual(a.calls[0].spawner, parent)

    def test_root_agent_has_no_spawner(self):
        a = Recorder()
        start(a, [], None)
        self.assertIsNone(a.calls[0].spawner)

    def test_agent_without_handler_is_fine(self):
        a = NoHandler()
        start(a, [1], None)
        self.assertEqual(a.state, AgentState.RUNNING)


if __name__ == "__main__":
    unittest.main()
