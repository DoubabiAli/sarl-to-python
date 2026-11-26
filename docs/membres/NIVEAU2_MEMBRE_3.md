# Niveaux 2 + 3, Membre 3 : `if` / `else` / `while`, `List` et `Set`

**Ta mission** : faire reconnaître, vérifier et traduire les structures de contrôle `if` / `else` /
`while`, et les collections `List` et `Set` (création et opérations simples).

```sarl
agent A {
    on Initialize {
        var liste = new ArrayList<Integer>()
        var i = 0
        while (i < 3) {
            if (i == 1) {
                liste.add(i)
            } else {
                info("autre")
            }
            i = i + 1
        }
        var s = #{1, 2}
    }
}
```

doit devenir

```python
class A(Agent):
    def on_initialize(self, occurrence):
        liste = []
        i = 0
        while i < 3:
            if i == 1:
                liste.append(i)
            else:
                self.info("autre")
            i = i + 1
        s = {1, 2}
```

Temps estimé : 5 à 6 h, lecture comprise. Tu écris environ 150 lignes de Java et une douzaine de tests.

**Lis d'abord `docs/membres/NIVEAU2_CONTRAT.md`** (sous-ensemble, nœuds, règles du Python). Tes nœuds
(`IfStatement`, `WhileStatement`, `ListLiteral`, `SetLiteral`, `NewObject`) **existent déjà** : ils sont écrits
dans la PR 0 par le Membre 1. Tu ne modifies pas `SyntaxTree.java`.

---

## Étape 0 : préparer ton poste (15 min)

1. La PR 0 doit être fusionnée. `git checkout main`, `git pull`.
2. `mvn test` : tout est vert. Dans le terminal Git Bash, avant Maven :
   `export JAVA_HOME="$HOME/.jdks/openjdk-25.0.2"` puis `export PATH="$JAVA_HOME/bin:$PATH"`.
3. `git checkout -b membre3-controle-collections`.

## Étape 1 : comprendre le projet (1 h)

| # | Fichier | À comprendre |
|---|---|---|
| 1 | `docs/NIVEAU2_CONTRAT.md`, §2, 4, 8 | la syntaxe de `if`/`while`, des collections, et ce qu'il faut générer |
| 2 | `Parser.java` : `parseBlock()` | elle lit `{ ... }` : **tu la réutilises** pour les corps de `if` et de `while` |
| 3 | `PythonGenerator.java` : `generateBlock()` | elle écrit un corps (avec `pass` si vide) : tu la réutilises |
| 4 | `Semantic.java` : `checkBlock`, `checkStatement` | un bloc ouvre une **portée** ; ton `if` appelle `checkBlock` |
| 5 | `docs/SEMANTIC.md`, `SymbolTable.java` | chaîne de portées : `openContext()` / `closeContext()` |

Pour vérifier que tu as compris, réponds à voix haute :
- Pourquoi le corps d'un `if` a-t-il sa propre portée ?
- Quelle méthode lit `{ ... }` et quelle méthode l'écrit en Python ?
- Pourquoi `#{}` seul est-il refusé alors que `new HashSet<Integer>()` est accepté ?

## Étape 2 : comprendre ta tâche (30 min)

| Ordre | Fichier | Méthodes |
|---|---|---|
| 1 | `Parser.java` | `parseIf`, `parseWhile`, `parseCollectionLiteral`, `parseNew` |
| 2 | `Semantic.java` | `checkIf`, `checkWhile`, `typeOfCollectionLiteral`, `typeOfNew` (pour `ArrayList`/`HashSet`), `typeOfListSetCall` |
| 3 | `PythonGenerator.java` | `generateIf`, `generateWhile`, `generateListLiteral`, `generateSetLiteral`, `generateNew` (pour `ArrayList`/`HashSet`), `generateListSetCall` |
| 4 | `ControlCollectionTest.java` | les tests de l'étape 7 |

Les conditions (`i < 3`) et les éléments (`1, 2`) sont lus par **`parseExpression()` du Membre 2**. Tu ne
les analyses pas toi-même. En attendant sa PR, teste avec `true`, `false`, des nombres et des noms.

Hors de ta tâche : maps (`#{k -> v}`, `HashMap`, `TreeMap`) : Membre 4. Les types (`TypeRef`, `Types`) :
Membre 1.

## Étape 3 : écrire le Parser (1 h 30)

`if` / `else` / `while` (accolades **obligatoires**, parenthèses autour de la condition) :

```java
private SyntaxTree.Statement parseIf() {
    Token keyword = advance();                                      // if
    expect(Tag.LPAREN, Diagnostics.Code.SYNTAX_ERROR, "'(' attendu après 'if'");
    SyntaxTree.Expression condition = ???;                          // parseExpression() du Membre 2
    expect(Tag.RPAREN, Diagnostics.Code.SYNTAX_ERROR, "')' attendu après la condition");
    List<SyntaxTree.Statement> thenBody = ???;                      // quelle méthode lit un bloc ?
    List<SyntaxTree.Statement> elseBody = new ArrayList<>();
    if (match(Tag.ELSE)) {
        if (check(Tag.IF)) {
            elseBody.add(parseIf());                                // « else if » : un if dans le else
        } else {
            elseBody = parseBlock();
        }
    }
    return new SyntaxTree.IfStatement(condition, thenBody, elseBody, keyword.line(), keyword.column());
}
```

`parseWhile()` : même chose, sans `else`, et retourne un `WhileStatement(condition, body, ...)`.

Comprends-le : un `if` sans accolades (`if (x) y = 1`) donne l'erreur `MISSING_BRACE` de `parseBlock`
(« '{' attendu pour ouvrir un bloc ») : c'est voulu, c'est dans les limites du sous-ensemble.

Collections. Le Lexer donne `HASH` pour `#`, puis `LBRACKET` ou `LBRACE`.

```java
private SyntaxTree.Expression parseCollectionLiteral() {
    Token hash = advance();                                          // #
    if (match(Tag.LBRACKET)) {                                       // #[ ... ]  : liste
        List<SyntaxTree.Expression> elements = parseElementsUntil(Tag.RBRACKET);
        return new SyntaxTree.ListLiteral(elements, hash.line(), hash.column());
    }
    if (match(Tag.LBRACE)) {                                         // #{ ... }  : ensemble OU map
        if (check(Tag.RBRACE)) {
            throw error(Diagnostics.Code.UNSUPPORTED_FEATURE,
                    "#{} vide est ambigu : écrire new HashSet<T>() ou new HashMap<K,V>()");
        }
        SyntaxTree.Expression first = ???;                           // parseExpression()
        if (check(Tag.ARROW)) {
            return parseMapRest(hash, first);                        // c'est une map : le Membre 4 continue
        }
        ... // sinon : lire les éléments suivants (séparés par des virgules) jusqu'à RBRACE, puis SetLiteral
    }
    throw error(Diagnostics.Code.SYNTAX_ERROR, "'[' ou '{' attendu après '#'");
}
```

Comprends-le : `#{1, 2}` (ensemble) et `#{"a" -> 1}` (map) commencent pareil ; c'est la flèche `->` après le
premier élément qui décide. Pour `#[ ]` vide : même refus que `#{}` (`#[]` : type non déductible).
`parseElementsUntil(Tag fin)` est un petit helper à toi : lit `expr {, expr}` puis consomme la fermeture.

`new` :

```java
private SyntaxTree.Expression parseNew() {
    Token keyword = advance();                                       // new
    SyntaxTree.TypeRef type = ???;                                   // parseType() du Membre 1 : ArrayList<Integer>
    expect(Tag.LPAREN, Diagnostics.Code.SYNTAX_ERROR, "'(' attendu après le type");
    List<SyntaxTree.Expression> args = ???;                          // parseArguments() du Membre 2
    expect(Tag.RPAREN, Diagnostics.Code.SYNTAX_ERROR, "')' attendu");
    return new SyntaxTree.NewObject(type, args, keyword.line(), keyword.column());
}
```

(C'est toi qui écris le Parser de `new` pour **toutes** les collections, y compris `HashMap` / `TreeMap` :
le Membre 4 fait seulement la vérification et la génération pour les maps.)

## Étape 4 : écrire le Semantic (1 h 30)

```java
void checkIf(SyntaxTree.IfStatement node, SymbolTable scope) {
    TypeRef condition = typeOf(node.condition(), scope);             // Membre 2
    if (condition != null && !Types.isBoolean(condition)) {
        diag.report(Diagnostics.Code.TYPE_MISMATCH, node.condition().line(), node.condition().column(),
                "la condition d'un if doit être un boolean");
    }
    checkBlock(node.thenBody(), scope);                              // chaque corps ouvre sa portée
    checkBlock(node.elseBody(), scope);
}
```

`checkWhile` : pareil, avec le corps.

**Types des collections** (règles de `typeOfCollectionLiteral`, `typeOfNew`) :
- `#[a, b]` → `List<T>` où `T` est le type du premier élément ; les autres éléments doivent être
  compatibles (`Types.isAssignable`), sinon `TYPE_MISMATCH`. `#{a, b}` → `Set<T>` de la même façon.
- `new ArrayList<T>()` ou `new HashSet<T>()` : un seul argument de type, **aucun argument** au constructeur,
  sinon `UNSUPPORTED_FEATURE`. Le type du résultat est le `TypeRef` du `new`.
- Dans `typeOfNew`, si le nom est `HashMap` ou `TreeMap`, **laisse le Membre 4** vérifier (il écrit sa
  partie dans le même `switch`) : ton code ne traite que `ArrayList` et `HashSet`.

**Opérations** : `typeOfListSetCall(MemberCall call, TypeRef receiverType, SymbolTable scope)` est appelée
par le Membre 2 quand le receveur est une liste ou un ensemble. Elle valide le nom de la méthode, le nombre
d'arguments et leurs types, et retourne le type du résultat. Table à respecter :

| Receveur | Méthode | Arguments | Résultat |
|---|---|---|---|
| `List<T>` | `add` | `T` | `void` |
| `List<T>` | `get` | `int` | `T` |
| `List<T>` | `size` | aucun | `int` |
| `List<T>` | `contains` | `T` | `boolean` |
| `Set<T>` | `add` | `T` | `void` |
| `Set<T>` | `contains` | `T` | `boolean` |
| `Set<T>` | `size` | aucun | `int` |

- Une méthode d'une **autre** collection (par exemple `get` sur un ensemble) : `UNSUPPORTED_FEATURE` avec
  le message « `get` est réservé à List » : c'est le test « opération réservée à List utilisée sur Set ».
- Une méthode inconnue : `UNSUPPORTED_FEATURE`.
- Mauvais nombre d'arguments : `WRONG_ARGUMENT_COUNT`. Argument de mauvais type (ajouter un `String` à une
  `List<Integer>`) : `TYPE_MISMATCH`.
- `add` retourne `void` ici (en SARL il retourne un booléen) : on ne l'utilise que comme instruction.
  `var b = liste.add(1)` donne donc `TYPE_MISMATCH` automatiquement (grâce à `Types.VOID`).

## Étape 5 : écrire le Générateur (1 h)

```java
private void generateIf(SyntaxTree.IfStatement node) {
    line("if " + expression(node.condition()) + ":");
    indent++; generateBlock(node.thenBody()); indent--;
    if (!node.elseBody().isEmpty()) {
        line("else:");
        indent++; generateBlock(node.elseBody()); indent--;
    }
}
```

Amélioration à faire : quand `elseBody` ne contient qu'**un seul** `IfStatement`, écrire `elif ...:` au lieu de
`else:` suivi d'un `if` imbriqué (les deux sont corrects ; `elif` est plus lisible). Indice : une méthode
`generateIf(node, boolean elif)` qui écrit `elif` ou `if` au début.

`generateWhile` : `while condition:` puis le corps, avec `generateBlock` (qui écrit `pass` si le corps est vide).

Collections :

| SARL | Python |
|---|---|
| `#[1, 2]` | `[1, 2]` |
| `#{1, 2}` | `{1, 2}` |
| `new ArrayList<T>()` | `[]` |
| `new HashSet<T>()` | `set()` ← **jamais** `{}`, qui est un dictionnaire |
| `liste.add(e)` | `liste.append(e)` |
| `liste.get(i)` | `liste[i]` |
| `liste.size()` | `len(liste)` |
| `x.contains(e)` | `(e in x)` |
| `ensemble.add(e)` | `ensemble.add(e)` |

`generateListSetCall(MemberCall call)` est appelée par le Membre 2 : tu lis le type du receveur avec
`info.typeOf(call.receiver())` pour savoir si c'est une liste ou un ensemble (`append` contre `add`), puis tu
écris le texte Python. Tu écris le receveur avec `expression(call.receiver())` et les arguments de la même
façon. **Tu ne réécris pas** la traduction des expressions : tu la **réutilises**.
Pour `generateNew`, ne traite que `ArrayList` et `HashSet` : le Membre 4 ajoute `HashMap` / `TreeMap`.

## Étape 6 : testes à la main

Avant les tests, essaie avec un fichier `examples/Mien.sarl` (l'exemple en haut de cette fiche) et lance le
transpileur. Regarde le Python. Un bloc qui a une mauvaise indentation est **l'**erreur classique : vérifie
que `indent++` / `indent--` encadrent bien chaque corps.

## Étape 7 : les tests, `ControlCollectionTest.java` (1 h)

| Test | SARL (dans `agent A { on Initialize { ... } }`) | Ce qu'on vérifie |
|---|---|---|
| `if` simple | `if (true) { info("a") }` | `if True:` puis `self.info("a")` indenté |
| `if` / `else` | `if (true) { info("a") } else { info("b") }` | `else:` présent |
| `else if` | `if (true) { } else if (false) { } else { }` | `elif False:` |
| `if` imbriqué | un `if` dans un `if` | indentation de 2 niveaux correcte |
| `while` | `while (true) { info("a") }` | `while True:` puis le corps |
| corps vide | `while (true) { }` | `pass` |
| condition non booléenne | `if (1) { }` | échec, `TYPE_MISMATCH` |
| `if` sans accolades | `if (true) info("a")` | échec, `MISSING_BRACE` |
| liste | `var l = new ArrayList<Integer>()` | `l = []` |
| liste avec valeurs | `var l = #[1, 2]` | `l = [1, 2]` |
| **ensemble vide** | `var s = new HashSet<Integer>()` | `s = set()` et **pas** `s = {}` |
| ensemble avec valeurs | `var s = #{1, 2}` | `s = {1, 2}` |
| `add` sur liste / sur ensemble | `l.add(1)` / `s.add(1)` | `l.append(1)` / `s.add(1)` |
| opération réservée à List | `s.get(0)` sur un `Set` | échec, `UNSUPPORTED_FEATURE` |
| mauvais type d'élément | `var l = new ArrayList<Integer>() l.add("x")` | échec, `TYPE_MISMATCH` |
| `add` utilisé comme valeur | `var b = l.add(1)` | échec, `TYPE_MISMATCH` |
| littéral vide | `var l = #[]` | échec, `UNSUPPORTED_FEATURE` |

Pour tester le Python complet des corps, utilise `assertTrue(r.python().contains("..."))` comme dans
`InitializeTest`. Les tests qui utilisent `i < 3`, `i = i + 1` ou des appels de méthodes attendent les PR des
Membres 1 et 2 : écris-les `@Disabled` puis active-les quand elles sont fusionnées.

## Étape 8 : pousser

`git add .`, `git commit -m "Membre 3 niveau 2 : if/while, List et Set"`, `git push -u origin membre3-controle-collections`,
puis une Pull Request relue par un autre membre avant fusion.

## Checklist de fin

- [ ] `parseIf`, `parseWhile`, `parseCollectionLiteral`, `parseNew` écrits
- [ ] `checkIf`, `checkWhile`, `typeOfCollectionLiteral`, `typeOfNew`, `typeOfListSetCall` écrits
- [ ] `generateIf` (avec `elif`), `generateWhile`, collections, `generateListSetCall` écrits
- [ ] **`set()` pour l'ensemble vide**, jamais `{}`
- [ ] les tests de `ControlCollectionTest` verts ; `mvn test` entièrement vert
- [ ] tu n'as pas modifié `SyntaxTree.java`, ni les méthodes des autres membres
- [ ] tu sais expliquer chaque ligne (règle du groupe : signaler l'aide d'une IA par un commentaire)

## Questions pour vérifier que tu maîtrises ton code

1. Que se passe-t-il, étape par étape, pour `if (x < 3) { info("a") } else { info("b") }` (Lexer, Parser,
   Semantic, Générateur) ?
2. Pourquoi réutilises-tu `parseBlock()` et `generateBlock()` plutôt que de réécrire la lecture d'un bloc ?
3. Pourquoi le corps d'un `if` a-t-il sa propre portée (`checkBlock`) ? Que se passe-t-il d'une variable
   déclarée dedans, après le `if` ?
4. Pourquoi un `if` sans accolades est-il refusé ?
5. Pourquoi `#{}` vide est-il refusé alors que `new HashSet<Integer>()` donne `set()` ?
6. Pourquoi `{}` est-il un dictionnaire en Python et pas un ensemble vide ?
7. `liste.add(e)` devient `liste.append(e)` mais `ensemble.add(e)` reste `add` : comment ton générateur
   choisit-il, puisqu'il ne voit que le texte `liste` ? (Réponse attendue : `TypeInfo`.)
8. Pourquoi `add` est-il de type `void` dans le sous-ensemble ?

## Si tu es bloqué

- Mauvaise indentation : affiche le Python (`System.out.println(r.python())`) et compte les `indent`.
- Erreur de type que tu ne comprends pas : regarde `r.errors()` (code, ligne, colonne, message).
- La méthode d'un autre membre n'existe pas encore : teste avec des expressions simples (`true`, un nombre).
- Dis-le au groupe avant de modifier un fichier partagé.
