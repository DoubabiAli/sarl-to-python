# README Membre 3 : `on Initialize`

**Ta mission cette semaine** : faire en sorte que `on Initialize { ... }` soit reconnu, vérifié et
traduit en Python.

```sarl
agent A { on Initialize { info("hi") } }
```

doit devenir

```python
class A:
    def on_initialize(self, occurrence):
        self.info("hi")
```

Temps estimé : 3 à 4 heures, lecture comprise. Tu écris environ 20 lignes de Java et 5 tests.

---

## Étape 0 : préparer ton poste (15 min)

1. JDK 25 et IntelliJ installés. Ouvrir le dossier du projet dans IntelliJ (voir `README.md`, §4).
2. Lancer `AgentTest` (triangle vert) : tous les tests passent. Si oui, ton installation est bonne.
3. Créer ta branche : `git checkout -b membre3-initialize`.

## Étape 1 : comprendre le projet (1 h)

À lire dans cet ordre. Ne cherche pas à tout retenir, comprends l'idée.

| # | Fichier | Ce que tu dois avoir compris |
|---|---|---|
| 1 | `README.md` | les 4 étapes : Lexer, Parser, Semantic, Générateur |
| 2 | `docs/EXPLICATION.md` | le pipeline avec un exemple |
| 3 | `docs/GRAMMAIRE.md` | la règle `<handler> -> Initialize <bloc>` |
| 4 | `docs/ARBRE.md` | ce qu'est un `record`, et le nœud `InitializeNode(body, line, column)` |
| 5 | `docs/PARSER.md` | comment s'écrit une méthode `parseXxx` (exemple : `parseInfo`) |
| 6 | `docs/GENERATEUR.md` | `line()`, `indent++`, `generateBlock()` |
| 7 | `docs/SEMANTIC.md` | comment compter les handlers d'un agent |

Pour vérifier que tu as compris, réponds à voix haute :
- Que fait le Lexer avec `on Initialize` ? (Il produit deux tokens : `<on>` et `<Initialize>`.)
- Qui construit le `InitializeNode` ? (Le Parser.) Qui écrit le Python ? (Le Générateur.)
- Pourquoi le Parser ne génère-t-il jamais de Python ?

## Étape 2 : comprendre ta tâche (30 min)

Ta tâche touche trois fichiers et un test, **toujours dans cet ordre** :

| Ordre | Fichier | Méthode | Ce qu'elle fait |
|---|---|---|---|
| 1 | `Parser.java` | `parseInitialize(Token on)` | lit `Initialize` et le bloc, retourne un `InitializeNode` |
| 2 | `PythonGenerator.java` | `generateInitialize(...)` | écrit `def on_initialize(self, occurrence):` et le corps |
| 3 | `Semantic.java` | `checkInitialize(agent)` (à créer) | refuse deux `on Initialize` dans le même agent |
| 4 | `InitializeTest.java` | 5 tests | vérifie tout ça |

Comment ta méthode est appelée : dans `Parser.parseMember()`, quand le Parser voit `on`, il consomme
`on`, regarde le token suivant, et si c'est `Initialize` il appelle `parseInitialize(on)`.
**Entrée de ta méthode** : le `on` est déjà consommé (c'est ton paramètre `on`), le token courant est
`Initialize`. **Sortie** : un `InitializeNode`.

Ce qui est déjà fait pour toi : `parseBlock()` lit `{ ... }` et retourne la liste des instructions ;
`generateBlock(...)` écrit les instructions ; `info(...)` est déjà géré.

Hors de ta tâche (ne le fais pas) : `uses`, `spawn`, `killMe`, `on Destroy`.

## Étape 3 : écrire le Parser (45 min)

Ouvre `Parser.java`, trouve `parseInitialize`. Remplace `throw notImplemented(...)` par ce squelette,
à compléter à la place des `???` :

```java
private SyntaxTree.Node parseInitialize(Token on) {
    advance();                                       // consomme le token Initialize
    if (check(Tag.LBRACKET)) {                       // on Initialize [condition] : handler "gardé"
        throw error(Diagnostics.Code.UNSUPPORTED_FEATURE,
                "les handlers gardés [...] ne sont pas supportés");
    }
    List<SyntaxTree.Statement> body = ???;           // quelle méthode lit "{ ... }" ?
    return new SyntaxTree.InitializeNode(???);       // corps, ligne et colonne du token "on"
}
```

Indices :
- la méthode qui lit un bloc s'appelle `parseBlock()` (cherche son commentaire dans `Parser.java`) ;
- le nœud se construit avec `(body, on.line(), on.column())`, comme dans `parseInfo`.

**Pour tester à la main** : écris un fichier `examples/Mien.sarl` avec `agent A { on Initialize { } }`
et lance le transpileur. Tu vas voir l'erreur « génération de on Initialize non implémentée » : c'est
bon signe, ça veut dire que le Parser marche et que le Générateur n'est pas encore fait.

## Étape 4 : écrire le Générateur (30 min)

Ouvre `PythonGenerator.java`, trouve `generateInitialize`. Remplace le `diag.report(...)` par :

```java
private void generateInitialize(SyntaxTree.InitializeNode node) {
    line("???");              // la ligne : def on_initialize(self, occurrence):
    indent++;                 // on entre dans la méthode
    ???                       // écrire le contenu du bloc (quelle méthode ?)
    indent--;                 // on sort
}
```

Indices : `generateBlock(node.body())` écrit le corps (et un `pass` si le bloc est vide).
Relance le transpileur sur ton exemple : tu dois voir le Python attendu.

## Étape 5 : écrire la vérification Semantic (30 min)

Règle : au plus **un** `on Initialize` par agent, sinon `DUPLICATE_HANDLER`.

Dans `Semantic.java`, ajoute **ta propre méthode** et **une seule ligne** dans `checkAgent` :

```java
private void checkAgent(SyntaxTree.AgentNode agent, SymbolTable global) {
    checkInitialize(agent);     // <- ta ligne (le Membre 4 ajoutera la sienne dessous)
}

private void checkInitialize(SyntaxTree.AgentNode agent) {
    int count = 0;
    for (SyntaxTree.Node member : agent.members()) {
        if (member instanceof SyntaxTree.InitializeNode init) {
            count++;
            if (count > 1) {
                diag.report(Diagnostics.Code.DUPLICATE_HANDLER, init.line(), init.column(),
                        "l'agent " + agent.name() + " a plusieurs handlers on Initialize");
            }
        }
    }
}
```

Comprends-le : on parcourt les membres de l'agent, on compte ceux qui sont des `InitializeNode`, et à
partir du deuxième on signale une erreur à sa position.

## Étape 6 : écrire les 5 tests (45 min)

Ouvre `InitializeTest.java`. Pour chaque test, retire `@Disabled`, écris le corps. Modèle :

```java
@Test
void initializeHandlerBecomesAMethod() {
    Transpiler.Result r = Transpiler.transpile("agent A { on Initialize { info(\"hi\") } }");
    assertTrue(r.success());
    assertTrue(r.python().contains(
            "def on_initialize(self, occurrence):\n        self.info(\"hi\")"));
}
```

Ajoute en haut : `import static org.junit.jupiter.api.Assertions.assertTrue;` (et `assertFalse`).

| Test | SARL à transpiler | Ce qu'on vérifie |
|---|---|---|
| `initializeHandlerBecomesAMethod` | `agent A { on Initialize { info("hi") } }` | succès, et le Python contient `def on_initialize(self, occurrence):` puis `self.info("hi")` |
| `emptyHandlerBodyBecomesPass` | `agent A { on Initialize { } }` | le Python contient `def on_initialize(self, occurrence):\n        pass` |
| `agentWithoutHandlerStillWorks` | `agent A {}` | succès, `class A:\n    pass` |
| `guardedHandlerGetsADiagnostic` | `agent A { on Initialize [x] { } }` | **échec**, et `r.has(Diagnostics.Code.UNSUPPORTED_FEATURE)` |
| `twoInitializeHandlersGetADiagnostic` | `agent A { on Initialize { } on Initialize { } }` | **échec**, et `r.has(Diagnostics.Code.DUPLICATE_HANDLER)` |

Lance `InitializeTest` : tout doit être vert. Lance ensuite **tous** les tests (`mvn test`) : rien ne
doit être cassé.

## Étape 7 : pousser (15 min)

```bash
git add .
git commit -m "Membre 3 : on Initialize (parser, generateur, semantic, tests)"
git push -u origin membre3-initialize
```

Ouvre une Pull Request sur GitHub. N'oublie pas le commentaire `// IA : ...` à chaque endroit où une
IA t'a aidé.

## Checklist de fin

- [ ] `parseInitialize`, `generateInitialize`, `checkInitialize` écrits
- [ ] les 5 tests de `InitializeTest` activés et verts ; `mvn test` entièrement vert
- [ ] pas de modification dans les méthodes des autres membres, ni dans `SyntaxTree.java`
- [ ] commentaires `// IA : ...` mis où il faut
- [ ] tu sais expliquer chaque ligne que tu as écrite

## Questions pour vérifier que tu maîtrises ton code (entraîne-toi)

1. Que se passe-t-il, étape par étape, pour `agent A { on Initialize { info("hi") } }` ?
2. Pourquoi a-t-on un `InitializeNode` dans l'arbre et pas directement du texte Python ?
3. Que fait `expect` et que fait `match` ? Dans quel cas utilise-t-on l'un ou l'autre ?
4. Pourquoi refuse-t-on `on Initialize [x] { }` au lieu de l'ignorer ?
5. Pourquoi la vérification des doublons est-elle dans le Semantic et pas dans le Parser ?
6. Dans la grammaire, pourquoi `on` est-il factorisé (`<membre> -> on <handler>`) ? (Voir
   `docs/TABLE_LL1.md`, §02.)

## Si tu es bloqué

- Une erreur de compilation Java : lis le message, il donne le fichier et la ligne.
- Un test échoue : regarde le texte Python produit en l'affichant (`System.out.println(r.python())`).
- Les erreurs du transpileur donnent `ligne`/`colonne` : compare avec ton fichier `.sarl`.
- Demande au groupe avant de toucher à un fichier partagé.
