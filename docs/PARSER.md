# Le Parser (analyse syntaxique)

Fichier de code : `src/main/java/Parser.java`. Méthodes classiques : descente récursive
et récupération d'erreurs en mode panique.

## 1. À quoi sert le Parser

Le Lexer a donné une liste de tokens. Le Parser :
1. vérifie que les tokens respectent la grammaire (`GRAMMAIRE.md`) ;
2. **construit l'arbre syntaxique** (`SyntaxTree.java`, voir `ARBRE.md`) ;
3. signale les erreurs avec leur position.

Il ne génère **jamais** de Python. Il ne regarde que des tokens.

## 2. Descente récursive : une méthode par règle

Pour chaque non-terminal de la grammaire il y a une méthode. La méthode lit ses tokens, et
**retourne le nœud de l'arbre** correspondant. Quand une règle en contient une autre, la méthode
appelle l'autre méthode.

```text
parse()                     <programme>
  parseAgent()              <agent>
    parseMember()           <membre>
      parseUses()           uses ...                (Membre 2)
      parseInitialize()     on Initialize { ... }   (Membre 3)
      parseDestroy()        on Destroy { ... }      (Membre 4)
        parseBlock()        { instructions }
          parseStatement()  <instruction>
            parseInfo()     info(...)
            parseSpawn()    spawn(...)              (Membre 2)
            parseKillMe()   killMe                  (Membre 4)
              parseExpression()   chaine | nombre | id
```

## 3. Les outils de lecture (déjà écrits)

| Méthode | Ce qu'elle fait |
|---|---|
| `peek()` | regarde le token courant **sans avancer** |
| `advance()` | retourne le token courant et avance d'un token (jamais au-delà de `EOF`) |
| `check(tag)` | `true` si le token courant a ce type |
| `match(tag)` | si le token courant a ce type : avance et retourne `true`, sinon `false` |
| `expect(tag, code, message)` | comme `match`, mais si ce n'est pas le bon token : **signale une erreur et sort de la méthode** |
| `error(code, message)` | écrit l'erreur dans `Diagnostics` (avec ligne et colonne du token courant), retourne une exception à lancer : `throw error(...)` |

Quand on **doit** avoir un certain token, on utilise `expect`. Quand il est **facultatif**, on
utilise `match` (exemple : le `;` après `info(...)`).

## 4. Un exemple complet : `parseInfo()`

```java
private SyntaxTree.Statement parseInfo() {
    Token keyword = advance();                                              // info
    expect(Tag.LPAREN, Diagnostics.Code.SYNTAX_ERROR, "'(' attendu après 'info'");
    SyntaxTree.Expression message = parseExpression();                      // la valeur
    expect(Tag.RPAREN, Diagnostics.Code.SYNTAX_ERROR, "')' attendu");
    match(Tag.SEMICOLON);                                                   // ';' facultatif
    return new SyntaxTree.InfoStatement(message, keyword.line(), keyword.column());
}
```

C'est la forme de **toutes** les méthodes du Parser :
1. garder le token de départ (pour la position dans l'arbre) ;
2. lire le reste avec `expect` / `match` / les autres méthodes `parseXxx` ;
3. retourner le nœud de l'arbre.

## 5. Les erreurs : mode panique

Quand `expect` ou `error` échoue, la méthode **sort immédiatement** (une exception interne
`SyntaxError`, sans message : l'erreur est **déjà** dans `Diagnostics`). La méthode appelante
l'attrape, puis appelle `synchronize(...)` : elle **jette les tokens** jusqu'à un token « sûr »
(un token qui peut commencer l'élément suivant, ou fermer l'élément courant). L'analyse continue
ensuite, ce qui permet de trouver **plusieurs erreurs** en un seul passage.

| Zone | Tokens de synchronisation | Où dans le code |
|---|---|---|
| fichier | `agent` (et fin de fichier) | `parse()` |
| membres d'un agent | `uses`, `on`, `}` | `parseAgent()` |
| instructions d'un bloc | `info`, `spawn`, `killMe`, `}` | `parseBlock()` |

Ces ensembles sont `FIRST ∪ FOLLOW` de la règle (voir `TABLE_LL1.md`). `synchronize` saute un bloc
`{ ... }` d'un seul coup, pour ne pas s'arrêter sur une accolade fermante qui ne nous concerne pas.

Le motif `int start = pos; ... if (pos == start) advance();` dans les boucles empêche une **boucle
infinie** : si une erreur n'a consommé aucun token, on en saute un de force.

## 6. Ce que les Membres 2, 3 et 4 doivent écrire

Chaque méthode a déjà une signature et un commentaire. Il faut remplacer `throw notImplemented(...)`
par le vrai code. Le détail est dans `GUIDE_MEMBRES.md`.

| Méthode | Token courant en entrant | Doit retourner |
|---|---|---|
| `parseUses()` | `uses` | un `UsesNode` |
| `parseSpawn()` | `spawn` | un `SpawnStatement` |
| `parseInitialize(on)` | `Initialize` (le `on` est déjà consommé et donné en paramètre) | un `InitializeNode` |
| `parseDestroy(on)` | `Destroy` (idem) | un `DestroyNode` |
| `parseKillMe()` | `killMe` | un `KillMeStatement` |

## 7. Règles de conduite

- Une méthode du Parser ne touche **pas** à Python et ne regarde pas l'arbre des autres : elle lit
  des tokens et retourne un nœud.
- Ne modifiez que vos propres méthodes (`MEMBRE n`), pour éviter les conflits Git.
- Si vous ajoutez une règle, mettez à jour `GRAMMAIRE.md` et `TABLE_LL1.md`.
