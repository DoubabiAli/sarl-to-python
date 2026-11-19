# Comprendre le projet

Le projet lit un petit sous-ensemble SARL et le traduit en Python. Le Python produit
représente les déclarations reconnues ; il ne possède pas de moteur d'exécution.

## Le pipeline

```text
texte SARL
  -> Lexer : découpe le texte en tokens
  -> Parser : vérifie la syntaxe et construit l'arbre
  -> Semantic : vérifie les déclarations et les noms
  -> PythonGenerator : produit le texte Python
  -> Transpiler : écrit le fichier .py
```

## Les fichiers principaux

| Fichier | Rôle |
|---|---|
| `Tag.java` | Liste les types de tokens |
| `Token.java` | Associe un type de token à son texte et à sa position |
| `Lexer.java` | Lit les caractères et produit les tokens |
| `Diagnostics.java` | Conserve les erreurs avec leur position |
| `SyntaxTree.java` | Contient les nœuds de l'arbre syntaxique |
| `Parser.java` | Analyse les tokens avec une descente récursive |
| `SymbolTable.java` | Contient les symboles déclarés |
| `Semantic.java` | Vérifie les doubles déclarations et les règles sémantiques |
| `PythonGenerator.java` | Parcourt l'arbre et écrit le Python |
| `Transpiler.java` | Coordonne toutes les étapes et écrit le résultat |

Tous les fichiers Java se trouvent directement dans `src/main/java/` ou
`src/test/java/`. Ils n'utilisent aucun paquet Java afin de garder une
architecture simple.

## Exemple

Entrée SARL :

```sarl
agent A {}
agent B {}
```

Le lexer produit les tokens :

```text
AGENT, IDENTIFICATEUR(A), ACCOLADE_OUVRANTE,
ACCOLADE_FERMANTE, AGENT, IDENTIFICATEUR(B),
ACCOLADE_OUVRANTE, ACCOLADE_FERMANTE, FIN
```

Le parser construit deux nœuds `AgentNode`. Le Semantic vérifie que `A` et `B`
ne sont pas déjà déclarés plusieurs fois. Le générateur produit :

```python
# Genere par le transpileur SARL -> Python. Ne pas modifier a la main.
class A:
    pass


class B:
    pass
```

## Gestion des erreurs

La plupart des erreurs contiennent un code, une ligne et une colonne. Le parser
utilise une récupération en mode panique : après une erreur, il avance jusqu'à un
token de synchronisation afin de continuer l'analyse.

Exemples de diagnostics :

- identifiant manquant ;
- accolade manquante ;
- déclaration non supportée ;
- double déclaration d'un agent ;
- syntaxe ou fonctionnalité non implémentée.

## Fonctions à compléter

Les méthodes sont réparties entre les membres du groupe :

| Fonction | Parser | Générateur | Analyse sémantique | Test |
|---|---|---|---|---|
| Agents | `parseAgent` | `generateAgent` | doublons d'agents | `AgentTest` |
| Spawn | `parseUses`, `parseSpawn` | `generateSpawn` | type d'agent connu | `SpawnTest` |
| Initialize | `parseInitialize` | `generateInitialize` | handler unique | `InitializeTest` |
| Destroy | `parseDestroy`, `parseKillMe` | `generateDestroy`, `generateKillMe` | handler unique | `DestroyTest` |

Les méthodes de ces fonctionnalités peuvent contenir des messages `TODO`. Elles
doivent être implémentées avant que les tests associés ne soient activés.

## Compiler et lancer

```bash
mvn test
mvn -q compile exec:java -Dexec.mainClass=Transpiler -Dexec.args="examples/TwoAgents.sarl generated"
```

Le programme affiche le chemin du fichier Python généré ou les diagnostics en cas
d'erreur.
