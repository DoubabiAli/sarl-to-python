# Niveaux 2 + 3, Membre 1 : variables, types, affectations, méthodes (et PR 0)

**Ta mission** : faire reconnaître, vérifier et traduire les déclarations `var` / `val`, les affectations
et les méthodes `def`, avec des types et des portées fiables. Tu écris aussi la **PR 0** (le squelette
commun) et tu fournis aux autres la résolution « champ ou variable locale ».

```sarl
agent A {
    var total : int = 0
    def ajoute(a : int) : int {
        var r = total + a
        total = r
        return r
    }
}
```

doit devenir

```python
from sarl_runtime import Agent


class A(Agent):
    def __init__(self, runtime=None):
        super().__init__(runtime)
        self.total = 0

    def ajoute(self, a):
        r = self.total + a
        self.total = r
        return r
```

Temps estimé : ta part de la PR 0 (0a) environ 1 h 30, puis 6 à 8 h pour ta partie (lecture comprise).

**Lis d'abord `docs/membres/NIVEAU2_CONTRAT.md`** : il définit le sous-ensemble SARL, les nœuds, les noms
des méthodes et les règles du Python généré. Cette fiche ne le répète pas.

---

## Étape 0 : préparer ton poste (15 min)

1. Le niveau 1 doit être fusionné dans `main` (voir contrat, §10). Puis : `git checkout main`, `git pull`.
2. `mvn test` : tout est vert. (Dans le terminal, `JAVA_HOME` doit pointer sur le JDK 25.)
3. Branche de la PR 0 : `git checkout -b pr0-contrat-niveau2`.

## Étape 1 : ta part de la PR 0 (1 h 30)

La PR 0 est répartie **par fichier** entre les quatre membres (contrat, §10). Elle ne contient **aucune
logique** : seulement des définitions et des méthodes `notImplemented`. **Ta part est la PR 0a**
(lignes 1, 4, 5 et 6 du tableau, plus `Types.java`), à fusionner **en premier**, car les autres en ont besoin
pour compiler. Les autres lignes sont écrites par d'autres membres (indiqué sous le tableau) : tu les lis
pour savoir ce qui existera. Chaque PR compile et ne casse aucun test. Lance `mvn test` après chaque point :

| # | Fichier | À faire |
|---|---|---|
| 1 | `Diagnostics.java` | ajouter `UNKNOWN_SYMBOL`, `TYPE_MISMATCH`, `IMMUTABLE_ASSIGNMENT`, `WRONG_ARGUMENT_COUNT` |
| 2 | `Tag.java` | ajouter les tokens du contrat, §3 |
| 3 | `Lexer.java` | ajouter les mots réservés à `KEYWORDS` ; ajouter les symboles d'un caractère à `getSymbol()` ; écrire `getOperator()` pour les symboles de deux caractères (`==`, `!=`, `<=`, `>=`, `&&`, `\|\|`, `->`) en diagramme de transition, comme `getNumber()` |
| 4 | `SyntaxTree.java` | ajouter **tous** les nœuds du contrat, §4, et le champ `imports` de `ProgramNode` |
| 5 | `SymbolTable.java` | agrandir `Symbol` (garder l'ancien constructeur) ; ajouter `getLocal(nom)` |
| 6 | `TypeInfo.java` (nouveau) | les deux dictionnaires par identité (`IdentityHashMap`) et leurs accesseurs |
| 7 | `Transpiler.java` | créer le `TypeInfo`, le passer à `Semantic` et à `PythonGenerator` (changer leurs constructeurs) |
| 8 | `Parser.java` | aiguillages (`parseStatement`, `parseMember`, `parse()`, `parsePrimary`) ; mettre à jour **les ensembles de synchronisation** ; squelettes `notImplemented` des 4 membres |
| 9 | `Semantic.java` | `checkAgent` en deux passes, `checkBlock`, `checkStatement` (aiguillage), `typeOf` minimal, squelettes |
| 10 | `PythonGenerator.java` | cas de `generateStatement` / `generateMember` / `expression` pour les nouveaux nœuds, squelettes |
| 11 | `runtime/check_generated.py` | le script `ast.parse` (voir plus bas) |
| 12 | `docs/` | `GRAMMAIRE.md` (les nouvelles règles) et `TABLE_LL1.md` (FIRST et FOLLOW) |

**Qui fait quoi** : toi, 0a (lignes 1, 4, 5, 6 et `Types.java`) puis 0e (ligne 12) ; Membre 4, 0b (lignes 2, 3,
11) ; Membre 2, 0c (ligne 8) ; Membre 3, 0d (lignes 7, 9, 10).

`Types.java` : `isAssignable(cible, source)` avec **toutes** les règles du contrat, §5 (même type,
`int` vers `double`, **types boîtes** `int`/`Integer`, `double`/`Double`, `boolean`/`Boolean` via `unbox`,
`ArrayList` comme `List`, etc., et les types littéraux **immuables**), `isNumeric`, `isBoolean`, et `VOID`.
Les trois autres s'en servent : écris-la tôt et dis-leur.

Pour le point 8 (écrit par le Membre 2), voici le principe pour `parseStatement` (le début existe déjà) :

```java
private SyntaxTree.Statement parseStatement() {
    if (check(Tag.INFO)) return parseInfo();
    if (check(Tag.SPAWN)) return parseSpawn();
    if (check(Tag.KILLME)) return parseKillMe();
    if (check(Tag.VAR) || check(Tag.VAL)) return parseVarDecl();     // M1
    if (check(Tag.IF)) return parseIf();                             // M3
    if (check(Tag.WHILE)) return parseWhile();                       // M3
    if (check(Tag.RETURN)) return parseReturn();                     // M2
    if (check(Tag.ID) || check(Tag.THIS)) return parseIdStatement(); // M2
    throw error(Diagnostics.Code.UNSUPPORTED_FEATURE, "instruction non supportée");
}
```

Et la synchronisation dans `parseBlock` devient
`synchronize(Tag.INFO, Tag.SPAWN, Tag.KILLME, Tag.VAR, Tag.VAL, Tag.IF, Tag.WHILE, Tag.RETURN, Tag.ID, Tag.THIS, Tag.RBRACE)` ;
dans `parseAgent` : `synchronize(Tag.ON, Tag.USES, Tag.VAR, Tag.VAL, Tag.DEF, Tag.RBRACE)`.
(Pourquoi ? Après une erreur, le Parser saute des tokens jusqu'à un token qui peut **commencer** une
instruction ou finir le bloc : c'est `FIRST ∪ FOLLOW`, voir `docs/TABLE_LL1.md`.)

Quand la PR 0 est prête : ouvre la PR et demande aux 3 autres de la **valider** (ils doivent pouvoir
écrire leurs méthodes sans rien changer à ce que tu as défini). Fusionne seulement après leur accord.

`runtime/check_generated.py` (≈ 25 lignes) : pour chaque fichier de `examples/*.sarl`, lance
`java -cp target/classes Transpiler fichier.sarl generated`, puis `ast.parse` sur le `.py` produit, et
affiche OK ou l'erreur. Les exemples qui doivent échouer (`Integration.sarl` tant que le niveau 1 n'est
pas fini) sont listés à part.

## Étape 2 : comprendre ta partie (30 min)

Après la PR 0, tu remplis **tes** méthodes, dans cet ordre :

| Ordre | Fichier | Méthodes |
|---|---|---|
| 1 | `Parser.java` | `parseType`, `parseVarDecl`, `parseAssignRest`, `parseMethod` |
| 2 | `Semantic.java` | `resolveName`, `declareMembers`, `checkMethod`, `checkVarDecl`, `checkAssign` |
| 3 | `PythonGenerator.java` | `pyName`, `generateConstructor`, `generateMethod`, `generateVarDecl`, `generateAssign` |
| 4 | `VariableMethodTest.java` | les tests de l'étape 6 |

Hors de ta tâche : les opérateurs et les appels (Membre 2), `if`/`while` et `List`/`Set` (Membre 3),
maps et imports (Membre 4). Pour la valeur d'une variable tu appelles **`parseExpression()` du Membre 2**
(en attendant, celle qui existe lit un nombre, une chaîne ou un nom).

## Étape 3 : écrire le Parser (1 h 30)

```java
private SyntaxTree.TypeRef parseType() {
    Token name = expect(Tag.ID, Diagnostics.Code.SYNTAX_ERROR, "nom de type attendu");
    List<SyntaxTree.TypeRef> args = new ArrayList<>();
    if (match(Tag.LT)) {                                  // type générique : Map<String, Integer>
        do {
            args.add(???);                                // quel appel lit un type ? (récursion)
        } while (match(Tag.COMMA));
        expect(Tag.GT, Diagnostics.Code.SYNTAX_ERROR, "'>' attendu");
    }
    return new SyntaxTree.TypeRef(name.lexeme(), args, name.line(), name.column());
}

private SyntaxTree.Statement parseVarDecl() {
    Token keyword = advance();                            // var ou val
    boolean mutable = keyword.tag() == Tag.VAR;
    Token name = expect(Tag.ID, Diagnostics.Code.MISSING_IDENTIFIER, "nom de variable attendu");
    SyntaxTree.TypeRef type = null;
    if (match(Tag.COLON)) type = ???;                     // le type
    SyntaxTree.Expression init = null;
    if (match(Tag.ASSIGN)) init = ???;                    // la valeur : parseExpression()
    match(Tag.SEMICOLON);
    if (type == null && init == null) {
        throw error(Diagnostics.Code.SYNTAX_ERROR, "type ou valeur initiale attendu");
    }
    if (!mutable && init == null) {
        throw error(Diagnostics.Code.SYNTAX_ERROR, "une valeur 'val' doit avoir une valeur initiale");
    }
    return new SyntaxTree.VarDecl(mutable, name.lexeme(), type, init, keyword.line(), keyword.column());
}
```

`parseVarDecl` sert pour un champ (appelée depuis `parseMember`) **et** pour une variable locale (depuis
`parseStatement`) : même méthode, le contexte ne change que la façon dont Semantic et le Générateur
l'utilisent.

`parseAssignRest(Expression cible)` : appelée par le Membre 2 quand, après avoir lu `x` ou `this.x`, il voit `=`.

```java
private SyntaxTree.Statement parseAssignRest(SyntaxTree.Expression target) {
    if (!(target instanceof SyntaxTree.VariableRef) && !(target instanceof SyntaxTree.FieldRef)) {
        throw error(Diagnostics.Code.SYNTAX_ERROR, "on ne peut affecter qu'une variable ou un champ");
    }
    expect(Tag.ASSIGN, Diagnostics.Code.SYNTAX_ERROR, "'=' attendu");
    SyntaxTree.Expression value = ???;                    // parseExpression()
    match(Tag.SEMICOLON);
    return new SyntaxTree.Assign(target, value, target.line(), target.column());
}
```

`parseMethod()` : consommer `def`, le nom (`ID`), `(`, puis **tant qu'il y a des paramètres** : `ID`, `:`
(obligatoire : « les paramètres sont typés »), un type, une virgule éventuelle ; puis `)` ; puis, si
`:` suit, le type de retour ; enfin le corps avec **`parseBlock()`**. Retourne un `MethodDecl`.
Le cas « aucun paramètre » (`def f() { }`) doit marcher : teste `check(Tag.RPAREN)` avant la boucle.

## Étape 4 : écrire le Semantic (2 h)

Règles : un nom est unique **dans sa portée** ; un `val` et un paramètre ne se réaffectent pas ; on ne
déclare pas deux fois le même nom dans la même portée ; un nom doit exister avant d'être utilisé.

```java
/** Cherche un nom ; le note dans TypeInfo (champ ou local) ; signale UNKNOWN_SYMBOL s'il n'existe pas. */
Symbol resolveName(SyntaxTree.VariableRef ref, SymbolTable scope) {
    Symbol s = scope.get(ref.name());
    if (s == null) { diag.report(UNKNOWN_SYMBOL, ref.line(), ref.column(), "nom inconnu : " + ref.name()); return null; }
    info.setKind(ref, s.kind());       // "field", "local" ou "param"
    return s;
}

void checkVarDecl(SyntaxTree.VarDecl v, SymbolTable scope) {
    if (scope.getLocal(v.name()) != null) {          // doublon dans la MÊME portée
        diag.report(DUPLICATE_DECLARATION, ???);
        return;
    }
    TypeRef initType = v.init() == null ? null : typeOf(v.init(), scope);   // Membre 2
    TypeRef type = v.type() != null ? v.type() : initType;                  // inférence : var x = 0
    if (type == null) { /* UNSUPPORTED_FEATURE : "type non déductible" */ }
    if (v.type() != null && initType != null && !Types.isAssignable(v.type(), initType)) {
        /* TYPE_MISMATCH à la position de v */
    }
    scope.declare(v.name(), new Symbol(v.name(), ???, v.line(), v.column(), type, v.mutable(), null));
}
```

Indices :
- `kind` vaut `"field"` si on est dans la passe A de `declareMembers`, `"local"` sinon, `"param"` pour un paramètre.
- La classe `Types` est celle de la PR 0a : `isAssignable` accepte `Integer` pour `int` (types boîtes), refuse
  d'affecter un littéral immuable à un type `List`/`Set`/`Map` déclaré (`UNSUPPORTED_FEATURE`, « littéral
  immuable »), et ne confond pas `TYPE_MISMATCH` (types incompatibles) avec `UNSUPPORTED_FEATURE`
  (construction valide en SARL qu'on ne traduit pas).
- **Portées Python** : un `if` ou un `while` ne crée pas de portée en Python. Dans `checkVarDecl`, cherche
  le nom dans **toutes les portées de la méthode** (pas seulement `getLocal`) : s'il désigne déjà un
  paramètre ou une variable locale, `DUPLICATE_DECLARATION` (« déjà déclaré dans une portée englobante »).
  Seul un **champ** peut être masqué par un local.
- `requireValue(type, nœud)` : si `type` est `VOID`, signale `UNSUPPORTED_FEATURE` (« valeur de retour de
  add/put non supportée »). Appelle-la dans `checkVarDecl` et `checkAssign`, et donne-la aux autres.
- `alwaysReturns(corps)` : vrai si la dernière instruction est un `ReturnStatement`, ou un `if` **avec** `else`
  dont **les deux** corps retournent (récursion). `checkMethod` signale `UNSUPPORTED_FEATURE` si la méthode
  déclare un type de retour et que `alwaysReturns` est faux (sinon le Python renverrait `None`).
- `checkMethod` : ouvre une portée (`scope.openContext()`), y déclare les paramètres (non mutables), met
  `currentReturnType` (le type de retour, ou `null`), appelle `checkBlock(corps, portée)`, puis remet
  `currentReturnType` à `null` et ferme la portée.
- `declareMembers` (passe A) : pour chaque `VarDecl` et chaque `MethodDecl` de l'agent, déclarer le nom dans
  la portée de l'agent (doublon : `DUPLICATE_DECLARATION`). Une méthode reçoit une `signature`.
- `checkAssign` : cible introuvable → `UNKNOWN_SYMBOL` ; symbole non mutable → `IMMUTABLE_ASSIGNMENT` ;
  type de la valeur non compatible → `TYPE_MISMATCH`. Pour une `FieldRef` (`this.x`), cherche le champ dans
  la portée de l'agent.

## Étape 5 : écrire le Générateur (1 h 30)

```java
/** Écrit __init__ s'il y a au moins un champ (voir contrat, §8). */
private void generateConstructor(SyntaxTree.AgentNode agent) {
    ... line("def __init__(self, runtime=None):");
    indent++;
    line("super().__init__(runtime)");
    for (member : agent.members()) if (member instanceof SyntaxTree.VarDecl field) {
        line("self." + pyName(field.name()) + " = " + (field.init() != null ? expression(field.init()) : defaultValue(field)));
    }
    indent--;
}
```

- `defaultValue` : `0` pour `int`, `0.0` pour `double`, `False` pour `boolean`, `None` sinon.
- `generateAgent` : appelle `generateConstructor` avant la boucle sur les membres, et `generateMember`
  **ignore** les `VarDecl` (déjà écrits dans `__init__`). Attention à la ligne vide entre deux méthodes.
- `generateVarDecl` (variable locale) : `line(pyName(nom) + " = " + valeur)`. Sans valeur : la valeur par défaut.
- `generateAssign` : la cible est `self.x` si `info.kindOf(cible)` est `"field"`, sinon `x`.
- `generateMethod` : `def nom(self, a, b):` puis le corps avec `generateBlock` (`pass` si vide).
- `pyName(nom)` : ajoute `_` si `nom` est un mot réservé de Python (`False None True and as assert async
  await break class continue def del elif else except finally for from global if import in is lambda
  nonlocal not or pass raise return try while with yield`).
- **Sans toucher à `expression(...)`** : c'est le Membre 2 qui le complète. Pour un nom, il t'appelle
  (`self.` + `pyName` pour un champ).

## Étape 6 : les tests, `VariableMethodTest.java` (1 h)

| Test | SARL à transpiler | Ce qu'on vérifie |
|---|---|---|
| champ `var` et `val` | `agent A { var x : int = 1 val y = "a" }` | `__init__` contient `self.x = 1` et `self.y = "a"` |
| pas d'attribut de classe | `agent A { var x : int = 1 }` | le Python ne contient pas `    x = 1` au niveau de la classe |
| variable locale sans `self.` | `agent A { def f() { var z = 1 info("a") } }` | `z = 1`, et pas `self.z` |
| local qui cache un champ | `agent A { var x : int = 1 def f() { var x = 2 } }` | dans `f`, `x = 2` sans `self.` |
| doublon | `agent A { def f() { var a = 1 var a = 2 } }` | échec, `DUPLICATE_DECLARATION` |
| `val` réaffecté | `agent A { def f() { val a = 1 a = 2 } }` | échec, `IMMUTABLE_ASSIGNMENT` (après le Membre 2 pour `a = 2`) |
| méthode | `agent A { def f(a : int, b : int) : int { return a } }` | `def f(self, a, b):` puis `return a` |
| paramètre visible seulement dans sa méthode | une méthode qui utilise `a` en dehors | échec, `UNKNOWN_SYMBOL` |
| mot réservé Python | `agent A { var from : int = 1 }` | `self.from_ = 1` |
| inférence | `agent A { def f() { var a = 1 a = "x" } }` | échec, `TYPE_MISMATCH` |
| types boîtes | `agent A { def f() { var n : int = 1 var b : Integer = n } }` | succès |
| masquer une variable locale | `agent A { def f() { var a = 1 if (true) { var a = 2 } } }` | échec, `DUPLICATE_DECLARATION` |
| méthode sans `return` | `agent A { def f() : int { } }` | échec, `UNSUPPORTED_FEATURE` |
| `return` sur tous les chemins | `def f(b : boolean) : int { if (b) { return 1 } else { return 2 } }` | succès |
| littéral immuable | `agent A { def f() { var l : List<Integer> = #[1] } }` | échec, `UNSUPPORTED_FEATURE` |

Les tests qui ont besoin de `a = 2` ou d'opérateurs attendent le Membre 2 ; écris-les quand sa PR est
fusionnée (tu peux les laisser `@Disabled` d'ici là).

## Étape 7 : documenter et pousser

- Mets à jour `docs/GRAMMAIRE.md` et `docs/TABLE_LL1.md` avec **les règles de tous les membres** (c'est
  ton rôle). Pour `x = 1` contre `x(1)` : la règle est factorisée par le Membre 2 (`ID` puis `=` ou `(`).
- Écris dans un fichier `docs/NIVEAU2_LIMITES.md` ce qui n'est pas supporté (contrat, §2).
- `git add .` puis `git commit`, `git push -u origin <ta-branche>`, puis une Pull Request, relue par un
  autre membre avant fusion.

## Checklist de fin

- [ ] PR 0 fusionnée, **avant** que les autres commencent
- [ ] `parseType`, `parseVarDecl`, `parseAssignRest`, `parseMethod` écrits
- [ ] `resolveName`, `declareMembers`, `checkMethod`, `checkVarDecl`, `checkAssign`, `requireValue`, `alwaysReturns` écrits ; `Types` partagée (types boîtes, littéraux immuables)
- [ ] champs dans `__init__`, champ/local bien distingués, `pyName`
- [ ] tests de `VariableMethodTest` verts ; `mvn test` entièrement vert
- [ ] `runtime/check_generated.py` passe sur les exemples du sous-ensemble
- [ ] `GRAMMAIRE.md`, `TABLE_LL1.md`, limites documentés
- [ ] tu sais expliquer chaque ligne (règle du groupe : signaler l'aide d'une IA par un commentaire)

## Questions pour vérifier que tu maîtrises ton code

1. Pourquoi un champ liste doit-il être créé dans `__init__` et pas au niveau de la classe ? Donne un
   exemple de bug.
2. Pourquoi le Semantic fait-il **deux passes** sur un agent ?
3. À quoi sert `TypeInfo` ? Pourquoi le Générateur ne peut-il pas deviner seul s'il faut écrire `self.x` ?
4. `var x = 2` dans une méthode et un champ `x` : lequel est utilisé dans la méthode ? Pourquoi (la chaîne de portées) ?
5. Que fait `getLocal` de plus que `get` ? Pourquoi `get` ne suffit-il pas pour détecter un doublon ?
6. Pourquoi le Parser accepte-t-il `<` pour un type dans `x : List<Integer>` mais le traite comme une
   comparaison dans `a < b` ? Qui prend la décision ?
7. Pourquoi les ensembles de synchronisation ont-ils dû changer ?

## Si tu es bloqué

- Erreur de compilation Java : le message donne le fichier et la ligne.
- Un test échoue : affiche le Python avec `System.out.println(r.python())` et les erreurs avec `r.errors()`.
- Une interface des autres ne te convient pas : dis-le au groupe **avant** de modifier leur méthode.
