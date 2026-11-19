# L'analyse sémantique (`Semantic.java`, `SymbolTable.java`)

Notion classique : « program checking ».

## 1. À quoi ça sert

Après le Parser, la **syntaxe** est correcte : les mots sont dans le bon ordre. Mais le programme
peut avoir un **sens** faux. Exemples que le Parser ne voit pas :

```sarl
agent A {}
agent A {}                          // deux agents du même nom
agent B { on Initialize { spawn(typeof(Z)) } }   // Z n'existe pas
agent C { on Initialize {} on Initialize {} }    // deux handlers Initialize
```

Le Semantic regarde l'arbre et signale ces erreurs, avec la ligne et la colonne du nœud fautif.

Le `Transpiler` ne lance le Semantic que s'il n'y a eu **aucune erreur** avant (lexicale ou
syntaxique).

## 2. La table des symboles

`SymbolTable` est un dictionnaire `nom -> Symbol`, avec une chaîne de portées :

- `declare(nom, symbole)` : ajoute un nom dans cette portée ;
- `get(nom)` : cherche dans cette portée puis dans les portées englobantes ; `null` si absent.

Dans ce projet il n'y a qu'une portée globale : elle contient les **noms des agents**.

## 3. Les deux passes

```java
// Passe 1 : déclarer tous les agents (détecte les doublons)
for (AgentNode agent : program.agents()) { ... global.declare(...) ... }

// Passe 2 : vérifier le contenu de chaque agent
for (AgentNode agent : program.agents()) { checkAgent(agent, global); }
```

Pourquoi deux passes ? Pour qu'un agent puisse en créer un autre qui est déclaré **plus bas** dans
le fichier. Si on vérifiait en lisant une seule fois de haut en bas, `ChildAgent` serait inconnu au
moment du `spawn` dans `ParentAgent`.

## 4. Ce qui reste à écrire : `checkAgent`

La méthode `checkAgent(agent, global)` est vide, avec trois commentaires :

| Membre | Règle | Code d'erreur |
|---|---|---|
| 2 | dans chaque corps de handler, `spawn(typeof(X))` doit viser un agent déclaré : `global.get("X") != null` | `UNKNOWN_AGENT_TYPE` |
| 3 | au plus **un** `on Initialize` par agent | `DUPLICATE_HANDLER` |
| 4 | au plus **un** `on Destroy` par agent | `DUPLICATE_HANDLER` |

Forme générale pour compter (exemple pour `Initialize`) :

```java
int count = 0;
for (SyntaxTree.Node member : agent.members()) {
    if (member instanceof SyntaxTree.InitializeNode init) {
        count++;
        if (count > 1) {
            diag.report(Diagnostics.Code.DUPLICATE_HANDLER, init.line(), init.column(),
                    "l'agent " + agent.name() + " a plusieurs handlers on Initialize");
        }
    }
}
```

Pour `spawn`, on parcourt de la même façon les instructions de `init.body()` (et de
`destroy.body()`), et on regarde celles qui sont des `SyntaxTree.SpawnStatement`.

## 5. Règle de conduite

Chacun écrit **sa** vérification dans `checkAgent`, à l'endroit marqué de son numéro de membre.
Si trois personnes éditent la même méthode, Git signale des conflits : mettez chaque vérification
dans une méthode privée séparée (`checkInitialize`, `checkDestroy`, `checkSpawn`) et appelez-la
depuis `checkAgent`, sur une seule ligne chacun.
