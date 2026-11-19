# Table d'analyse LL(1) : FIRST, FOLLOW et predictive parsing

Méthode classique de l'analyse syntaxique prédictive : grammaire numérotée `⟨X⟩ ::= ...`, fonctions FIRST et FOLLOW, table d'analyse
`M[X, a]` avec des numéros de règles, et *nonrecursive predictive parsing* avec les colonnes
**stack / input / output**.

> Le `Parser.java` du projet est une **descente récursive** (une méthode par non-terminal), pas un analyseur à table. Mais les deux méthodes reposent sur la même idée : la table
> dit quelle règle choisir en regardant **un seul token** (le lookahead). Voir le paragraphe 06 pour
> le lien entre chaque ligne de la table et chaque méthode du Parser.

## 00 | La grammaire

Les non-terminaux sont écrits `⟨X⟩`. Les terminaux sont les tokens : `agent`, `uses`, `on`,
`Initialize`, `Destroy`, `info`, `spawn`, `typeof`, `killMe`, `id`, `chaine`, `nombre`, `{`, `}`,
`(`, `)`, `,`. Le symbole `$` est la fin de fichier (`eof` dans certains livres). `ϵ` est la chaîne vide.
Le symbole de départ est `⟨programme⟩`.

```text
(1)  ⟨programme⟩     ::= ⟨agent⟩ ⟨programme⟩
(2)  ⟨programme⟩     ::= ϵ
(3)  ⟨agent⟩         ::= agent id { ⟨membres⟩ }
(4)  ⟨membres⟩       ::= ⟨membre⟩ ⟨membres⟩
(5)  ⟨membres⟩       ::= ϵ
(6)  ⟨membre⟩        ::= uses id ⟨identifiants⟩
(7)  ⟨membre⟩        ::= on ⟨handler⟩
(8)  ⟨identifiants⟩  ::= , id ⟨identifiants⟩
(9)  ⟨identifiants⟩  ::= ϵ
(10)  ⟨handler⟩       ::= Initialize ⟨bloc⟩
(11)  ⟨handler⟩       ::= Destroy ⟨bloc⟩
(12)  ⟨bloc⟩          ::= { ⟨instructions⟩ }
(13)  ⟨instructions⟩  ::= ⟨instruction⟩ ⟨instructions⟩
(14)  ⟨instructions⟩  ::= ϵ
(15)  ⟨instruction⟩   ::= info ( ⟨expression⟩ )
(16)  ⟨instruction⟩   ::= spawn ( typeof ( id ) ⟨arguments⟩ )
(17)  ⟨instruction⟩   ::= killMe
(18)  ⟨arguments⟩     ::= , ⟨expression⟩ ⟨arguments⟩
(19)  ⟨arguments⟩     ::= ϵ
(20)  ⟨expression⟩    ::= chaine
(21)  ⟨expression⟩    ::= nombre
(22)  ⟨expression⟩    ::= id
```

Le `;` facultatif après `info(...)` n'est pas dans la grammaire : le Parser l'accepte avec
`match(Tag.SEMICOLON)`.

Cette semaine, seules les règles de `⟨programme⟩`, `⟨agent⟩`, `⟨membres⟩`, `⟨membre⟩` (avec `on`),
`⟨handler⟩`, `⟨bloc⟩`, `⟨instructions⟩`, `⟨instruction⟩` (avec `info`) et `⟨expression⟩` sont
implémentées. `uses`, `spawn` et `killMe` viendront plus tard.

## 01 | Les fonctions FIRST et FOLLOW

- `FIRST(α)` : l'ensemble des terminaux par lesquels peut **commencer** une chaîne dérivée de `α`.
  Si `α` peut dériver `ϵ`, alors `ϵ ∈ FIRST(α)`.
- `FOLLOW(A)` : l'ensemble des terminaux qui peuvent apparaître **juste après** `A` dans une
  dérivation. `$ ∈ FOLLOW(S)` pour le symbole de départ `S`.

| Non-terminal | FIRST | FOLLOW |
|---|---|---|
| `⟨programme⟩` | `agent`, `ϵ` | `$` |
| `⟨agent⟩` | `agent` | `agent`, `$` |
| `⟨membres⟩` | `on`, `uses`, `ϵ` | `}` |
| `⟨membre⟩` | `on`, `uses` | `on`, `uses`, `}` |
| `⟨identifiants⟩` | `,`, `ϵ` | `on`, `uses`, `}` |
| `⟨handler⟩` | `Destroy`, `Initialize` | `on`, `uses`, `}` |
| `⟨bloc⟩` | `{` | `on`, `uses`, `}` |
| `⟨instructions⟩` | `info`, `killMe`, `spawn`, `ϵ` | `}` |
| `⟨instruction⟩` | `info`, `killMe`, `spawn` | `info`, `killMe`, `spawn`, `}` |
| `⟨arguments⟩` | `,`, `ϵ` | `)` |
| `⟨expression⟩` | `chaine`, `id`, `nombre` | `)`, `,` |

Calculs importants :

- `FOLLOW(⟨agent⟩)` contient `FIRST(⟨programme⟩) − {ϵ} = {agent}` (règle 1 : `⟨agent⟩ ⟨programme⟩`)
  et `FOLLOW(⟨programme⟩) = {$}`, car `⟨programme⟩` peut être vide.
- `FOLLOW(⟨membre⟩)` = `FIRST(⟨membres⟩) − {ϵ}` ∪ `FOLLOW(⟨membres⟩)` = `{uses, on, }}`.
- `FOLLOW(⟨bloc⟩)` = `FOLLOW(⟨handler⟩)` = `FOLLOW(⟨membre⟩)`, car `⟨bloc⟩` est en fin de règle (10),
  (11) et `⟨handler⟩` est en fin de règle (7).
- `FOLLOW(⟨arguments⟩) = {)}` : `⟨arguments⟩` est juste avant la parenthèse fermante du `spawn`.

## 02 | La grammaire est-elle LL(1) ?

Pour deux règles `A ::= α | β`, il faut :

1. `FIRST(α) ∩ FIRST(β) = ∅` ;
2. si `ϵ ∈ FIRST(β)`, alors `FIRST(α) ∩ FOLLOW(A) = ∅`.

Vérification pour les non-terminaux à plusieurs règles :

| Non-terminal | Alternatives | Test |
|---|---|---|
| `⟨programme⟩` | `{agent}` contre `{$}` (règle ϵ, FOLLOW) | disjoints |
| `⟨membres⟩` | `{uses, on}` contre `{}}` | disjoints |
| `⟨membre⟩` | `{uses}` contre `{on}` | disjoints |
| `⟨identifiants⟩` | `{,}` contre `{uses, on, }}` | disjoints |
| `⟨handler⟩` | `{Initialize}` contre `{Destroy}` | disjoints |
| `⟨instructions⟩` | `{info, spawn, killMe}` contre `{}}` | disjoints |
| `⟨instruction⟩` | `{info}`, `{spawn}`, `{killMe}` | disjoints |
| `⟨arguments⟩` | `{,}` contre `{)}` | disjoints |
| `⟨expression⟩` | `{chaine}`, `{nombre}`, `{id}` | disjoints |

La grammaire est **LL(1)** : on choisit la règle en lisant un seul token.

La **factorisation à gauche** du `on` est nécessaire : sans la règle `⟨membre⟩ ::= on ⟨handler⟩`,
les règles `on Initialize ...` et `on Destroy ...` commenceraient toutes les deux par `on`, et il
faudrait lire deux tokens pour choisir (la condition 1 ne serait pas vraie).

## 03 | La table d'analyse M[X, a]

Construction (algorithme classique) : pour chaque règle numéro `n` : `⟨X⟩ ::= α`,
1. pour chaque terminal `a ∈ FIRST(α)`, mettre `(n)` dans `M[X, a]` ;
2. si `ϵ ∈ FIRST(α)`, mettre `(n)` dans `M[X, b]` pour chaque `b ∈ FOLLOW(X)`.

Une case vide est une erreur de syntaxe.

| | `agent` | `uses` | `on` | `Initialize` | `Destroy` | `info` | `spawn` | `killMe` | `id` | `chaine` | `nombre` | `{` | `}` | `(` | `)` | `,` | `$` |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `⟨programme⟩` | (1) |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  | (2) |
| `⟨agent⟩` | (3) |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| `⟨membres⟩` |  | (4) | (4) |  |  |  |  |  |  |  |  |  | (5) |  |  |  |  |
| `⟨membre⟩` |  | (6) | (7) |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| `⟨identifiants⟩` |  | (9) | (9) |  |  |  |  |  |  |  |  |  | (9) |  |  | (8) |  |
| `⟨handler⟩` |  |  |  | (10) | (11) |  |  |  |  |  |  |  |  |  |  |  |  |
| `⟨bloc⟩` |  |  |  |  |  |  |  |  |  |  |  | (12) |  |  |  |  |  |
| `⟨instructions⟩` |  |  |  |  |  | (13) | (13) | (13) |  |  |  |  | (14) |  |  |  |  |
| `⟨instruction⟩` |  |  |  |  |  | (15) | (16) | (17) |  |  |  |  |  |  |  |  |  |
| `⟨arguments⟩` |  |  |  |  |  |  |  |  |  |  |  |  |  |  | (19) | (18) |  |
| `⟨expression⟩` |  |  |  |  |  |  |  |  | (22) | (20) | (21) |  |  |  |  |  |  |

Aucune case ne contient deux règles, ce qui confirme que la grammaire est LL(1).

## 04 | Nonrecursive predictive parsing

Algorithme classique. La pile `S` contient les symboles à reconnaître, le sommet est à
gauche dans les tableaux ci-dessous.

```text
begin
  a <- nextInputSymbol;
  push(S, $);  push(S, s0);               // s0 = ⟨programme⟩
  while topOf(S) ≠ $ do
    X <- topOf(S);
    if X = a then                          // terminal égal au token courant
      pop(S);  a <- nextInputSymbol;
    else if X is a terminal then
      Report an error;
    else if M[X, a] = (⟨X⟩ -> Y1 ... Yn) then
      print(⟨X⟩ -> Y1 ... Yn);             // la colonne « output »
      pop(S);
      for i <- n to 1 do push(S, Yi);
    else
      Report an error;
  end
end
```

## 05 | Exemple d'exécution

Entrée : `agent A { on Initialize { info("hi") } }`, soit les tokens
`agent id { on Initialize { info ( chaine ) } } $`.
La colonne **output** donne la règle utilisée ; `match` signifie qu'on consomme un terminal.

| # | Stack | Input | Output |
|---|---|---|---|
| 1 | `⟨programme⟩ $` | `agent id { on Initialize { info ( chaine ) } } $` | (1) `⟨programme⟩ ::= ⟨agent⟩ ⟨programme⟩` |
| 2 | `⟨agent⟩ ⟨programme⟩ $` | `agent id { on Initialize { info ( chaine ) } } $` | (3) `⟨agent⟩ ::= agent id { ⟨membres⟩ }` |
| 3 | `agent id { ⟨membres⟩ } ⟨programme⟩ $` | `agent id { on Initialize { info ( chaine ) } } $` | match `agent` |
| 4 | `id { ⟨membres⟩ } ⟨programme⟩ $` | `id { on Initialize { info ( chaine ) } } $` | match `id` |
| 5 | `{ ⟨membres⟩ } ⟨programme⟩ $` | `{ on Initialize { info ( chaine ) } } $` | match `{` |
| 6 | `⟨membres⟩ } ⟨programme⟩ $` | `on Initialize { info ( chaine ) } } $` | (4) `⟨membres⟩ ::= ⟨membre⟩ ⟨membres⟩` |
| 7 | `⟨membre⟩ ⟨membres⟩ } ⟨programme⟩ $` | `on Initialize { info ( chaine ) } } $` | (7) `⟨membre⟩ ::= on ⟨handler⟩` |
| 8 | `on ⟨handler⟩ ⟨membres⟩ } ⟨programme⟩ $` | `on Initialize { info ( chaine ) } } $` | match `on` |
| 9 | `⟨handler⟩ ⟨membres⟩ } ⟨programme⟩ $` | `Initialize { info ( chaine ) } } $` | (10) `⟨handler⟩ ::= Initialize ⟨bloc⟩` |
| 10 | `Initialize ⟨bloc⟩ ⟨membres⟩ } ⟨programme⟩ $` | `Initialize { info ( chaine ) } } $` | match `Initialize` |
| 11 | `⟨bloc⟩ ⟨membres⟩ } ⟨programme⟩ $` | `{ info ( chaine ) } } $` | (12) `⟨bloc⟩ ::= { ⟨instructions⟩ }` |
| 12 | `{ ⟨instructions⟩ } ⟨membres⟩ } ⟨programme⟩ $` | `{ info ( chaine ) } } $` | match `{` |
| 13 | `⟨instructions⟩ } ⟨membres⟩ } ⟨programme⟩ $` | `info ( chaine ) } } $` | (13) `⟨instructions⟩ ::= ⟨instruction⟩ ⟨instructions⟩` |
| 14 | `⟨instruction⟩ ⟨instructions⟩ } ⟨membres⟩ } ⟨programme⟩ $` | `info ( chaine ) } } $` | (15) `⟨instruction⟩ ::= info ( ⟨expression⟩ )` |
| 15 | `info ( ⟨expression⟩ ) ⟨instructions⟩ } ⟨membres⟩ } ⟨programme⟩ $` | `info ( chaine ) } } $` | match `info` |
| 16 | `( ⟨expression⟩ ) ⟨instructions⟩ } ⟨membres⟩ } ⟨programme⟩ $` | `( chaine ) } } $` | match `(` |
| 17 | `⟨expression⟩ ) ⟨instructions⟩ } ⟨membres⟩ } ⟨programme⟩ $` | `chaine ) } } $` | (20) `⟨expression⟩ ::= chaine` |
| 18 | `chaine ) ⟨instructions⟩ } ⟨membres⟩ } ⟨programme⟩ $` | `chaine ) } } $` | match `chaine` |
| 19 | `) ⟨instructions⟩ } ⟨membres⟩ } ⟨programme⟩ $` | `) } } $` | match `)` |
| 20 | `⟨instructions⟩ } ⟨membres⟩ } ⟨programme⟩ $` | `} } $` | (14) `⟨instructions⟩ ::= ϵ` |
| 21 | `} ⟨membres⟩ } ⟨programme⟩ $` | `} } $` | match `}` |
| 22 | `⟨membres⟩ } ⟨programme⟩ $` | `} $` | (5) `⟨membres⟩ ::= ϵ` |
| 23 | `} ⟨programme⟩ $` | `} $` | match `}` |
| 24 | `⟨programme⟩ $` | `$` | (2) `⟨programme⟩ ::= ϵ` |
| 25 | `$` | `$` | **accept** |

Les étapes où la règle est `ϵ` (règles (14), (5) et (2)) sont choisies parce que le token courant
est dans le `FOLLOW` du non-terminal : par exemple `}` est dans `FOLLOW(⟨instructions⟩)`.

## 06 | Lien avec `Parser.java`

| Non-terminal | Méthode du Parser | Comment la table s'y retrouve |
|---|---|---|
| `⟨programme⟩` | `parse()` | boucle `while (!check(EOF))` : `agent` = règle (1), `$` = règle (2) |
| `⟨agent⟩` | `parseAgent()` | `expect(AGENT)`, `expect(ID)`, `expect(LBRACE)`... |
| `⟨membres⟩` / `⟨membre⟩` | boucle de `parseAgent()` + `parseMember()` | `uses` ou `on` : on lit un membre ; `}` : règle ϵ, la boucle s'arrête |
| `⟨identifiants⟩` | `parseUses()` (plus tard) | `,` : un identifiant de plus ; sinon ϵ |
| `⟨handler⟩` | `parseMember()`, puis `parseInitialize()` / `parseDestroy()` | `Initialize` ou `Destroy` |
| `⟨bloc⟩` / `⟨instructions⟩` | `parseBlock()` | boucle jusqu'à `}` |
| `⟨instruction⟩` | `parseStatement()` | `info`, `spawn` ou `killMe` |
| `⟨arguments⟩` | `parseSpawn()` (plus tard) | `,` : un argument de plus ; `)` : ϵ |
| `⟨expression⟩` | `parseExpression()` | `chaine`, `nombre` ou `id` |

Quand une case de la table est vide, le Parser est dans la situation « token inattendu » : il signale
l'erreur avec `error(...)`, puis passe en **mode panique** : il jette des tokens jusqu'à un token de
l'ensemble `FIRST ∪ FOLLOW` de la règle (voir `PARSER.md`).

