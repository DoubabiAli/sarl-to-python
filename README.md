# SARL vers Python : un transpileur en Java

Projet de groupe : un transpileur **SARL vers Python**, écrit en Java.

> **Tu débutes ? Lis ce fichier en entier, puis ouvre le README de ton membre dans
> `docs/membres/`.**

## 1. C'est quoi ce projet ?

On écrit un programme **Java** qui lit un fichier écrit en **SARL** (un langage pour programmer des
agents) et qui écrit le **même programme en Python**. Un programme qui traduit un langage vers un
autre s'appelle un **transpileur**. C'est comme traduire une phrase d'une langue vers une autre.

Exemple. Entrée (SARL) :

```sarl
agent StudentAgent {
    on Initialize {
        info("Agent started")
    }
    on Destroy {
        info("Agent stopped")
    }
}
```

Sortie (Python) :

```python
# Genere par le transpileur SARL -> Python. Ne pas modifier a la main.
class StudentAgent:
    def on_initialize(self, occurrence):
        self.info("Agent started")

    def on_destroy(self, occurrence):
        self.info("Agent stopped")
```

Vocabulaire SARL utilisé ici :
- un **agent** est un objet autonome (comme une classe) ;
- `on Initialize { ... }` est le code exécuté **quand l'agent démarre** ;
- `on Destroy { ... }` est le code exécuté **quand l'agent s'arrête** ;
- `info("...")` affiche un message.

## 2. Comment ça marche : quatre étapes

Pour traduire « Le chat mange la souris » vers l'anglais, un humain découpe les mots, vérifie la
grammaire, vérifie le sens, puis écrit la traduction. Notre programme fait pareil :

```text
texte SARL
   |
   v
 Lexer ---------> découpe le texte en mots appelés TOKENS          (Lexer.java)
   |
   v
 Parser --------> vérifie la grammaire, construit un ARBRE          (Parser.java)
   |
   v
 Semantic ------> vérifie le sens (pas de doublons...)              (Semantic.java)
   |
   v
 PythonGenerator > parcourt l'arbre et écrit le Python              (PythonGenerator.java)
   |
   v
fichier .py
```

Que devient `agent A { on Initialize { info("hi") } }` ?

| Étape | Résultat |
|---|---|
| Lexer | `<agent> <ID,A> <{> <on> <Initialize> <{> <info> <(> <STRING,hi> <)> <}> <}>` |
| Parser | un arbre : `AgentNode "A"` qui contient `InitializeNode` qui contient `InfoStatement "hi"` |
| Semantic | vérifie, par exemple, qu'il n'y a pas deux `on Initialize` dans l'agent |
| Générateur | `class A:` puis `def on_initialize(self, occurrence):` puis `self.info("hi")` |

Si une étape trouve une erreur, elle la note avec sa **ligne et sa colonne** (`Diagnostics.java`).
À la fin, le programme affiche toutes les erreurs et n'écrit pas de fichier Python.

## 3. Les fichiers du projet

```text
pom.xml                  configuration Maven (JDK 25, JUnit)
src/main/java/           le transpileur : tous les fichiers Java ici, sans package
src/test/java/           les tests JUnit
examples/                des fichiers SARL pour essayer
docs/                    toute la documentation
docs/membres/            un README par membre
```

| Fichier Java | Rôle | Qui le modifie |
|---|---|---|
| `Tag.java` | la liste des types de tokens (`AGENT`, `ID`, `LBRACE`...) | personne |
| `Token.java` | un token : type + texte + ligne + colonne | personne |
| `Lexer.java` | découpe le texte en tokens | déjà fait |
| `SyntaxTree.java` | les classes de l'arbre : **le contrat commun** | on prévient les autres avant de le changer |
| `Parser.java` | une méthode par règle de grammaire, construit l'arbre | chacun dans ses méthodes |
| `Semantic.java`, `SymbolTable.java` | les vérifications de sens | chacun dans sa zone |
| `PythonGenerator.java` | arbre vers texte Python | chacun dans ses méthodes |
| `Diagnostics.java` | la liste des erreurs, avec leurs codes | commun |
| `Transpiler.java` | le `main` qui relie les 4 étapes | personne |

## 4. Prérequis, lancer, tester

- **JDK 25** et **IntelliJ IDEA** (ou Maven 3.9+ en ligne de commande).
- Ouvrir le dossier dans IntelliJ : *File > Open*, choisir le dossier du projet (celui qui contient
  `pom.xml`), accepter « Open as Project ». IntelliJ charge Maven tout seul.

**Lancer les tests** : dans IntelliJ, clic sur le triangle vert à gauche d'une classe de test
(par exemple `AgentTest`) ; ou en ligne de commande : `mvn test`.

**Lancer le transpileur sur un fichier SARL** :

```bash
mvn -q compile exec:java -Dexec.mainClass=Transpiler -Dexec.args="examples/InitDestroy.sarl generated"
```

Le résultat est écrit dans `generated/InitDestroy.py`. Dans IntelliJ : ouvrir `Transpiler.java`,
cliquer sur le triangle vert à côté de `main`, *Run*, puis *Modify Run Configuration* et écrire dans
*Program arguments* : `examples/InitDestroy.sarl generated`.

## 5. Où en est-on ?

**Périmètre de cette semaine : `on Initialize` et `on Destroy` seulement.**
Le reste (`uses Lifecycle`, `spawn`, `killMe`) viendra plus tard.

| Membre | Fonctionnalité | Quand | README |
|---|---|---|---|
| 1 | Agents (`agent A { }`), base du projet | **fait** | `docs/membres/README_MEMBRE_1.md` |
| 2 | `uses Lifecycle`, `spawn` | plus tard | `docs/membres/README_MEMBRE_2.md` |
| 3 | `on Initialize` | **cette semaine** | `docs/membres/README_MEMBRE_3.md` |
| 4 | `on Destroy` (et `killMe` plus tard) | **cette semaine** | `docs/membres/README_MEMBRE_4.md` |

Déjà fait et commun : Lexer, Parser (agents, blocs, `info(...)`), arbre, Semantic (agents en double),
générateur de base, tests `AgentTest` et `LexerTest`.

Les tests de `InitializeTest`, `DestroyTest` et `SpawnTest` sont marqués `@Disabled` : chaque membre
les écrit, puis enlève `@Disabled`.

Objectif de la semaine : `examples/InitDestroy.sarl` se transpile sans erreur, et les tests
`InitializeTest` et `DestroyTest` passent. `examples/Integration.sarl` est la cible finale
(avec `uses` et `spawn`) : il échoue pour l'instant, c'est normal.

L'exécution du code Python n'est **pas** dans le projet : on s'arrête au texte Python généré.

## 6. Par où commencer ? L'ordre de lecture

1. Ce README.
2. **Le README de ton membre** (`docs/membres/`) : il te donne les étapes à suivre.
3. `docs/EXPLICATION.md` : le pipeline expliqué.
4. `docs/GRAMMAIRE.md` : les règles du langage accepté.
5. `docs/ARBRE.md`, `docs/PARSER.md`, `docs/GENERATEUR.md`, `docs/SEMANTIC.md` : chaque étape du code.
6. `docs/LEXER.md` et `docs/TABLE_LL1.md` : le lexer et le parser avec la méthode classique de la compilation
   (regex, NFA, DFA, FIRST, FOLLOW, table LL(1)). Utile pour comprendre la théorie derrière le projet.
7. `docs/GUIDE_MEMBRES.md` : la vue d'ensemble de qui fait quoi.
8. `docs/membres/README_MEMBRE_n.md` : les étapes détaillées pour chaque membre.

## 7. Comment travailler en groupe (Git)

Chacun travaille sur **sa branche**, pour ne pas écraser le travail des autres :

```bash
git clone <adresse-du-depot>
git checkout -b membre3-initialize        # ta branche (adapter le nom)
# ... tu modifies tes méthodes, tu lances les tests ...
git add .
git commit -m "Membre 3 : on Initialize (parser, generateur, semantic, tests)"
git push -u origin membre3-initialize
```

Puis, sur GitHub, ouvrir une *Pull Request* vers `main`. Un autre membre relit avant de fusionner.

Pour éviter les conflits :
- ne modifie que **tes** méthodes (elles portent la mention `MEMBRE n` ou `TODO Membre n`) ;
- dans `Semantic.java`, écris ta vérification dans **ta propre méthode** (`checkInitialize`...) et
  ajoute **une seule ligne** dans `checkAgent` ;
- ne change pas `SyntaxTree.java` sans prévenir le groupe.

## 8. Règles du groupe

- **IA** : la règle du groupe est d'ajouter un commentaire `// IA : ...` à chaque endroit où une IA a servi. Chacun doit
  **comprendre et pouvoir expliquer** chaque ligne qu'il pousse : on pose des questions sur tout le
  code en revue de code.
- Un test par comportement ; un test qui échoue se corrige avant de pousser.
- Les messages d'erreur donnent toujours la **ligne et la colonne**.
- On n'ignore jamais un morceau de code qu'on ne sait pas traduire : on signale
  `UNSUPPORTED_FEATURE`.

## 9. Mini-glossaire

| Mot | Sens |
|---|---|
| token | un « mot » découpé par le Lexer (`<ID,A>`, `<{>`...) |
| lexème | le texte exact du token (`A`) |
| grammaire | les règles qui disent comment assembler les tokens |
| Parser | vérifie la grammaire et construit l'arbre |
| arbre (AST) | des objets Java qui représentent le programme (`AgentNode`...) |
| Semantic | vérifie le sens (noms, doublons) |
| `Diagnostics` | la liste des erreurs trouvées |
| NFA / DFA | des « plans » à états pour reconnaître un token (voir `docs/LEXER.md`) |
| FIRST / FOLLOW | ce qui peut commencer / suivre une règle (voir `docs/TABLE_LL1.md`) |
| transpileur | un compilateur dont la sortie est du code source (ici Python) |
