# README Membre 1 : les agents (base du projet)

**Ta partie est déjà faite** dans le squelette : `agent Nom { ... }` est reconnu et traduit en
`class Nom:`. Ton rôle cette semaine est différent : **tu es la personne de référence sur la base du
projet**. Les Membres 3 et 4 ajoutent `on Initialize` et `on Destroy` en s'appuyant sur ton code.

Ce que contient ta partie :

| Fichier | Méthode | Rôle |
|---|---|---|
| `Parser.java` | `parse()`, `parseAgent()` | lit `agent Nom { ... }`, plusieurs agents par fichier, erreurs avec position |
| `SyntaxTree.java` | `ProgramNode`, `AgentNode` | l'arbre d'un fichier et d'un agent (membres dans l'ordre) |
| `PythonGenerator.java` | `generate()`, `generateAgent()` | écrit `class Nom:` ou `pass` si l'agent est vide |
| `Semantic.java` | `analyze()` (passe 1) | refuse deux agents du même nom |
| `AgentTest.java` | 8 tests | agents vides, plusieurs agents, erreurs, doublons, reprise après erreur |

---

## Étape 0 : préparer ton poste (15 min)

JDK 25, IntelliJ, dossier ouvert (voir `README.md`, §4). Lance `AgentTest` : 8 tests verts. Branche :
`git checkout -b membre1-support`.

## Étape 1 : comprendre le projet en entier (2 h)

Tu dois pouvoir **expliquer le projet entier**, pas seulement ta partie, parce que les autres te
poseront des questions.

| # | Fichier | À comprendre |
|---|---|---|
| 1 | `README.md` | le pipeline en 4 étapes |
| 2 | `docs/EXPLICATION.md` | un exemple complet |
| 3 | `docs/GRAMMAIRE.md` | les règles |
| 4 | `docs/LEXER.md` | comment le texte devient des tokens (regex, NFA, DFA) |
| 5 | `docs/TABLE_LL1.md` | FIRST, FOLLOW, table LL(1), et pourquoi le Parser est écrit ainsi |
| 6 | `docs/PARSER.md` | descente récursive et mode panique |
| 7 | `docs/ARBRE.md` | l'arbre, le contrat entre les membres |
| 8 | `docs/SEMANTIC.md`, `docs/GENERATEUR.md` | les deux dernières étapes |

Lis ensuite le code de `Parser.parse()` et `parseAgent()` ligne par ligne : c'est le modèle que les
autres imitent.

## Étape 2 : comprendre ta tâche (30 min)

D'après ta liste de tâches, il fallait : définir la syntaxe
`agent Nom { ... }`, créer le nœud `AgentNode`, écrire le parser, générer la classe Python, gérer les
erreurs avec ligne et colonne, et les tests. Tout est fait. Ce qui reste, c'est de **vérifier** et
d'**aider**.

## Étape 3 : ce que tu fais cette semaine

### 3.1 Relire et tester ta base (1 h)

1. Lance `AgentTest` et lis chaque test : comprends ce qu'il vérifie.
2. Ajoute 3 petits tests de ton choix dans `AgentTest.java`, par exemple :
   - un nom d'agent avec chiffres (`agent Agent2 {}`) ;
   - un commentaire avant l'agent (`// salut` puis `agent A {}`) ;
   - un agent avec un membre inconnu (`agent A { var x = 1 }`) doit échouer avec
     `UNSUPPORTED_FEATURE`.
3. Essaie de casser le Lexer avec un fichier bizarre, et regarde les erreurs.

### 3.2 Aider les Membres 3 et 4 (pendant la semaine)

- **Relire leurs Pull Requests** : la méthode écrite suit-elle le modèle de `parseInfo` ? Y a-t-il un
  test par comportement ? Le commentaire `// IA : ...` est-il présent ?
- Surveiller `SyntaxTree.java` : personne ne le modifie sans prévenir.
- Veiller à ce que `Semantic.checkAgent` contienne **une ligne par membre** (`checkInitialize`,
  `checkDestroy`), pas de gros code mélangé.

### 3.3 Organiser le dépôt (30 min)

- Pousser le squelette sur GitHub (branche `main`) et donner accès aux 3 autres.
- Vérifier que `.gitignore` empêche de pousser `target/` et `generated/`.

### 3.4 Préparer les décisions du groupe

Deux questions sont ouvertes (voir `docs/GUIDE_MEMBRES.md`, §7), à trancher avec tout le groupe :

1. **`class A:` ou `class A(Agent):` ?** La liste de tâches demande `(Agent)`. Si on le fait : un `import` en
   tête du fichier généré, et il faut modifier `PythonGenerator.generateAgent` et `AgentTest`.
2. **Les tests de runtime des listes de tâches** (UUID, « une seule fois », état `STOPPED`) ne peuvent pas être
   écrits en Java puisqu'on ne fait pas tourner le Python. On les remplace par des tests sur le texte
   généré.

## Étape 4 : vérifier à la fin de la semaine

- [ ] `mvn test` entièrement vert après la fusion des branches des Membres 3 et 4
- [ ] `examples/InitDestroy.sarl` se transpile sans erreur
- [ ] les deux décisions ci-dessus sont prises et notées dans le README
- [ ] chacun a ajouté `// IA : ...` là où une IA a aidé

## Questions pour vérifier que tu maîtrises ton code

1. Explique le pipeline complet sur un exemple.
2. Pourquoi y a-t-il deux passes dans `Semantic.analyze` ?
3. Que fait le mode panique du Parser, et pourquoi `synchronize` saute-t-il les blocs `{ ... }` ?
4. Pourquoi le `PythonGenerator` ne lit-il jamais de tokens ?
5. Pourquoi l'agent vide donne-t-il `pass` ?
