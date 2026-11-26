# Niveaux 2 + 3 : le contrat commun (PR 0)

Ce document est lu par **les quatre membres avant d'écrire du code**. Il dit quoi construire en commun
(la « PR 0 »), qui l'écrit, et ce que chacun remplit ensuite. Même méthode qu'au niveau 1 : un
squelette qui compile, avec des méthodes `notImplemented`, puis chacun remplit **uniquement ses
méthodes**.

> Les noms ci-dessous sont une **proposition du groupe à valider** (réunion de 15 min). Si vous changez
> un nom, changez-le dans ce fichier : c'est la seule source de vérité. Les consignes réelles du
> professeur passent avant ce document.

---

## 1. Qui fait quoi

| Membre | Mission | Fiche |
|---|---|---|
| 1 | `var` / `val`, affectations, types, portées, méthodes `def`, **coordonne la PR 0** | `NIVEAU2_MEMBRE_1.md` |
| 2 | `parseExpression()` unique, opérateurs, appels, `return`, typage des expressions | `NIVEAU2_MEMBRE_2.md` |
| 3 | `if` / `else` / `while`, `List` et `Set` | `NIVEAU2_MEMBRE_3.md` |
| 4 | `HashMap`, `TreeMap`, imports | `NIVEAU2_MEMBRE_4.md` |

Le niveau 1 n'est pas fini : `uses Lifecycle` et `spawn` (Membre 2, `README_MEMBRE_2.md`) restent à faire.
Le groupe décide qui les prend, et le note ici : **Niveau 1 restant : ______**.

## 2. Le sous-ensemble SARL supporté

Syntaxe vérifiée sur la documentation officielle (liens en fin de document). Tout ce qui n'est pas
dans ce tableau donne `UNSUPPORTED_FEATURE`, jamais un oubli silencieux.

| Construction | Exemple SARL |
|---|---|
| champ ou variable | `var x : int = 0` ; `val nom = "Max"` ; `var y : double` |
| types | `int`, `double`, `boolean`, `String`, `List<T>`, `Set<T>`, `Map<K,V>`, `ArrayList<T>`, `HashSet<T>`, `HashMap<K,V>`, `TreeMap<K,V>` |
| méthode | `def somme(a : int, b : int) : int { return a + b }` (paramètres typés ; type de retour facultatif) |
| affectation | `x = 5` ; `this.x = 5` |
| appels | `somme(1, 2)` ; `liste.add(3)` ; `map.put("a", 1)` |
| `if` | `if (x > 0) { ... } else { ... }` ; `else if` accepté ; **accolades obligatoires** |
| `while` | `while (i < 3) { ... }` |
| `return` | `return x` ; `return` |
| liste, ensemble, map | `#[1, 2]` ; `#{1, 2}` ; `#{"a" -> 1}` ; `new ArrayList<Integer>()` ; `new HashSet<String>()` ; `new HashMap<String, Integer>()` ; `new TreeMap<String, Integer>()` |
| opérateurs | `+ - * /` ; `< > <= >=` ; `== !=` ; `&& \|\| !` ; `-x` ; parenthèses |
| import | `import java.util.List` |
| déjà au niveau 1 | `info(...)`, `spawn(...)`, `killMe`, `on Initialize`, `on Destroy` |

`;` reste facultatif.

**Hors sous-ensemble** (à refuser avec `UNSUPPORTED_FEATURE`) : `+=`, `++`, `--`, `===`, `!==`, `**`, `%`,
`for`, `switch`, lambdas, `if` utilisé comme valeur (`val a = if ...`), corps de `if`/`while` sans accolades,
`list.size` écrit **sans parenthèses**, `#[]` et `#{}` vides (ambigus : utiliser `new ...`), génériques de
méthode, `static`, `null`, concaténation `String + autre type`, `list.remove`, `firstKey` et toute opération
qui dépend de l'ordre d'une `TreeMap`.

## 3. Tokens à ajouter (Lexer et Tag) : Membre 1, dans la PR 0

| Catégorie | Tokens |
|---|---|
| mots réservés | `VAR`, `VAL`, `DEF`, `IF`, `ELSE`, `WHILE`, `RETURN`, `NEW`, `IMPORT`, `THIS` |
| symboles d'un caractère | `COLON` (`:`), `DOT` (`.`), `HASH` (`#`), `PLUS`, `MINUS`, `STAR`, `SLASH`, `ASSIGN` (`=`), `LT`, `GT`, `NOT` (`!`) |
| symboles de deux caractères | `EQ` (`==`), `NEQ` (`!=`), `LE` (`<=`), `GE` (`>=`), `AND` (`&&`), `OR` (`\|\|`), `ARROW` (`->`) |

Les types (`int`, `String`, `List`...) restent des tokens `ID`. `<` et `>` sont toujours lus comme `LT` et `GT` :
c'est le **Parser** qui décide s'ils ouvrent un type générique (après `:` ou après `new Nom`) ou s'ils
comparent. Il n'y a pas de token `>>` (`Map<String, List<Integer>>` donne deux `GT`).

## 4. Les nœuds de l'arbre (`SyntaxTree.java`) : écrits par le Membre 1 dans la PR 0

Chaque nœud a `line` et `column`. Les autres membres **n'ajoutent pas de nœud** : ils utilisent ceux-ci.

| Nœud | Champs | Utilisé par |
|---|---|---|
| `TypeRef` | `name`, `arguments` (liste de `TypeRef`) | M1, tous |
| `VarDecl` (Statement) | `mutable`, `name`, `type` (peut être `null`), `init` (peut être `null`) | M1 |
| `Assign` (Statement) | `target` (une `Expression`), `value` | M1 |
| `Param` | `name`, `type` | M1 |
| `MethodDecl` | `name`, `params`, `returnType` (peut être `null`), `body` | M1 |
| `FieldRef` (Expression) | `name` : c'est `this.nom` | M2 |
| `BinaryOp` (Expression) | `op` (texte : `"+"`, `"&&"`...), `left`, `right` | M2 |
| `UnaryOp` (Expression) | `op` (`"!"` ou `"-"`), `operand` | M2 |
| `Call` (Expression) | `name`, `arguments` | M2 |
| `MemberCall` (Expression) | `receiver`, `name`, `arguments` | M2 (M3 et M4 le traduisent) |
| `ExprStatement` (Statement) | `expression` | M2 |
| `ReturnStatement` (Statement) | `value` (peut être `null`) | M2 |
| `IfStatement` (Statement) | `condition`, `thenBody`, `elseBody` (liste vide s'il n'y a pas de `else`) | M3 |
| `WhileStatement` (Statement) | `condition`, `body` | M3 |
| `ListLiteral`, `SetLiteral` (Expression) | `elements` | M3 |
| `NewObject` (Expression) | `type` (un `TypeRef` avec ses génériques), `arguments` | M3 écrit le parser ; M3 et M4 vérifient |
| `MapLiteral` (Expression) | `entries` (liste de `MapEntry(key, value)`) | M4 |
| `ImportDecl` | `qualifiedName` (texte `java.util.List`) | M4 |

`ProgramNode` reçoit un champ en plus : `imports` (liste de `ImportDecl`). Le Membre 1 corrige tous les
`new ProgramNode(...)` de la PR 0.

## 5. Les types, les symboles et `TypeInfo` : Membre 1

- Un type est un `TypeRef`. Règles de compatibilité, **toutes dans `Types.isAssignable`** (les autres
  membres l'appellent, personne ne les réécrit) :
  - même type ; `int` est accepté là où `double` est attendu (pas l'inverse) ;
  - **types boîtes** : `int` ≡ `Integer`, `double` ≡ `Double`, `boolean` ≡ `Boolean` (sinon
    `ages.put("Max", 20)` sur une `Map<String, Integer>` serait refusé) ; `Types.unbox(type)` les normalise ;
  - `ArrayList<T>` est accepté comme `List<T>`, `HashSet<T>` comme `Set<T>`, `HashMap<K,V>` et
    `TreeMap<K,V>` comme `Map<K,V>` ;
  - **les littéraux sont immuables en SARL** : `#[..]`, `#{..}` et `#{k -> v}` ont pour type
    `ImmutableList<T>`, `ImmutableSet<T>`, `ImmutableMap<K,V>` (noms internes). Ils se lisent comme une
    `List` / `Set` / `Map` (`get`, `size`, `contains`, `containsKey`) mais `add` et `put` sont refusés
    (`UNSUPPORTED_FEATURE`, « littéral immuable »), et on ne les affecte pas à un type `List` / `Set` /
    `Map` déclaré (on perdrait l'information). Pour une collection modifiable : `new ArrayList<T>()` ;
  - le type spécial `void` n'a pas de valeur : l'utiliser comme valeur (opérande, argument,
    `var b = l.add(1)`, `return`) donne `UNSUPPORTED_FEATURE` (« valeur de retour de add/put non
    supportée » : SARL l'autorise, pas notre sous-ensemble). `Semantic.requireValue(type, nœud)` fait cette
    vérification ; tout le monde l'appelle quand une valeur est attendue.
- `var x = 0` : le type est celui de l'expression (`int`). Si on ne peut pas le déduire :
  `UNSUPPORTED_FEATURE` (« type non déductible »).
- `SymbolTable.Symbol` reçoit trois champs : `type` (`TypeRef`), `mutable` (`boolean`),
  `signature` (pour une méthode : types des paramètres et type de retour). L'ancien constructeur reste.
  `SymbolTable` reçoit `getLocal(nom)` : cherche seulement dans la portée courante (pour les doublons).
  Les genres de symboles (`kind`) : `"agent"`, `"field"`, `"local"`, `"param"`, `"method"`.
- **Pas de seconde table de symboles.** Tout le monde utilise `SymbolTable`.
- **Portées.** Python n'a qu'**une** portée par méthode (un `if` ou un `while` n'en crée pas). Une variable
  locale ne peut donc pas porter le même nom qu'un paramètre ou qu'une autre variable locale déjà visible
  (portée englobante) : `DUPLICATE_DECLARATION`. Un local peut masquer un **champ** (`x` contre `self.x`).
- **Retour.** Une méthode qui déclare un type de retour doit finir par `return valeur` sur **tous** ses
  chemins (`Semantic.alwaysReturns(corps)`), sinon `UNSUPPORTED_FEATURE` (« écrire return explicitement » :
  en SARL la dernière expression peut servir de valeur, pas chez nous).
- **`TypeInfo`** (nouveau fichier) : le Semantic y **note** ce qu'il a compris, le Générateur le **lit**.
  Il contient deux dictionnaires par identité de nœud : le type de chaque expression, et le genre de
  chaque nom (`"field"` ou `"local"`, ou `"method"` pour un appel). Sans lui, le Générateur ne saurait pas
  écrire `self.x` pour un champ et `x` pour une variable locale, ni choisir `append` ou `add`.
  `Transpiler` crée un `TypeInfo` et le donne au `Semantic` et au `PythonGenerator` (les constructeurs
  changent dans la PR 0).

## 6. Les squelettes de méthodes : écrits dans la PR 0, remplis ensuite par leur propriétaire

**Parser.java** (la règle `parseStatement` aiguille sur le premier token ; `parseMember` aussi)

| Méthode | Propriétaire | Rôle |
|---|---|---|
| `parseVarDecl()`, `parseType()`, `parseMethod()`, `parseAssignRest(Expression cible)` | M1 | déclarations, types, méthodes, `= valeur` |
| `parseExpression()` (et ses niveaux), `parseUnary()`, `parsePostfix()`, `parsePrimary()`, `parseArguments()`, `parseReturn()`, `parseIdStatement()` | M2 | expressions, appels, début d'instruction par `ID` ou `this` |
| `parseIf()`, `parseWhile()`, `parseCollectionLiteral()` (`#[` et `#{`), `parseNew()` | M3 | contrôle de flux, collections |
| `parseMapRest(Token hash, Expression premièreClé)`, `parseImport()` | M4 | maps et imports |

La PR 0 écrit **une fois pour toutes** l'aiguillage : dans `parseStatement` les cas `VAR`/`VAL`, `IF`,
`WHILE`, `RETURN`, `ID`/`THIS` ; dans `parseMember` les cas `VAR`/`VAL` et `DEF` ; dans `parse()` la boucle
`while (check(Tag.IMPORT)) imports.add(parseImport())` ; dans `parsePrimary` les cas `HASH` (vers
`parseCollectionLiteral`) et `NEW` (vers `parseNew`). **Les ensembles de synchronisation du mode panique
sont mis à jour** : une instruction peut commencer par `INFO`, `SPAWN`, `KILLME`, `VAR`, `VAL`, `IF`,
`WHILE`, `RETURN`, `ID`, `THIS` ; un membre d'agent par `ON`, `USES`, `VAR`, `VAL`, `DEF`. Chaque méthode
du tableau commence par `throw notImplemented("Membre n", "...")`.

**Semantic.java**

La PR 0 écrit le parcours : `checkAgent` fait deux passes. **Passe A** : déclarer champs et méthodes de
l'agent dans sa portée (pour qu'une méthode puisse en appeler une écrite plus bas). **Passe B** :
vérifier chaque corps (méthode, `on Initialize`, `on Destroy`) avec `checkBlock(corps, portée)`, qui ouvre
une portée, appelle `checkStatement` sur chaque instruction, puis la ferme. `checkStatement` aiguille :

| Méthode | Propriétaire |
|---|---|
| `declareMembers(agent, portée)`, `checkMethod(MethodDecl, portée)` (met `currentReturnType`), `checkVarDecl`, `checkAssign`, `resolveName(VariableRef, portée)`, `requireValue(type, nœud)`, `alwaysReturns(corps)` | M1 |
| `typeOf(Expression, portée)` (note le résultat dans `TypeInfo`), `checkExprStatement`, `checkReturn`, `checkCall` | M2 |
| `checkIf`, `checkWhile`, `typeOfListSetCall(MemberCall, TypeRef, portée)`, `typeOfNew(NewObject, portée)` pour `ArrayList`/`HashSet`, `typeOfCollectionLiteral` | M3 |
| `typeOfMapCall(MemberCall, TypeRef, portée)`, `typeOfMapLiteral`, `checkImport` | M4 |

`typeOf(MemberCall)` (M2) calcule le type du receveur : `List`/`Set` → `typeOfListSetCall` (M3), `Map` →
`typeOfMapCall` (M4), autre type → `UNSUPPORTED_FEATURE`. La PR 0 fournit un `typeOf` minimal (littéraux,
noms) pour que le Membre 1 puisse déjà tester.

**PythonGenerator.java**

| Méthode | Propriétaire |
|---|---|
| `generateConstructor(agent)` (écrit `__init__`), `generateMethod`, `generateVarDecl`, `generateAssign`, `pyName(String)` | M1 |
| `expression(...)` (tous les opérateurs et appels), `generateExprStatement`, `generateReturn`, `generateMemberCall` | M2 |
| `generateIf`, `generateWhile`, `generateListLiteral`, `generateSetLiteral`, `generateNew` (pour `ArrayList`/`HashSet`), `generateListSetCall` (valeurs : `get`, `size`, `contains`), `generateListSetStatement` (`add`) | M3 |
| `generateMapLiteral`, `generateMapCall` (valeurs : `get`, `size`, `containsKey`), `generateMapStatement` (`put`), `generateNew` (pour `HashMap`/`TreeMap`, via un `switch` sur le nom) | M4 |

`generateMemberCall` (M2) lit le type du receveur dans `TypeInfo` et délègue à `generateListSetCall` (M3)
ou `generateMapCall` (M4) pour les appels qui ont une valeur. `generateExprStatement` (M2) fait de même pour
les instructions `add` et `put` : `generateListSetStatement` (M3) ou `generateMapStatement` (M4), qui écrivent
leur ligne eux-mêmes avec `line(...)`. **Les imports ne génèrent aucune ligne Python** : les collections sont des types
natifs de Python.

## 7. Les codes d'erreur (`Diagnostics.Code`) : Membre 1, dans la PR 0

On ajoute : `UNKNOWN_SYMBOL`, `TYPE_MISMATCH`, `IMMUTABLE_ASSIGNMENT`, `WRONG_ARGUMENT_COUNT`.
On réutilise `SYNTAX_ERROR`, `MISSING_BRACE`, `UNSUPPORTED_FEATURE`, `DUPLICATE_DECLARATION`.
**Toute erreur donne la ligne et la colonne.**

## 8. Le Python généré (décisions)

| Sujet | Décision |
|---|---|
| Classe | `class A(Agent):` avec `from sarl_runtime import Agent` en tête (**proposée** par la PR `fix-decisions-groupe` : à confirmer par le groupe avant la PR 0 ; c'est alors la seule ligne `import` du Python généré) |
| Champs | **dans `__init__`**, jamais au niveau de la classe : sinon une liste serait partagée entre tous les agents. `def __init__(self, runtime=None):` puis `super().__init__(runtime)` puis `self.x = ...`. Un agent sans champ n'a pas de `__init__`. `var x : int` sans valeur reçoit `0` ; `double` : `0.0` ; `boolean` : `False` ; les autres types : `None` |
| Noms | champ → `self.x` ; paramètre ou variable locale → `x` ; méthode de l'agent → `self.f(...)` ; un nom qui est un mot réservé Python (`from`, `in`, `is`, `pass`, `lambda`...) reçoit un `_` à la fin (`from_`) |
| Opérateurs | `&&` → `and`, `\|\|` → `or`, `!` → `not`, `true` → `True` |
| Priorités | un opérande est mis entre parenthèses si son opérateur est moins prioritaire, ou de même priorité et à droite ; **une comparaison dont l'opérande est lui-même une comparaison est toujours parenthésée** (en Python `a < b < c` est une chaîne) ; l'opérande de `not` ou de `-` est parenthésé s'il n'est pas un atome |
| Division | `int / int` → `int(a / b)` (SARL tronque vers zéro) ; `double` : `a / b` |
| Collections | `#[a, b]` → `[a, b]` ; `#{a, b}` → `{a, b}` ; **ensemble vide : `set()`** (jamais `{}`, qui est un dictionnaire) ; `#{k -> v}` → `{k: v}` ; `new ArrayList<T>()` → `[]` ; `new HashSet<T>()` → `set()` ; `new HashMap<K,V>()` et `new TreeMap<K,V>()` → `{}` |
| Opérations | liste : `add` → `x.append(e)`, `get(i)` → `x[i]`, `size()` → `len(x)`, `contains(e)` → `(e in x)` ; ensemble : `add` → `x.add(e)`, `contains`, `size` ; map : `put(k, v)` → `m[k] = v`, `get(k)` → `m.get(k)`, `size()` → `len(m)`, `containsKey(k)` → `(k in m)` |
| `add` et `put` | de type `void` : ils ne s'utilisent que comme instruction (en SARL `put` renvoie l'ancienne valeur, `add` renvoie un booléen : on refuse de les traduire comme valeur, `UNSUPPORTED_FEATURE`). **Ils sont générés comme des instructions** (`generateListSetStatement`, `generateMapStatement`), jamais par `expression(...)`, qui ne produit que des expressions Python |
| `TreeMap` | **comportement très restreint** : seules les 4 opérations qui ne dépendent pas de l'ordre (`put`, `get`, `size`, `containsKey`) sont acceptées. Comme `for` n'existe pas dans le sous-ensemble et que `info(...)` refuse les collections, l'ordre trié n'est jamais observable : traduire par un `dict` est donc fidèle. Toute autre opération donne `UNSUPPORTED_FEATURE` |
| `info(...)` | refuse une collection en argument (l'affichage diffère entre SARL et Python) |
| Concaténation | `String + String` seulement (`"n=" + 1` donne `TYPE_MISMATCH`) |

## 9. L'exemple complet que tout le groupe doit pouvoir transpiler à la fin

```sarl
import java.util.List

agent Counter {
    var count : int = 0
    val names = new ArrayList<String>()

    def increment(step : int) : int {
        var n = count + step
        count = n
        return n
    }

    on Initialize {
        info("start")
        var i = 0
        while (i < 3) {
            if (i == 1) {
                info("one")
            } else {
                info("other")
            }
            i = i + 1
        }
        names.add("a")
    }
}
```

```python
from sarl_runtime import Agent


class Counter(Agent):
    def __init__(self, runtime=None):
        super().__init__(runtime)
        self.count = 0
        self.names = []

    def increment(self, step):
        n = self.count + step
        self.count = n
        return n

    def on_initialize(self, occurrence):
        self.info("start")
        i = 0
        while i < 3:
            if i == 1:
                self.info("one")
            else:
                self.info("other")
            i = i + 1
        self.names.append("a")
```

## 10. Déroulement et validation

1. **PR 0, répartie par fichier** : personne ne la porte seul, et deux personnes ne modifient jamais le
   même fichier.

   | PR | Qui | Fichiers | Durée |
   |---|---|---|---|
   | 0a | Membre 1 | `SyntaxTree.java` (tous les nœuds, `imports`), `SymbolTable.java`, `TypeInfo.java`, `Types.java`, `Diagnostics.java` | 1 h 30 |
   | 0b | Membre 4 | `Tag.java`, `Lexer.java` (mots réservés, symboles, `getOperator`), `runtime/check_generated.py` | 1 h 30 à 2 h |
   | 0c | Membre 2 | `Parser.java` (aiguillages, ensembles de synchronisation, squelettes `notImplemented`) | 1 h 30 |
   | 0d | Membre 3 | `Semantic.java`, `PythonGenerator.java`, `Transpiler.java` (parcours, `TypeInfo`, squelettes) | 1 h 30 à 2 h |
   | 0e | Membre 1 | `GRAMMAIRE.md`, `TABLE_LL1.md`, `NIVEAU2_LIMITES.md` (après 0a à 0d) | 1 h |

   Les PR 0a à 0d s'**écrivent en parallèle** (chacun connaît les noms grâce à ce document) ; seule la
   **fusion** est ordonnée : 0a d'abord (les autres en ont besoin pour compiler), puis 0b, puis 0c et 0d.
   Chaque PR doit **compiler** et **ne casser aucun test** du niveau 1 ; les autres la relisent.
   Estimation réaliste : une demi-journée à quatre. Dès que 0a est fusionnée, chacun peut tester avec des
   **arbres construits à la main** (par exemple `new SyntaxTree.IfStatement(...)` passé au `Semantic` puis au
   `PythonGenerator`), sans attendre le Parser des autres.
2. **Avant la PR 0** : fusionner dans `main` les branches du niveau 1 (`membre3-initialize` et
   `fix-decisions-groupe`), pour partir d'un `main` à jour.
3. **Ensuite** : chacun ouvre sa branche depuis le `main` de la PR 0, remplit seulement ses méthodes,
   écrit ses tests, ouvre sa PR. Relecture croisée avant fusion. Pas de modification de
   `SyntaxTree.java`, `SymbolTable.java` ou `Lexer.java` sans prévenir.
4. **Ordre d'écriture conseillé** : tant que le Membre 2 n'a pas fini, les autres testent avec les
   expressions simples déjà présentes (nombres, chaînes, noms, `true`/`false` une fois la PR 0 fusionnée).
5. **Validation collective** : `mvn compile` et `mvn test` verts sur `main` ; chaque exemple SARL du
   sous-ensemble produit du Python qui passe `ast.parse` (script `runtime/check_generated.py`, voir
   fiche du Membre 1) ; chaque cas non supporté donne une erreur avec ligne et colonne ; limites
   documentées (`TreeMap`, imports, runtime).

## Sources (documentation officielle SARL)

- Variables : https://www.sarl.io/docs/official/lang/expr/VarDecls.html
- Littéraux (liste, ensemble, map) : https://www.sarl.io/docs/official/lang/expr/Literals.html
- `if` : https://www.sarl.io/docs/official/lang/expr/IfExpression.html
- boucles : https://www.sarl.io/docs/official/lang/expr/LoopExpression.html
- méthodes `def` : https://www.sarl.io/docs/official/lang/expr/FuncDecls.html
- opérateurs et priorités : https://www.sarl.io/docs/official/lang/expr/Operators.html
- exemples de collections et d'imports : https://www.sarl.io/docs/official/tutorials/basesyntax/IntroductionList.html
