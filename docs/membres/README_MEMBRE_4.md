# README Membre 4 : `on Destroy`

**Ta mission cette semaine** : faire en sorte que `on Destroy { ... }` soit reconnu, vérifié et
traduit en Python. (`killMe` viendra plus tard.)

```sarl
agent A { on Destroy { info("bye") } }
```

doit devenir

```python
class A:
    def on_destroy(self, occurrence):
        self.info("bye")
```

Temps estimé : 3 à 4 heures, lecture comprise. Tu écris environ 20 lignes de Java et 6 tests.
Ta tâche est le **jumeau** de celle du Membre 3 (`on Initialize`) : même forme, autre mot-clé.

---

## Étape 0 : préparer ton poste (15 min)

1. JDK 25 et IntelliJ installés. Ouvrir le dossier du projet (voir `README.md`, §4).
2. Lancer `AgentTest` : tous les tests passent.
3. Créer ta branche : `git checkout -b membre4-destroy`.

## Étape 1 : comprendre le projet (1 h)

| # | Fichier | Ce que tu dois avoir compris |
|---|---|---|
| 1 | `README.md` | les 4 étapes : Lexer, Parser, Semantic, Générateur |
| 2 | `docs/EXPLICATION.md` | le pipeline avec un exemple |
| 3 | `docs/GRAMMAIRE.md` | la règle `<handler> -> Destroy <bloc>` |
| 4 | `docs/ARBRE.md` | le nœud `DestroyNode(body, line, column)` |
| 5 | `docs/PARSER.md` | comment s'écrit une méthode `parseXxx` (exemple : `parseInfo`) |
| 6 | `docs/GENERATEUR.md` | `line()`, `indent++`, `generateBlock()` |
| 7 | `docs/SEMANTIC.md` | comment compter les handlers d'un agent |

Lis aussi `docs/membres/README_MEMBRE_3.md` : c'est presque la même tâche, tu comprendras mieux.

Pour vérifier que tu as compris, réponds à voix haute :
- Que produit le Lexer pour `on Destroy` ? (`<on>` puis `<Destroy>`.)
- Qui crée le `DestroyNode` ? Qui écrit `def on_destroy` ?
- Quelle est la différence entre `on Initialize` et `on Destroy` dans le sens SARL ?
  (démarrage de l'agent / arrêt de l'agent)

## Étape 2 : comprendre ta tâche (30 min)

| Ordre | Fichier | Méthode | Ce qu'elle fait |
|---|---|---|---|
| 1 | `Parser.java` | `parseDestroy(Token on)` | lit `Destroy` et le bloc, retourne un `DestroyNode` |
| 2 | `PythonGenerator.java` | `generateDestroy(...)` | écrit `def on_destroy(self, occurrence):` et le corps |
| 3 | `Semantic.java` | `checkDestroy(agent)` (à créer) | refuse deux `on Destroy` dans le même agent |
| 4 | `DestroyTest.java` | 6 tests | vérifie tout ça |

**Entrée de `parseDestroy`** : le `on` est déjà consommé (paramètre `on`), le token courant est
`Destroy`. **Sortie** : un `DestroyNode`.

Hors de ta tâche : `killMe` (`parseKillMe`, `generateKillMe`) est prévu **plus tard**. Ne l'écris pas
cette semaine. Hors de ta tâche aussi : `uses`, `spawn`, `on Initialize` (c'est le Membre 3).

## Étape 3 : écrire le Parser (45 min)

Dans `Parser.java`, remplace `throw notImplemented(...)` de `parseDestroy` par :

```java
private SyntaxTree.Node parseDestroy(Token on) {
    advance();                                       // consomme le token Destroy
    if (check(Tag.LBRACKET)) {                       // handler "gardé" : pas supporté
        throw error(Diagnostics.Code.UNSUPPORTED_FEATURE,
                "les handlers gardés [...] ne sont pas supportés");
    }
    List<SyntaxTree.Statement> body = ???;           // quelle méthode lit "{ ... }" ?
    return new SyntaxTree.DestroyNode(???);          // corps, ligne et colonne du token "on"
}
```

Indices : c'est la même méthode de lecture de bloc que pour `info` et `Initialize` ; le nœud se
construit avec `(body, on.line(), on.column())`.

Test à la main : `agent A { on Destroy { } }` dans un fichier `.sarl` : tu dois obtenir l'erreur
« génération de on Destroy non implémentée » (le Parser marche, le Générateur reste à faire).

## Étape 4 : écrire le Générateur (30 min)

Dans `PythonGenerator.java`, remplace le `diag.report(...)` de `generateDestroy` :

```java
private void generateDestroy(SyntaxTree.DestroyNode node) {
    line("???");              // def on_destroy(self, occurrence):
    indent++;
    ???                       // écrire le contenu du bloc
    indent--;
}
```

Relance le transpileur : le Python attendu apparaît.

## Étape 5 : écrire la vérification Semantic (30 min)

Règle : au plus **un** `on Destroy` par agent, sinon `DUPLICATE_HANDLER`.

Dans `Semantic.java`, ajoute **ta méthode** et **une ligne** dans `checkAgent` (le Membre 3 y ajoute
aussi la sienne : mettez chacun une ligne séparée pour éviter les conflits Git) :

```java
checkDestroy(agent);          // <- ta ligne dans checkAgent

private void checkDestroy(SyntaxTree.AgentNode agent) {
    // même idée que checkInitialize du Membre 3, mais avec les DestroyNode
    ???
}
```

Indice : copie la logique de `docs/SEMANTIC.md` §4, en remplaçant `InitializeNode` par `DestroyNode`.
Le message d'erreur doit parler de `on Destroy`.

## Étape 6 : écrire les 6 tests (45 min)

Ouvre `DestroyTest.java`, retire `@Disabled` et écris les corps (modèle : `AgentTest`, ajoute les
`import static ... assertTrue/assertFalse`).

| Test | SARL à transpiler | Ce qu'on vérifie |
|---|---|---|
| `destroyHandlerBecomesAMethod` | `agent A { on Destroy { info("bye") } }` | succès ; le Python contient `def on_destroy(self, occurrence):\n        self.info("bye")` |
| `emptyHandlerBodyBecomesPass` | `agent A { on Destroy { } }` | contient `def on_destroy(self, occurrence):\n        pass` |
| `agentWithoutHandlerStillWorks` | `agent A {}` | succès, `class A:\n    pass` |
| `guardedHandlerGetsADiagnostic` | `agent A { on Destroy [x] { } }` | **échec**, `r.has(Diagnostics.Code.UNSUPPORTED_FEATURE)` |
| `twoDestroyHandlersGetADiagnostic` | `agent A { on Destroy { } on Destroy { } }` | **échec**, `r.has(Diagnostics.Code.DUPLICATE_HANDLER)` |
| `initializeAndDestroyKeepTheirOrder` | `agent A { on Initialize { } on Destroy { } }` | `indexOf("on_initialize") < indexOf("on_destroy")` |

Le dernier test a besoin du travail du Membre 3 : écris-le, mais active-le **après** avoir fusionné
sa branche (ou garde-le `@Disabled` jusque-là).

## Étape 7 : pousser (15 min)

```bash
git add .
git commit -m "Membre 4 : on Destroy (parser, generateur, semantic, tests)"
git push -u origin membre4-destroy
```

Ouvre une Pull Request. Mets `// IA : ...` partout où une IA t'a aidé.

## Checklist de fin

- [ ] `parseDestroy`, `generateDestroy`, `checkDestroy` écrits
- [ ] les tests de `DestroyTest` verts (celui d'ordre après fusion avec le Membre 3)
- [ ] `mvn test` entièrement vert
- [ ] `examples/InitDestroy.sarl` se transpile sans erreur (quand les Membres 3 et 4 sont fusionnés)
- [ ] pas de modification chez les autres membres ni dans `SyntaxTree.java`
- [ ] tu sais expliquer chaque ligne

## Questions pour vérifier que tu maîtrises ton code (entraîne-toi)

1. Quelle est la différence entre `on Initialize` et `on Destroy` ? Qu'est-ce qui change dans ton code
   par rapport à celui du Membre 3 ?
2. Pourquoi `parseDestroy` reçoit-il le token `on` en paramètre ?
3. Pourquoi y a-t-il une ligne vide entre `on_initialize` et `on_destroy` dans le Python ? (C'est
   `generateAgent` qui l'insère.)
4. Que se passe-t-il, dans le Parser, quand on écrit `on Destroy [x] { }` ? (Erreur, puis mode
   panique : voir `docs/PARSER.md` §5.)
5. Pourquoi `killMe` est-il repoussé à plus tard ? Qu'est-ce qu'il faudrait ajouter ?

## Si tu es bloqué

Même conseils que le Membre 3 : message d'erreur Java, `System.out.println(r.python())`,
position ligne/colonne des erreurs, et demande au groupe avant de toucher aux fichiers partagés.
