# Le Lexer : analyse lexicale

Fichier de code : `src/main/java/Lexer.java`.
Méthode classique de l'analyse lexicale :
expression régulière -> NFA -> DFA, tables de transition, simulation d'un NFA,
et diagrammes de transition codés à la main avec `lexemeBegin` et `forward`.

Les mots techniques restent en anglais comme dans les ouvrages de compilation (NFA, DFA, `move`, ε-closure, token...).

## 00 | Description du langage

Le langage est un sous-ensemble de SARL. Le lexer lit le texte et retourne des tokens, comme dans le
les exemples classiques de compilation (`<NUM,5> <LET> <ID,S> <=>`). Notation : `<nom>` pour un token sans attribut, `<nom,attribut>`
sinon.

Exemple. Le programme SARL :

```sarl
agent StudentAgent {
    on Initialize {
        info("Agent started")
    }
}
```

Le lexer doit répondre les tokens suivants, dans cet ordre :

```text
<agent> <ID,StudentAgent> <{>
<on> <Initialize> <{>
<info> <(> <STRING,Agent started> <)>
<}>
<}>  <EOF>
```

Un token (`Token.java`) = un nom (`Tag`) + un attribut (`lexeme`) + la ligne et la colonne (début à 1).

### 00.1 Les tokens

Notations : `L` = une lettre (`a-z`, `A-Z`, `_`, `$`, lettres Unicode), `D` = un chiffre (`0-9`).

| Token (`Tag`) | Expression régulière | Exemples |
|---|---|---|
| `ID` | `L (L \| D)*` | `A`, `ChildAgent`, `x1` |
| mots réservés : `AGENT`, `USES`, `ON`, `INITIALIZE`, `DESTROY`, `SPAWN`, `TYPEOF`, `KILLME`, `INFO` | comme `ID`, puis recherche dans la table `KEYWORDS` | `agent`, `on`, `Initialize` |
| `NUMBER` | `D+ (. D+)?` | `20`, `3.14` |
| `STRING` | `" (c \| \\ c)* "` ou la même chose avec `'` (`c` = tout caractère sauf le guillemet, `\` et le retour à la ligne) | `"Max"`, `'x'` |
| `LBRACE` `RBRACE` | `{` `}` | |
| `LPAREN` `RPAREN` | `(` `)` | |
| `LBRACKET` `RBRACKET` | `[` `]` | |
| `COMMA` `SEMICOLON` | `,` `;` | |
| `EOF` | fin du texte | |

Ignorés : espaces, tabulations, retours à la ligne, commentaires `// ...` et `/* ... */`.

### 00.2 Mots réservés : une table, pas un automate

On ne fait pas un automate par mot-clé. Selon la méthode classique, on installe les
mots réservés dans une table **au départ**, on lit un identifiant, puis on regarde s'il est dans la
table :

```java
return new Token(KEYWORDS.getOrDefault(lexeme, Tag.ID), lexeme, line, column());
```

Attention : `Map.of` accepte au maximum 10 paires, il y en a 9 aujourd'hui. Pour en ajouter plus,
utiliser `Map.ofEntries`.

## 01 | Exercice : l'identifiant, de la regex au DFA

Expression régulière : `L (L | D)*`

### 01.1 Le NFA (construction de Thompson)

Définition formelle, comme dans la littérature sur les automates :

```text
S = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9}      Σ = {L, D}      s0 = 0      F = {9}
```

Table de la fonction `move` (les transitions `ε` ne consomment aucun caractère) :

| S | Σ ∪ {ε} | S' |
|---|---|---|
| 0 | L | 1 |
| 1 | ε | 2 |
| 2 | ε | 3 or 9 |
| 3 | ε | 4 or 5 |
| 4 | L | 6 |
| 5 | D | 7 |
| 6 | ε | 8 |
| 7 | ε | 8 |
| 8 | ε | 3 or 9 |

Lecture : `0 -L-> 1` lit la première lettre ; `1 -ε-> 2` ; de `2` on peut sortir directement
(`2 -ε-> 9`, zéro répétition) ou entrer dans la boucle (`2 -ε-> 3`) ; de `3` on choisit une lettre
(`4`) ou un chiffre (`5`) ; `6` et `7` reviennent en `8`, qui boucle (`8 -ε-> 3`) ou sort (`8 -ε-> 9`).

### 01.2 Simulation du NFA sur `x1`

Algorithme classique de simulation d'un NFA : `S <- ε-closure(s0)`, puis pour chaque caractère
`S <- ε-closure(move(S, c))`, et on accepte si `S ∩ F ≠ ∅`.

| Étape | Caractère `c` | `move(S, c)` | `ε-closure(...)` = nouveau `S` |
|---|---|---|---|
| début | | | `S = ε-closure({0}) = {0}` |
| 1 | `x` (une lettre `L`) | `move({0}, L) = {1}` | `{1, 2, 3, 4, 5, 9}` |
| 2 | `1` (un chiffre `D`) | `move({1,2,3,4,5,9}, D) = {7}` | `{3, 4, 5, 7, 8, 9}` |

Fin de l'entrée : `S ∩ F = {9} ≠ ∅`, donc le NFA **accepte** `x1`.

### 01.3 NFA -> DFA (construction des sous-ensembles)

On applique l'algorithme classique de la construction des sous-ensembles. Un état du DFA est un ensemble d'états du NFA. L'état initial est
`ε-closure({0})`. Un état est final s'il contient un état final du NFA (ici `9`).

| DFA State | NFA States | Init | Final | L | D |
|---|---|---|---|---|---|
| A | {0} | X | | B | - |
| B | {1,2,3,4,5,9} | | X | C | D |
| C | {3,4,5,6,8,9} | | X | C | D |
| D | {3,4,5,7,8,9} | | X | C | D |

Détail de deux cases : `move(B, L) = {6}` et `ε-closure({6}) = {3,4,5,6,8,9}` = C ;
`move(B, D) = {7}` et `ε-closure({7}) = {3,4,5,7,8,9}` = D.

**Minimisation.** B, C et D sont tous finaux et ont les mêmes transitions : sur `L` ou `D` ils vont
vers C ou D, qui sont eux-mêmes équivalents. On les fusionne en un seul état. Le DFA minimal a deux
états :

| DFA State | Init | Final | L | D |
|---|---|---|---|---|
| A | X | | B | - |
| B | | X | B | B |

### 01.4 Le NFA ou le DFA est-il déterministe ?

Le NFA de 01.1 est **non déterministe** : il a des transitions `ε` (depuis `2`, `3` et `8`, plusieurs
transitions sortent du même état sur la même « entrée » `ε`). Le DFA de 01.3 est **déterministe** :
pour chaque état et chaque symbole il y a au plus une transition, et il n'y a pas de transition `ε`.
C'est ce DFA minimal qu'on code à la main dans `getIdentifier()`.

## 02 | Les autres tokens

Pour ces tokens, l'expression régulière donne directement un automate déterministe (aucune
transition `ε`, aucun choix possible), donc NFA = DFA. On écrit leur **transition table** : les
colonnes sont l'alphabet et les lignes sont les états. Un `-` veut dire « pas de transition ».

### 02.1 Nombre : `D+ (. D+)?`

| DFA State | Init | Final | D | `.` | autre |
|---|---|---|---|---|---|
| 0 | X | | 1 | - | pas un nombre |
| 1 (`12`) | | X | 1 | 2 | on accepte, `forward` recule de 1 |
| 2 (`12.`) | | | 3 | - | on accepte **sans le point**, `forward` recule de 2 |
| 3 (`12.5`) | | X | 3 | - | on accepte, `forward` recule de 1 |

Le cas `12.` suivi d'autre chose est le seul où on recule de deux caractères (`forward -= 2` dans le
code), pour que le point ne fasse pas partie du nombre.

### 02.2 Chaîne : `"..."` ou `'...'`

Le premier guillemet lu devient le délimiteur `q`.

| DFA State | Init | Final | `q` | `\` | retour à la ligne ou EOF | autre |
|---|---|---|---|---|---|---|
| 0 | X | | 1 | - | - | pas une chaîne |
| 1 (dans la chaîne) | | | 3 | 2 | **erreur** : chaîne non terminée | 1 |
| 2 (après `\`) | | | 1 | 1 | **erreur** | 1 |
| 3 | | X | | | | |

Dans l'état 2 le caractère suivant est protégé : `\n`, `\t`, `\r` donnent un retour à la ligne, une
tabulation, un retour chariot ; pour les autres (`\"`, `\\`...) on garde le caractère. L'attribut du
token est le contenu **sans les guillemets**.

### 02.3 Ponctuation

Deux états : on lit un caractère parmi `{ } ( ) [ ] , ;` et on accepte (`getSymbol()` : un `switch`).

## 03 | Du DFA au code Java

| Élément de la théorie | Dans `Lexer.java` |
|---|---|
| un transition diagram par token | `getIdentifier()`, `getNumber()`, `getString()`, `getSymbol()` |
| un état du diagramme | un `case` du `switch (state)` |
| la transition table | les `if` à l'intérieur de chaque `case` |
| début du lexème | `lexemeBegin` |
| pointeur de lecture | `forward` (avance avec `nextChar()`) |
| reculer d'un caractère | `retract()` |
| essayer les diagrammes l'un après l'autre | dans `tokenize()` : `getIdentifier()`, sinon `getNumber()`, sinon `getString()`, sinon `getSymbol()` |
| un diagramme échoue | `forward = lexemeBegin; return null;` (on remet le pointeur et on essaie le suivant) |
| table des mots réservés | `KEYWORDS` |

**Plus long préfixe** : chaque automate continue tant qu'il peut lire. Donc `agentA` est un seul
identifiant, et non `agent` suivi de `A`.

Pour `getIdentifier()`, avec le DFA minimal de 01.3 : état 0 = A, état 1 = B (qui boucle sur `L` ou
`D`), état 2 = « on a lu un caractère de trop : `retract()` et on retourne le token ».

## 04 | Exemple : `agent A {}`

| Étape | Diagramme essayé | Caractères lus | Résultat |
|---|---|---|---|
| 1 | `getIdentifier` : état 0, 1, 1, 1, 1, 1, 2 | `a g e n t` puis l'espace | `retract`, `agent` est dans `KEYWORDS` : `<agent>` |
| 2 | espace ignoré | | |
| 3 | `getIdentifier` : état 0, 1, 2 | `A` puis l'espace | `<ID,A>` (colonne 7) |
| 4 | `getIdentifier`, `getNumber`, `getString` échouent ; `getSymbol` | `{` | `<{>` |
| 5 | `getSymbol` | `}` | `<}>` |
| 6 | fin du texte | | `<EOF>` |

## 05 | Les erreurs lexicales : mode panique

Le Lexer ne s'arrête pas à la première erreur :

- caractère inconnu (par exemple `#`) : `UNEXPECTED_CHARACTER` avec ligne et colonne, on **supprime** le
  caractère et on continue ;
- chaîne non terminée : `SYNTAX_ERROR`, on saute jusqu'à la fin de la ligne ;
- commentaire `/* ...` jamais fermé : `SYNTAX_ERROR`.

## 06 | Ajouter un token

1. Ajouter le nom dans `Tag.java`.
2. Mot réservé : une paire de plus dans `KEYWORDS`. Symbole : un `case` de plus dans `getSymbol()`.
   Nouvelle forme (opérateur de deux caractères, par exemple) : écrire d'abord la regex, le DFA et sa
   transition table (comme en 01 et 02), puis un nouveau diagramme `getXxx()` appelé dans `tokenize()`.
3. Ajouter un test dans `LexerTest`.
4. Mettre à jour cette page et `GRAMMAIRE.md`.
