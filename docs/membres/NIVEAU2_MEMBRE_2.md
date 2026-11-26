# Niveaux 2 + 3, Membre 2 : expressions, appels, `return`

**Ta mission** : construire **le seul moteur d'expressions** du transpileur : priorités et parenthèses,
calculs, comparaisons, logique booléenne, appels de méthodes et `return`. Tous les autres membres
appellent ton `parseExpression()` : personne n'en écrit un second.

```sarl
agent A {
    def calcule(a : int, b : int) : boolean {
        return (a + b) * 2 > 10 && !(a == b)
    }
}
```

doit devenir

```python
class A(Agent):
    def calcule(self, a, b):
        return (a + b) * 2 > 10 and not (a == b)
```

Temps estimé : 6 à 8 h, lecture comprise. Ta partie est la plus **utilisée** par les autres : livre
**vite** une première version de `parseExpression()` (nombres, noms, opérateurs), même sans tous les cas.

**Lis d'abord `docs/membres/NIVEAU2_CONTRAT.md`.**

---

## Étape 0 : préparer ton poste (15 min)

1. La PR 0 du Membre 1 doit être fusionnée. `git checkout main`, `git pull`.
2. `mvn test` : tout est vert (le terminal doit utiliser le JDK 25).
3. `git checkout -b membre2-expressions`.
4. **Niveau 1 restant** : `uses Lifecycle` et `spawn` (`docs/membres/README_MEMBRE_2.md`). Si le groupe te les
   a confiés (contrat, §1), fais-les **en premier** : ce sont de petites méthodes, et les 5 `SpawnTest` en
   dépendent.

## Étape 1 : comprendre le projet (1 h)

| # | Fichier | À comprendre |
|---|---|---|
| 1 | `docs/NIVEAU2_CONTRAT.md` (§2, 4, 6, 8) | les nœuds `BinaryOp`, `UnaryOp`, `Call`, `MemberCall` et les règles Python |
| 2 | `docs/PARSER.md` | descente récursive : une méthode par règle |
| 3 | `docs/TABLE_LL1.md` | FIRST et FOLLOW : pourquoi une règle d'expression ne doit pas être récursive à gauche |
| 4 | `Parser.java`, `parseExpression()` actuelle | elle lit un nombre, une chaîne ou un nom : tu la remplaces |
| 5 | `PythonGenerator.java`, `expression(...)` | elle traduit les trois littéraux : tu l'étends |

## Étape 2 : comprendre ta tâche (30 min)

| Ordre | Fichier | Méthodes |
|---|---|---|
| 1 | `Parser.java` | `parseExpression` et ses niveaux, `parseUnary`, `parsePostfix`, `parsePrimary`, `parseArguments`, `parseReturn`, `parseIdStatement` |
| 2 | `PythonGenerator.java` | `expression(...)` étendue, `generateExprStatement`, `generateReturn`, `generateMemberCall` |
| 3 | `Semantic.java` | `typeOf`, `checkExprStatement`, `checkReturn`, `checkCall` |
| 4 | `ExpressionTest.java` | les tests de l'étape 7 |

Hors de ta tâche : les déclarations et les affectations (Membre 1, tu appelles son `parseAssignRest`),
`if`/`while`/listes (Membre 3), maps (Membre 4).

## Étape 3 : écrire le Parser : la grammaire (2 h)

La grammaire (un niveau par priorité ; chaque `{ ... }` est une **boucle**, donc pas de récursion à
gauche et un seul token d'avance suffit) :

```
<expr>    → <or>
<or>      → <and>  { || <and> }
<and>     → <eq>   { && <eq> }
<eq>      → <rel>  { ( == | != ) <rel> }
<rel>     → <add>  { ( < | > | <= | >= ) <add> }
<add>     → <mul>  { ( + | - ) <mul> }
<mul>     → <unary>{ ( * | / ) <unary> }
<unary>   → ! <unary>  |  - <unary>  |  <postfix>
<postfix> → <primary> { . ID [ ( <args> ) ] }
<primary> → NUMBER | STRING | true | false | ( <expr> )
          | this . ID | ID [ ( <args> ) ] | <collection> | new ...
<args>    → [ <expr> { , <expr> } ]
```

(Les priorités sont celles de la documentation SARL, voir le contrat.) Chaque niveau binaire s'écrit de la
même façon, par exemple le niveau `<or>` :

```java
private SyntaxTree.Expression parseOr() {
    SyntaxTree.Expression left = parseAnd();
    while (check(Tag.OR)) {
        Token op = advance();
        SyntaxTree.Expression right = ???;                 // le niveau juste en dessous
        left = new SyntaxTree.BinaryOp("||", left, right, op.line(), op.column());
    }
    return left;                                            // associativité à gauche
}
```

Écris **un seul** helper `parseBinaryLevel(...)` si tu préfères ne pas répéter 6 fois : c'est autorisé
(la méthode doit rester claire pour que tu l'expliques à l'oral). Tu peux aussi faire un « precedence
climbing » : à toi de choisir, mais tu dois savoir l'expliquer.

`parsePostfix` : lit un `primary`, puis tant que le token est `DOT` : lit un `ID` ; si `(` suit, c'est un
`MemberCall(receiver, nom, arguments)` ; sinon c'est un accès de champ non supporté
(`UNSUPPORTED_FEATURE` : « `x.champ` ») **sauf** `list.size` sans parenthèses, qui est aussi
`UNSUPPORTED_FEATURE` (contrat, §2).

`parsePrimary` : `THIS` → consommer `this`, `.`, `ID` → `FieldRef`. `ID` suivi de `(` → `Call(nom, arguments)`.
`TRUE`/`FALSE` → `BooleanLiteral` (il existe déjà). `HASH` → `return parseCollectionLiteral();` et
`NEW` → `return parseNew();` (Membre 3, déjà branchés par la PR 0). `(` → `parseExpression()` puis `)`.

`parseIdStatement` : c'est **la factorisation à gauche** du début d'instruction par `ID` ou `this`
(`x = 1` contre `x(1)`) :

```java
private SyntaxTree.Statement parseIdStatement() {
    SyntaxTree.Expression first = parsePostfix();           // x   |   this.x   |   x(1)   |   o.m(1)
    if (check(Tag.ASSIGN)) {
        return parseAssignRest(first);                      // écrit par le Membre 1
    }
    if (!(first instanceof SyntaxTree.Call) && !(first instanceof SyntaxTree.MemberCall)) {
        throw error(Diagnostics.Code.SYNTAX_ERROR, "une instruction est une affectation ou un appel");
    }
    match(Tag.SEMICOLON);
    return new SyntaxTree.ExprStatement(first, first.line(), first.column());
}
```

`parseReturn` : consommer `return` ; s'il y a une valeur (le token suivant n'est ni `RBRACE` ni
`SEMICOLON`), `parseExpression()` ; sinon `null`. Retourne un `ReturnStatement`.

## Étape 4 : écrire le Semantic : les types (2 h)

`typeOf(Expression, SymbolTable)` calcule **et note dans `TypeInfo`** le type de chaque expression. Les
règles (contrat, §5) :

| Expression | Type | Erreur si... |
|---|---|---|
| nombre | `int` (sans point) ou `double` (avec point) | |
| chaîne / `true` / `false` | `String` / `boolean` | |
| nom, `this.x` | celui du symbole (via `resolveName`, Membre 1) | inconnu : `UNKNOWN_SYMBOL` |
| `a + b`, `a - b`, `a * b`, `a / b` | numérique : `double` si l'un des deux est `double`, sinon `int` | opérande non numérique : `TYPE_MISMATCH` |
| `a + b` avec deux `String` | `String` | `String` + autre type : `TYPE_MISMATCH` |
| `<`, `>`, `<=`, `>=` | `boolean` | opérandes non numériques |
| `==`, `!=` | `boolean` | types incompatibles |
| `&&`, `\|\|`, `!` | `boolean` | opérande non booléen |
| `-a` | le type de `a` | `a` non numérique |
| `f(a, b)` | le type de retour de la méthode `f` (`void` s'il n'en a pas) | inconnue : `UNKNOWN_SYMBOL` ; mauvais nombre d'arguments : `WRONG_ARGUMENT_COUNT` ; argument incompatible : `TYPE_MISMATCH` |
| `x.m(a)` | calcule le type de `x`, puis délègue : `List`/`Set` → `typeOfListSetCall` (Membre 3), `Map` → `typeOfMapCall` (Membre 4), autre : `UNSUPPORTED_FEATURE` | |

À chaque expression analysée : `info.setType(expression, type)`. **Réutilise** `Types.isAssignable`,
`Types.isNumeric` du Membre 1, **ne les réécris pas**. Si un opérande est de type inconnu (`null`, erreur
déjà signalée), ne signale pas une deuxième erreur en cascade.

Les autres méthodes :
- `checkExprStatement(ExprStatement, scope)` : appelle `typeOf` sur l'expression.
- `checkReturn(ReturnStatement, scope)` : compare avec `currentReturnType` (rempli par le Membre 1) : `return`
  avec valeur dans une méthode sans type de retour, ou valeur incompatible, ou `return` sans valeur dans une
  méthode qui retourne un type → `TYPE_MISMATCH`.
- `info(expr)` : l'argument ne doit **pas** être une collection (`List`/`Set`/`Map`) : `UNSUPPORTED_FEATURE`
  (l'affichage Java et l'affichage Python ne sont pas les mêmes). C'est dans le cas `InfoStatement` de
  `checkStatement`.

## Étape 5 : écrire le Générateur (1 h 30)

`expression(...)` traduit chaque nœud. Règles principales (contrat, §8) :
- `BooleanLiteral` : `True` / `False` ; `&&` → `and`, `||` → `or`, `!` → `not`.
- Un nom : si `info.kindOf(ref)` vaut `"field"` → `"self." + pyName(nom)` ; sinon `pyName(nom)`.
  (`pyName` est écrite par le Membre 1.) `FieldRef` (`this.x`) → `self.x`.
- `Call` : `self.f(args)` (`f` est une méthode de l'agent).
- Division : si **les deux** opérandes sont de type `int` (`info.typeOf(...)`), écrire `int(a / b)`.

**Parenthèses.** Donne à chaque opérateur un niveau : `or` 1, `and` 2, `==`/`!=` 3, comparaisons 4,
`+`/`-` 5, `*`/`/` 6, unaire 7. Règle pour l'opérande d'un opérateur de niveau `n` :
- on le met entre parenthèses si son niveau est **plus bas** que `n` ;
- ou s'il est de même niveau **et à droite** (`a - (b - c)` ne doit pas devenir `a - b - c`) ;
- **une comparaison dont l'opérande est lui-même une comparaison est toujours parenthésée** (en Python,
  `a < b < c` veut dire autre chose) ;
- l'opérande de `not` ou de `-` unaire est parenthésé s'il n'est pas un atome (littéral, nom, appel).

`generateExprStatement` : `line(expression(e.expression()))`. `generateReturn` : `line("return " + ...)` ou
`line("return")`. `generateMemberCall` : lis le type du receveur dans `TypeInfo`. Si c'est une liste ou un
ensemble, appelle `generateListSetCall(call)` (Membre 3) ; si c'est une map, `generateMapCall(call)`
(Membre 4).

## Étape 6 : travailler sans bloquer les autres

- Livre ta première version (nombres, noms, `+ - * /`, parenthèses, comparaisons) **dès que possible**,
  même si les appels ne sont pas finis. Pousse une PR courte.
- Les Membres 3 et 4 testent avec des expressions « factices » en attendant : ne change pas les noms de
  nœuds du contrat.

## Étape 7 : les tests, `ExpressionTest.java` (1 h)

| Test | SARL (dans un `on Initialize { ... }`) | Ce qu'on vérifie |
|---|---|---|
| priorité | `var a = 1 + 2 * 3` et `var b = (1 + 2) * 3` | `1 + 2 * 3` et `(1 + 2) * 3` : les deux Python sont différents |
| soustraction à droite | `var a = 5 - (3 - 1)` | le Python garde les parenthèses |
| opérateurs logiques | `var a = true && !false \|\| false` | `True and not False or False` |
| comparaison valide | `var a = 1 < 2` | succès |
| comparaisons chaînées | `var a = (1 < 2) == true` | le Python parenthèse `(1 < 2) == True` |
| types incompatibles | `var a = 1 + "x"` | échec, `TYPE_MISMATCH` |
| appel, mauvais nombre d'arguments | méthode `f(a : int)` appelée `f(1, 2)` | échec, `WRONG_ARGUMENT_COUNT` |
| appel traduit | `f(1)` dans l'agent | `self.f(1)` |
| `return` bien placé | `def f() : int { return 1 }` | `return 1` dans `def f(self):` |
| `return` incompatible | `def f() : int { return "x" }` | échec, `TYPE_MISMATCH` |
| `x = 1` contre `x(1)` | les deux | les deux se lisent correctement |
| division entière | `var a = 7 / 2` | `int(7 / 2)` |
| `info` d'une collection | `info(liste)` | échec, `UNSUPPORTED_FEATURE` |

## Étape 8 : pousser

`git add .`, `git commit`, `git push -u origin membre2-expressions`, Pull Request relue par un autre
membre.

## Checklist de fin

- [ ] `parseExpression()` écrite (priorités + associativité), `parsePostfix`, `parsePrimary`, `parseIdStatement`, `parseReturn`
- [ ] `typeOf` complète, notée dans `TypeInfo` ; `checkReturn`, `checkExprStatement` écrites
- [ ] `expression(...)` avec les bonnes parenthèses, la division entière, les noms champ/local
- [ ] les tests de `ExpressionTest` verts ; `mvn test` entièrement vert
- [ ] aucune seconde implémentation d'expression ailleurs dans le projet
- [ ] tu sais expliquer chaque ligne (règle du groupe : signaler l'aide d'une IA par un commentaire)

## Questions pour vérifier que tu maîtrises ton code

1. Pourquoi écrit-on `<or> → <and> { || <and> }` avec une boucle plutôt que `<or> → <or> || <and>` ?
2. Comment l'ordre des méthodes (`parseOr` appelle `parseAnd` qui appelle ...) crée-t-il les priorités ?
3. Que fait ton Parser pour `x = 1` et pour `x(1)` ? Pourquoi un seul token d'avance ne suffit-il pas
   à les distinguer, et comment la factorisation à gauche résout-elle ça ?
4. Pourquoi `a - (b - c)` doit-il garder ses parenthèses en Python ?
5. Pourquoi `a < b < c` est-il dangereux en Python ?
6. SARL fait `7 / 2 = 3` et Python `7 / 2 = 3.5`. Qu'est-ce que tu génères et pourquoi faut-il connaître le type ?
7. Pourquoi `"n=" + 1` est-il refusé ?

## Si tu es bloqué

- Affiche l'arbre ou le Python : `System.out.println(r.python())`.
- Une priorité est fausse : fais un test minuscule (`1 + 2 * 3`) et regarde l'ordre des appels.
- Dis au groupe **avant** de modifier un nœud du contrat.
