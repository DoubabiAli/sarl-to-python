"""Tests Python du contrat Agent. Lancer : python3 -m unittest discover -s runtime"""
import unittest

from sarl_runtime import Agent, AgentState


class AgentContractTest(unittest.TestCase):
    def test_new_agent_is_created_with_an_id_and_no_runtime(self):
        a = Agent()
        self.assertEqual(a.state, AgentState.CREATED)
        self.assertIsNotNone(a.id)
        self.assertIsNone(a.runtime)

    def test_two_agents_have_different_ids(self):
        self.assertNotEqual(Agent().id, Agent().id)

    def test_generated_style_subclass_works(self):
        class StudentAgent(Agent):
            pass

        self.assertEqual(StudentAgent().state, AgentState.CREATED)


if __name__ == "__main__":
    unittest.main()
