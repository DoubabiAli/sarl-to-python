# Guide des tâches : que doit faire chaque membre

Les listes de tâches du groupe parlaient de fichiers Python
(`agent_parser.py`...). Le groupe a décidé de tout faire en **Java**. Voici la traduction : au lieu
d'un fichier par tâche, **chaque fonctionnalité est une tranche verticale** qui traverse les mêmes
fichiers.

```text
                 Parser.java      Semantic.java     PythonGenerator.java     Test
Membre 1 Agents    parseAgent        doublons          generateAgent         AgentTest     (fait)
Membre 2 Spawn     parseUses         type connu        generateSpawn         SpawnTest
                   parseSpawn
Membre 3 Init      parseInitialize   1 seul handler    generateInitialize    InitializeTest
Membre 4 Destroy   parseDestroy      1 seul handler    generateDestroy       DestroyTest
                   parseKillMe                         generateKillMe
```

> Chaque membre a un README détaillé, étape par étape : `docs/membres/README_MEMBRE_1.md` à
> `README_MEMBRE_4.md`. Ce guide est la vue d'ensemble.

## 0. Périmètre de cette semaine

**Seulement `on Initialize` et `on Destroy`.** Tout le reste est « plus tard » :

| À faire cette semaine | Plus tard |
|---|---|
| Membre 3 : `on Initialize` (section 5) | Membre 2 : `uses Lifecycle` et `spawn` (section 4) |
| Membre 4 : `on Destroy` (section 6, sans `killMe`) | Membre 4 : `killMe` (fin de la section 6) |

L'objectif : `examples/InitDestroy.sarl` se transpile sans erreur, et les tests `InitializeTest` et
`DestroyTest` passent. Les corps de handlers ne contiennent que `info(...)`, déjà géré.

## 1. Méthode de travail commune

1. Lire `EXPLICATION.md`, `GRAMMAIRE.md`, puis `PARSER.md`, `ARBRE.md`, `SEMANTIC.md`, `GENERATEUR.md`.
2. Créer sa branche Git : `git checkout -b membre3-initialize` (adapter le nom).
3. Travailler **dans l'ordre** : (a) parser, (b) test du parser, (c) générateur, (d) sémantique.
4. Après chaque morceau : `mvn test`, ou le bouton vert d'IntelliJ à côté du test.
5. Ne modifier que ses méthodes (marquées `MEMBRE n` dans le code). `SyntaxTree.java` est partagé :
   prévenir le groupe avant de le modifier.
6. Commentaire `// IA : ...` à chaque endroit où une IA a aidé. Comprendre chaque ligne poussée :
   chacun doit pouvoir expliquer tout le code.
7. `git add`, `git commit`, `git push`, puis une *pull request* relue par un autre membre.

## 2. Comment écrire un test

Un test transpile une petite chaîne SARL et regarde le résultat (modèle : `AgentTest`).

```java
@Test
void initializeHandlerGeneratesAMethod() {
    Transpiler.Result r = Transpiler.transpile("agent A { on Initialize { info(\"hi\") } }");
    assertTrue(r.success());
    assertTrue(r.python().contains("def on_initialize(self, occurrence):"));
}
```

`Transpiler.Result` offre : `success()`, `python()` (le texte, `null` s'il y a des erreurs),
`errors()` (la liste des diagnostics) et `has(Diagnostics.Code.X)`. Pour un cas d'erreur :

```java
Transpiler.Result r = Transpiler.transpile("agent A { on Initialize [x] { } }");
assertFalse(r.success());
assertTrue(r.has(Diagnostics.Code.UNSUPPORTED_FEATURE));
```

Retirer `@Disabled` du test **quand** la fonctionnalité est prête.

## 3. Membre 1 : agents (déjà fait)

Rien à écrire. Lire le code pour comprendre le modèle : `Parser.parse()` et `parseAgent()`,
`PythonGenerator.generateAgent()`, `Semantic.analyze()` (passe 1), `AgentTest`.
Une amélioration possible : un meilleur message d'erreur, ou la décision `class A(Agent):` (voir §7).

## 4. Membre 2 : `uses Lifecycle` et `spawn` (plus tard)

**Exemple SARL** : `uses Lifecycle` puis `spawn(typeof(ChildAgent), "Max", 20)`.

| Où | Quoi |
|---|---|
| `Parser.parseUses()` | consommer `uses`, lire un `ID`, puis tant qu'il y a une virgule, un autre `ID`. Retourner `UsesNode(liste, ligne, colonne)` |
| `Parser.parseSpawn()` | consommer `spawn`, `(`, `typeof`, `(`, un `ID`, `)`, puis tant qu'il y a une virgule un argument avec `parseExpression()`, puis `)`. Retourner `SpawnStatement(typeAgent, arguments, ligne, colonne)` |
| `PythonGenerator.generateSpawn()` | `line("self.runtime.spawn(" + type + ", " + arguments + ", spawner=self)")` (arguments : `expression(e)` séparés par `, `) |
| `Semantic.checkAgent()` | pour chaque `SpawnStatement` dans les corps de handlers : `global.get(type) == null` -> `UNKNOWN_AGENT_TYPE` |

Erreurs à prévoir : spawn mal formé (`MALFORMED_SPAWN`), variantes non supportées (contexte, id
explicite, plusieurs agents : `UNSUPPORTED_FEATURE`), agent inconnu (`UNKNOWN_AGENT_TYPE`).

Squelette de `parseSpawn` à compléter, à imiter sur `parseInfo` :

```java
private SyntaxTree.Statement parseSpawn() {
    Token keyword = advance();                                   // spawn
    expect(Tag.LPAREN, Diagnostics.Code.MALFORMED_SPAWN, "...");
    // expect TYPEOF, LPAREN, ID (garder le Token : name), RPAREN
    List<SyntaxTree.Expression> arguments = new ArrayList<>();
    while (match(Tag.COMMA)) { arguments.add(parseExpression()); }
    // expect RPAREN
    return new SyntaxTree.SpawnStatement(name.lexeme(), arguments, keyword.line(), keyword.column());
}
```

Tests (`SpawnTest`) : spawn d'un agent connu, arguments dans l'ordre, agent inconnu rejeté, forme
non supportée rejetée.

## 5. Membre 3 : `on Initialize`

**Exemple SARL** : `on Initialize { info("Parent started") }`.

| Où | Quoi |
|---|---|
| `Parser.parseInitialize(on)` | le `on` est déjà consommé ; le token courant est `Initialize`. Consommer `Initialize` ; si le token suivant est `[` (handler gardé) : `throw error(UNSUPPORTED_FEATURE, ...)` ; lire le corps avec `parseBlock()` ; retourner `InitializeNode(corps, on.line(), on.column())` |
| `PythonGenerator.generateInitialize()` | écrire `def on_initialize(self, occurrence):`, `indent++`, `generateBlock(node.body())`, `indent--` |
| `Semantic.checkAgent()` | plus d'un `InitializeNode` dans `agent.members()` -> `DUPLICATE_HANDLER` (code d'exemple dans `SEMANTIC.md`) |

Squelette du parser :

```java
private SyntaxTree.Node parseInitialize(Token on) {
    advance();                                      // Initialize
    if (check(Tag.LBRACKET)) {
        throw error(Diagnostics.Code.UNSUPPORTED_FEATURE, "...");
    }
    List<SyntaxTree.Statement> body = ???;          // quelle méthode lit "{ ... }" ?
    return new SyntaxTree.InitializeNode(???);
}
```

Squelette du générateur :

```java
private void generateInitialize(SyntaxTree.InitializeNode node) {
    line("???");
    indent++;
    ???
    indent--;
}
```

Résultat attendu : `agent A { on Initialize { info("hi") } }` donne

```python
class A:
    def on_initialize(self, occurrence):
        self.info("hi")
```

Tests (`InitializeTest`) : agent avec handler -> méthode générée ; agent **sans** handler -> pas
de crash ; handler gardé ou deux handlers -> diagnostic ; corps vide -> `pass`.

Attention : la liste de tâches demande que le runtime Python appelle ce handler « une seule fois » avec
`occurrence.parameters` et `occurrence.spawner`. Voir §7 : le projet Java ne contient pas de
runtime Python, donc ces tests-là se vérifient sur le **texte généré**.

## 6. Membre 4 : `on Destroy` et `killMe`

**Exemple SARL** : `on Destroy { info("bye") }`. (`killMe` : plus tard, voir plus bas.)

| Où | Quoi |
|---|---|
| `Parser.parseDestroy(on)` | identique à `parseInitialize`, mais avec `Destroy` et un `DestroyNode` |
| `Parser.parseKillMe()` | **plus tard** : consommer `killMe`, retourner `KillMeStatement(ligne, colonne)` |
| `PythonGenerator.generateDestroy()` | `def on_destroy(self, occurrence):` + corps |
| `PythonGenerator.generateKillMe()` | **plus tard** : `line("self.runtime.stop(self)")` |
| `Semantic.checkAgent()` | plus d'un `DestroyNode` -> `DUPLICATE_HANDLER` |

Tests (`DestroyTest`) : handler `Destroy` généré en méthode ; corps vide -> `pass` ; agent sans
handler ; handler gardé ou deux handlers -> diagnostic. (Test de `killMe` : plus tard.)

## 7. Points à décider ensemble avant de coder

> **Décisions prises** : (1) on génère `class A(Agent):` avec `from sarl_runtime import Agent` en tête ;
> (2) le texte généré est testé en Java, le comportement exécuté en Python dans `runtime/`
> (option (a) + stub de l'option (b)). Les détails d'origine restent ci-dessous.

1. **`class A:` ou `class A(Agent):` ?** La liste de tâches du Membre 1 écrit `class StudentAgent(Agent):`.
   Aujourd'hui le générateur écrit `class A:`. Si on choisit `(Agent)`, il faut aussi une ligne
   d'import en tête (`from sarl_runtime import Agent`) et modifier `AgentTest`.
2. **Le runtime Python.** Les listes de tâches décrivent un petit runtime (UUID, états `CREATED / RUNNING /
   STOPPING / STOPPED`, `runtime.spawn`, `runtime.stop`). Le projet actuel s'arrête au texte
   Python : l'exécution est optionnelle dans le cahier des charges. Les tests des listes de tâches sur les UUID, l'état
   `STOPPED` ou le « une seule fois » ne peuvent donc pas être écrits en Java tels quels. Deux
   options : (a) les remplacer par des tests sur le **texte généré** (simple) ; (b) ajouter un petit
   fichier `runtime/sarl_runtime.py` écrit à la main, plus tard, comme bonus.
3. **La syntaxe SARL exacte** de `spawn` et `killMe` : à vérifier sur https://www.sarl.io avant
   de figer la grammaire (le fichier `examples/Integration.sarl` le dit aussi).

## 8. Quand c'est fini

- Tous les tests de votre classe sont activés et passent (`mvn test`).
- Cette semaine : `examples/InitDestroy.sarl` se transpile sans erreur.
- Plus tard : `examples/Integration.sarl` se transpile sans erreur quand tout est fait.
- Les fichiers `docs/` correspondants sont à jour si vous avez changé la grammaire.
