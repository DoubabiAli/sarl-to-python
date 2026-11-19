# L'arbre syntaxique (`SyntaxTree.java`)

C'est le **contrat commun** du groupe : le Parser fabrique ces objets, le Semantic les vérifie, le
PythonGenerator les traduit. Les trois parties du programme ne se parlent qu'à travers cet arbre.
Si quelqu'un doit changer un nœud, il prévient les autres avant.

Notion classique : arbre syntaxique abstrait (AST).

## 1. Qu'est-ce qu'un `record` ?

Un `record` Java est une classe de données courte. La ligne

```java
public record AgentNode(String name, List<Node> members, int line, int column) implements Node { }
```

crée automatiquement : un constructeur `new AgentNode(name, members, line, column)`, des accesseurs
`agent.name()`, `agent.members()`, `agent.line()`, `agent.column()`, et `equals` / `toString`.
Les champs ne changent plus après la création.

## 2. Les types de base

| Type | Sens |
|---|---|
| `Node` | tout nœud de l'arbre : il a une `line()` et une `column()` (pour les messages d'erreur) |
| `Statement` | une instruction à l'intérieur d'un bloc |
| `Expression` | une valeur |

## 3. Les nœuds

| Nœud | Contenu | Écrit par |
|---|---|---|
| `ProgramNode(agents)` | la liste des agents du fichier | Membre 1 |
| `AgentNode(name, members)` | le nom et les membres **dans l'ordre de déclaration** | Membre 1 |
| `UsesNode(capabilities)` | la liste des noms après `uses` (`Lifecycle`...) | Membre 2 |
| `InitializeNode(body)` | la liste des instructions du handler | Membre 3 |
| `DestroyNode(body)` | la liste des instructions du handler | Membre 4 |
| `InfoStatement(message)` | l'expression affichée | commun |
| `SpawnStatement(agentType, arguments)` | le nom de l'agent et les arguments **dans l'ordre** | Membre 2 |
| `KillMeStatement()` | rien | Membre 4 |
| `StringLiteral(value)` | le texte sans guillemets | commun |
| `NumberLiteral(text)` | le nombre sous forme de texte (`"20"`) | commun |
| `VariableRef(name)` | un nom (`x`) | commun |

## 4. Exemple : l'arbre de `examples/Integration.sarl`

```text
ProgramNode
├── AgentNode "ParentAgent"
│   ├── UsesNode [Lifecycle]
│   ├── InitializeNode
│   │   ├── InfoStatement (StringLiteral "Parent started")
│   │   └── SpawnStatement ChildAgent [StringLiteral "Max", NumberLiteral "20"]
│   └── DestroyNode
│       └── InfoStatement (StringLiteral "Parent stopped")
└── AgentNode "ChildAgent"
    ├── InitializeNode
    │   └── InfoStatement (StringLiteral "Child started")
    └── DestroyNode
        └── InfoStatement (StringLiteral "Child stopped")
```

Chaque feuille et chaque nœud garde sa ligne et sa colonne.

## 5. Comment lire l'arbre dans le code (`instanceof`)

Les listes `members` et `body` contiennent des types différents. On les distingue avec
`instanceof` :

```java
for (SyntaxTree.Node member : agent.members()) {
    if (member instanceof SyntaxTree.InitializeNode init) {
        // ici "init" est un InitializeNode : init.body() est la liste des instructions
    }
}
```

`instanceof X nom` teste le type **et** donne directement une variable du bon type.

## 6. Modifier l'arbre

Ajouter un nœud = ajouter une ligne `record` dans `SyntaxTree.java`, puis le produire dans le Parser,
le vérifier si besoin dans le Semantic, et le traduire dans le PythonGenerator. Si un nœud existe
mais que le générateur ne le connaît pas, `generateMember` / `generateStatement` signale
`UNSUPPORTED_FEATURE` : on n'oublie jamais un nœud en silence.
