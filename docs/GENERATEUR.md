# Le générateur de code (`PythonGenerator.java`)

Notion classique : génération de code.

## 1. À quoi ça sert

Il **parcourt l'arbre** (déjà correct) et écrit le texte Python. Règles d'or :
- il ne lit **jamais** de tokens (c'est le travail du Parser) ;
- il écrit le Python **uniquement avec `line(...)`**.

## 2. Les outils déjà écrits

| Outil | Rôle |
|---|---|
| `line("texte")` | écrit une ligne avec l'indentation courante (4 espaces par niveau) |
| `indent++` / `indent--` | entre dans un bloc (`class`, `def`) / en sort |
| `generateBlock(body)` | écrit les instructions d'un corps de méthode ; un corps vide devient `pass` |
| `generateStatement(s)` | aiguille une instruction (`info`, `spawn`, `killMe`) vers sa méthode |
| `expression(e)` | retourne le texte Python d'une expression (`"Max"`, `20`, `x`) |
| `pythonString(texte)` | met un texte entre guillemets et protège `\`, `"`, retour à la ligne... |

Exemple d'utilisation de l'indentation :

```java
line("def on_initialize(self, occurrence):");
indent++;
generateBlock(node.body());
indent--;
```

## 3. Le parcours

```text
generate(program)
  pour chaque agent : generateAgent(agent)
      line("class Nom:")  puis indent++
      pour chaque membre : generateMember(member)
            UsesNode        -> rien (pas de code Python)
            InitializeNode  -> generateInitialize(...)   (Membre 3)
            DestroyNode     -> generateDestroy(...)      (Membre 4)
            autre           -> erreur UNSUPPORTED_FEATURE
      si rien n'a été écrit : line("pass")             (agent vide)
```

Une ligne vide est insérée automatiquement entre deux méthodes d'une même classe.

## 4. Ce qu'on doit obtenir (cible)

SARL :

```sarl
agent ParentAgent {
    uses Lifecycle
    on Initialize {
        info("Parent started")
        spawn(typeof(ChildAgent), "Max", 20)
    }
    on Destroy {
        info("Parent stopped")
    }
}
```

Python attendu (aux détails de nom près, selon ce que le groupe décide) :

```python
class ParentAgent:
    def on_initialize(self, occurrence):
        self.info("Parent started")
        self.runtime.spawn(ChildAgent, "Max", 20, spawner=self)

    def on_destroy(self, occurrence):
        self.info("Parent stopped")
```

| Élément SARL | Python | Méthode | Qui |
|---|---|---|---|
| `agent A { }` | `class A:` + `pass` si vide | `generateAgent` | Membre 1 (fait) |
| `info(x)` | `self.info(x)` | `generateStatement` | commun (fait) |
| `on Initialize { ... }` | `def on_initialize(self, occurrence):` | `generateInitialize` | Membre 3 (cette semaine) |
| `on Destroy { ... }` | `def on_destroy(self, occurrence):` | `generateDestroy` | Membre 4 (cette semaine) |
| `killMe` | `self.runtime.stop(self)` | `generateKillMe` | Membre 4 (plus tard) |
| `spawn(typeof(B), a, b)` | `self.runtime.spawn(B, a, b, spawner=self)` | `generateSpawn` | Membre 2 (plus tard) |
| `uses Lifecycle` | rien | `generateMember` | (fait) |

Question ouverte du groupe : la liste de tâches parle de `class A(Agent):` avec une classe de base
`Agent` fournie par un module Python. Aujourd'hui le générateur écrit `class A:` sans classe de base
ni runtime (le projet ne fait pas tourner le Python). À décider ensemble.

## 5. Les erreurs du générateur

Si un nœud n'est pas géré, on écrit `diag.report(Diagnostics.Code.UNSUPPORTED_FEATURE, ...)` : on
n'ignore jamais un nœud en silence. Tant que les méthodes `generateInitialize`, `generateDestroy`,
`generateKillMe` et `generateSpawn` signalent « non implémenté », la transpilation échoue : c'est
voulu, pour ne pas produire un Python incomplet.
