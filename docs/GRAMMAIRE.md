# Grammaire Level 1

Cette grammaire décrit le sous-ensemble SARL accepté par le projet. La notation
utilise `ε` pour une règle vide.

## Périmètre de cette semaine

Seules les parties suivantes sont à implémenter maintenant : `agent`, `on Initialize { ... }`,
`on Destroy { ... }` et `info(...)` dans les corps. Les règles `uses`, `spawn` et `killMe` sont déjà
dans la grammaire (pour garder la grammaire stable) mais leur implémentation est **plus tard**.

## Règles

```text
<programme>  -> <agent> <programme> | ε
<agent>      -> agent identifiant { <membres> }
<membres>    -> <membre> <membres> | ε
<membre>     -> uses identifiant <identifiants>
              | on <handler>
<identifiants> -> , identifiant <identifiants> | ε
<handler>    -> Initialize <bloc>
              | Destroy <bloc>
<bloc>       -> { <instructions> }
<instructions> -> <instruction> <instructions> | ε
<instruction> -> info ( <expression> )
              | spawn ( typeof ( identifiant ) <arguments> )
              | killMe
<arguments>  -> , <expression> <arguments> | ε
<expression> -> chaine | nombre | identifiant
```

## Factorisation

Les deux alternatives `on Initialize ...` et `on Destroy ...` commencent toutes
les deux par `on`. Elles sont factorisées dans `<membre> -> on <handler>`.
`Parser.parseMember()` lit `on`, puis `parseHandler()` choisit entre
`Initialize` et `Destroy`.

Cette factorisation évite à l'analyseur de devoir deviner une règle avec plusieurs
tokens possibles.

## FIRST et FOLLOW

`$` représente la fin du fichier.

| Non-terminal | FIRST | FOLLOW |
|---|---|---|
| `<programme>` | `agent`, `ε` | `$` |
| `<agent>` | `agent` | `agent`, `$` |
| `<membres>` | `uses`, `on`, `ε` | `}` |
| `<membre>` | `uses`, `on` | `uses`, `on`, `}` |
| `<identifiants>` | `,`, `ε` | `uses`, `on`, `}` |
| `<handler>` | `Initialize`, `Destroy` | `uses`, `on`, `}` |
| `<bloc>` | `{` | `uses`, `on`, `}` |
| `<instructions>` | `info`, `spawn`, `killMe`, `ε` | `}` |
| `<instruction>` | `info`, `spawn`, `killMe` | `info`, `spawn`, `killMe`, `}` |
| `<arguments>` | `,`, `ε` | `)` |
| `<expression>` | chaîne, nombre, identifiant | `,`, `)` |

## Grammaire LL(1)

Les alternatives commencent par des tokens différents. Pour les règles qui peuvent
être vides, FIRST ne chevauche pas FOLLOW :

- `<programme>` : `{agent}` contre `{$}` ;
- `<membres>` : `{uses, on}` contre `{}}` ;
- `<identifiants>` : `{,}` contre `{uses, on, }}` ;
- `<instructions>` : `{info, spawn, killMe}` contre `{}}` ;
- `<arguments>` : `{,}` contre `{)}`.

Le parser peut donc choisir une règle en regardant un seul token.

## Mode panique

Après une erreur, le parser saute les tokens jusqu'à un ensemble de synchronisation :

| Zone | Tokens de synchronisation |
|---|---|
| Après une erreur de fichier ou d'agent | `agent`, fin de fichier |
| Après une erreur de membre | `uses`, `on`, `}` |
| Après une erreur d'instruction | `info`, `spawn`, `killMe`, `}` |

## Tokens réservés

Dans ce projet, les mots suivants sont réservés par le lexer :

- `agent`
- `uses`
- `on`
- `Initialize`
- `Destroy`
- `info`
- `spawn`
- `typeof`
- `killMe`

## Limitations Level 1

- Un point-virgule facultatif est accepté après `info(...)`.
- Les handlers gardés avec `[...]` ne sont pas supportés.
- Plusieurs handlers du même type ne sont pas supportés.
- Les expressions arithmétiques ne sont pas supportées.
- Les variables locales et les méthodes ne sont pas supportées.
- Les fonctionnalités membres 2, 3 et 4 restent à implémenter.
