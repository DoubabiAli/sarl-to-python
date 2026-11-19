# README Membre 2 : `uses Lifecycle` et `spawn` (prévu plus tard)

**Ces fonctionnalités ne sont pas à implémenter cette semaine.** Cette semaine, le groupe fait
seulement `on Initialize` (Membre 3) et `on Destroy` (Membre 4). Ton travail cette semaine est de
**te préparer** pour ne pas perdre de temps ensuite, et d'aider.

Ta tâche finale :

```sarl
uses Lifecycle
...
spawn(typeof(ChildAgent), "Max", 20)      // un agent en crée un autre
```

doit devenir, dans le Python :

```python
self.runtime.spawn(ChildAgent, "Max", 20, spawner=self)
```

---

## Étape 0 : préparer ton poste (15 min)

JDK 25, IntelliJ, dossier ouvert (voir `README.md`, §4). Lance `AgentTest` : tous verts. Branche :
`git checkout -b membre2-spawn` (à créer quand tu commenceras vraiment).

## Étape 1 : comprendre le projet (1 h 30)

| # | Fichier | À comprendre |
|---|---|---|
| 1 | `README.md` | le pipeline en 4 étapes |
| 2 | `docs/EXPLICATION.md` | un exemple complet |
| 3 | `docs/GRAMMAIRE.md` | les règles `<membre> -> uses ...` et `<instruction> -> spawn (...)` |
| 4 | `docs/ARBRE.md` | les nœuds `UsesNode(capabilities)` et `SpawnStatement(agentType, arguments)` |
| 5 | `docs/PARSER.md` | comment s'écrit une méthode `parseXxx` |
| 6 | `docs/GENERATEUR.md` | `line()`, `expression(...)` |
| 7 | `docs/SEMANTIC.md` | la table des symboles : c'est **ta** partie importante |

Lis aussi `docs/membres/README_MEMBRE_3.md` : c'est le même type de travail (Parser, Générateur,
Semantic, tests) sur un exemple plus simple. Quand tu l'auras compris, ta tâche sera facile à suivre.

## Étape 2 : comprendre ta tâche (1 h)

Ta tâche touche trois fichiers et un test :

| Ordre | Fichier | Méthode | Ce qu'elle fait |
|---|---|---|---|
| 1 | `Parser.java` | `parseUses()` | lit `uses` suivi d'un ou plusieurs noms séparés par des virgules |
| 2 | `Parser.java` | `parseSpawn()` | lit `spawn(typeof(Agent), arg1, arg2, ...)` |
| 3 | `PythonGenerator.java` | `generateSpawn(...)` | écrit `self.runtime.spawn(Agent, args, spawner=self)` |
| 4 | `Semantic.java` | `checkSpawn(agent, global)` (à créer) | vérifie que l'agent visé par `spawn` existe |
| 5 | `SpawnTest.java` | 5 tests (déjà listés) | vérifient tout ça |

Pourquoi le Semantic a besoin d'un travail à part : `spawn(typeof(ChildAgent))` n'est valide que si
`ChildAgent` est déclaré quelque part dans le fichier, même **plus bas**. C'est pour ça que
`Semantic.analyze` fait deux passes : la première déclare tous les agents dans la `SymbolTable`, la
seconde peut donc faire `global.get("ChildAgent")`.

**Une chose à vérifier avant de coder** : la syntaxe exacte de `spawn` en SARL, sur
https://www.sarl.io (et `docs/GUIDE_MEMBRES.md`, §7). Écris ce que tu as trouvé en haut de
`docs/GRAMMAIRE.md` pour que tout le groupe le voie.

## Étape 3 : ce que tu fais cette semaine (préparation)

1. **Aide les Membres 3 et 4** : relis leurs Pull Requests.
2. **Écris tes tests à l'avance** dans `SpawnTest.java`, sans les activer. C'est la bonne habitude :
   d'abord dire ce qu'on veut obtenir, puis écrire le code.
   - `spawningAKnownAgentProducesTheRightCall` : un agent qui crée un autre produit
     `self.runtime.spawn(ChildAgent, "Max", 20, spawner=self)`.
   - `argumentsRemainInOrder` : les arguments sortent dans l'ordre d'écriture.
   - `unknownAgentTypeIsRejected` : `UNKNOWN_AGENT_TYPE`.
   - `malformedSpawnIsRejected` : `spawn(` sans fermeture donne `MALFORMED_SPAWN`.
   - `unsupportedSpawnFormIsReported` : une variante non supportée donne `UNSUPPORTED_FEATURE`.
3. **Prépare le terrain** : dessine sur papier l'arbre de
   `agent P { on Initialize { spawn(typeof(C), "Max", 20) } } agent C {}` pour savoir ce que ton
   `parseSpawn` doit retourner : `SpawnStatement("C", [StringLiteral "Max", NumberLiteral "20"], ligne, colonne)`.

## Étape 4 : quand ce sera ton tour (après Initialize et Destroy)

Attends que les Membres 3 et 4 aient fusionné, puis :

1. **`parseUses`** : consommer `uses`, lire un `ID`, puis tant qu'il y a une virgule, un autre `ID` ;
   retourner un `UsesNode`.
2. **`parseSpawn`** : consommer `spawn`, `(`, `typeof`, `(`, un `ID`, `)`, puis tant qu'il y a une
   virgule un argument avec `parseExpression()`, puis `)`. Retourner un `SpawnStatement`. Utilise les
   codes `MALFORMED_SPAWN` pour les erreurs de forme.
3. **`generateSpawn`** : `line("self.runtime.spawn(" + type + ", " + arguments + ", spawner=self)")`.
4. **`checkSpawn`** : parcourir les `InitializeNode` et `DestroyNode`, puis leurs instructions, et
   pour chaque `SpawnStatement` faire `global.get(type) == null` pour signaler `UNKNOWN_AGENT_TYPE`.
5. Activer les 5 tests, lancer `mvn test`, pousser.

Rappel : dans `Semantic.checkAgent`, ajoute **une seule ligne** (`checkSpawn(agent, global);`) pour
éviter les conflits Git avec les Membres 3 et 4.

## Checklist de fin (quand ce sera fait)

- [ ] `parseUses`, `parseSpawn`, `generateSpawn`, `checkSpawn` écrits
- [ ] les 5 tests de `SpawnTest` verts, `mvn test` vert
- [ ] `examples/Integration.sarl` se transpile (avec les 4 parties)
- [ ] `// IA : ...` mis où il faut ; tu sais expliquer chaque ligne

## Questions pour vérifier que tu maîtrises ton code

1. Pourquoi a-t-on besoin d'une table des symboles pour `spawn` ?
2. Pourquoi deux passes dans `Semantic.analyze` ?
3. Que contient `SpawnStatement` et pourquoi garder les arguments **dans l'ordre** ?
4. Que fait `uses Lifecycle` dans le Python généré ? (Rien : c'est seulement un nœud d'arbre.)
5. Pourquoi signaler une variante non supportée plutôt que de l'ignorer ?
