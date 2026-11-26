# Niveaux 2 + 3, Membre 4 : `HashMap`, `TreeMap`, imports

**Ta mission** : faire reconnaître, vérifier et traduire les maps (`HashMap`, `TreeMap`, littéraux
`#{k -> v}`), leurs opérations, et les imports autorisés, **sans jamais changer en silence ce que le
programme SARL veut dire**.

```sarl
import java.util.Map

agent A {
    on Initialize {
        var ages = new HashMap<String, Integer>()
        ages.put("Max", 20)
        var n = ages.size()
        var m = #{"a" -> 1, "b" -> 2}
    }
}
```

doit devenir

```python
class A(Agent):
    def on_initialize(self, occurrence):
        ages = {}
        ages["Max"] = 20
        n = len(ages)
        m = {"a": 1, "b": 2}
```

(L'`import` SARL ne produit aucune ligne Python : les dictionnaires sont natifs. La seule ligne `import` du fichier généré est `from sarl_runtime import Agent`.)

Temps estimé : 4 à 5 h, lecture comprise.

**Lis d'abord `docs/membres/NIVEAU2_CONTRAT.md`.** Tes nœuds (`MapLiteral`, `MapEntry`, `ImportDecl`) et le
`NewObject` partagé existent déjà (PR 0).

---

## Étape 0 : préparer ton poste (15 min)

1. La PR 0 doit être fusionnée (0a à 0d, contrat §10). Ta part est la **PR 0b** : `Tag.java` et `Lexer.java` (mots réservés, symboles, un `getOperator()` en diagramme de transition pour `==`, `!=`, `<=`, `>=`, `&&`, `||`, `->`) et le script `runtime/check_generated.py`, environ 1 h 30 à 2 h. `git checkout main`, `git pull`.
2. `mvn test` : tout est vert (le terminal doit utiliser le JDK 25).
3. `git checkout -b membre4-maps-imports`.

## Étape 1 : comprendre le projet (1 h)

| # | Fichier | À comprendre |
|---|---|---|
| 1 | `docs/NIVEAU2_CONTRAT.md`, §2, 4, 6, 8 | la syntaxe des maps, des imports, et la décision `TreeMap` |
| 2 | `Parser.java`, `parse()` | c'est là qu'on lit le début d'un fichier : tes imports viennent avant `agent` |
| 3 | `Semantic.java` : `typeOf` du Membre 2 | tu y branches `typeOfMapCall`, `typeOfMapLiteral` et la partie `HashMap`/`TreeMap` de `typeOfNew` |
| 4 | `docs/SEMANTIC.md` | comment on signale une erreur à une position |

Pour vérifier que tu as compris, réponds à voix haute :
- Pourquoi un `dict` Python n'est-il pas une `TreeMap` ?
- Pourquoi `map.put(k, v)` n'est-il pas simplement `m[k] = v` dans tous les contextes ?
- Pourquoi un import Java ne génère-t-il pas d'import Python ?

## Étape 2 : comprendre ta tâche (30 min)

| Ordre | Fichier | Méthodes |
|---|---|---|
| 1 | `Parser.java` | `parseMapRest`, `parseImport` |
| 2 | `Semantic.java` | `typeOfMapCall`, `typeOfMapLiteral`, `typeOfNew` (cas `HashMap`/`TreeMap`), `checkImport` |
| 3 | `PythonGenerator.java` | `generateMapLiteral`, `generateMapCall`, `generateMapStatement`, `generateNew` (cas `HashMap`/`TreeMap`) |
| 4 | `MapImportTest.java` | les tests de l'étape 6 |

Hors de ta tâche : `List` et `Set` (Membre 3), `new` côté Parser (Membre 3 : il l'écrit pour toutes les
collections), les types génériques et `Types` (Membre 1), les expressions (Membre 2).

## Étape 3 : écrire le Parser (1 h)

Le Membre 3 lit `#{`, puis la première expression, et s'il voit `->` il t'appelle avec la première clé :

```java
/** Le '#' est consommé, le '{' aussi, la première clé est lue ; le token courant est ARROW. */
private SyntaxTree.Expression parseMapRest(Token hash, SyntaxTree.Expression firstKey) {
    List<SyntaxTree.MapEntry> entries = new ArrayList<>();
    expect(Tag.ARROW, Diagnostics.Code.SYNTAX_ERROR, "'->' attendu après la clé");
    entries.add(new SyntaxTree.MapEntry(firstKey, ???, firstKey.line(), firstKey.column()));  // la valeur : parseExpression()
    while (match(Tag.COMMA)) {
        SyntaxTree.Expression key = ???;                  // parseExpression()
        expect(Tag.ARROW, Diagnostics.Code.SYNTAX_ERROR, "'->' attendu après la clé");
        entries.add(new SyntaxTree.MapEntry(key, ???, key.line(), key.column()));
    }
    expect(Tag.RBRACE, Diagnostics.Code.MISSING_BRACE, "'}' attendu à la fin de la map");
    return new SyntaxTree.MapLiteral(entries, hash.line(), hash.column());
}
```

Les imports : `import java.util.List` (un nom qualifié = des `ID` séparés par des `DOT`), et `import java.util.*`
(se termine par `STAR`). Le `;` est facultatif.

```java
private SyntaxTree.ImportDecl parseImport() {
    Token keyword = advance();                               // import
    StringBuilder name = new StringBuilder(expect(Tag.ID, Diagnostics.Code.SYNTAX_ERROR, "nom attendu après 'import'").lexeme());
    while (match(Tag.DOT)) {
        if (match(Tag.STAR)) { name.append(".*"); break; }
        name.append('.').append(???);                         // le prochain ID (expect(...).lexeme())
    }
    match(Tag.SEMICOLON);
    return new SyntaxTree.ImportDecl(name.toString(), keyword.line(), keyword.column());
}
```

La boucle qui appelle `parseImport()` avant les agents est déjà dans `parse()` (PR 0). Un `import`
après un `agent` est une erreur de syntaxe (le Parser le voit comme une déclaration inconnue).

## Étape 4 : écrire le Semantic (1 h 30)

**La matrice « autorisé / refusé »** : écris-la **d'abord** (dans `docs/NIVEAU2_LIMITES.md`), puis le code.

| Receveur | Méthode | Arguments | Résultat | Python |
|---|---|---|---|---|
| `Map<K,V>` ou `HashMap<K,V>` ou `TreeMap<K,V>` | `put` | `K`, `V` | `void` | `m[k] = v` |
| idem | `get` | `K` | `V` | `m.get(k)` |
| idem | `size` | aucun | `int` | `len(m)` |
| idem | `containsKey` | `K` | `boolean` | `(k in m)` |
| tout le reste (`remove`, `keySet`, `values`, `isEmpty`...) | | | | `UNSUPPORTED_FEATURE` |
| `TreeMap` seulement : `firstKey`, `lastKey`, `headMap`... | | | | `UNSUPPORTED_FEATURE`, message « dépend de l'ordre trié » |

- `put` est de type `void` : on ne l'utilise que comme instruction. `var x = m.put("a", 1)` donne
  `UNSUPPORTED_FEATURE` (construction valide en SARL, que notre sous-ensemble ne traduit pas ; `requireValue` du Membre 1). Pourquoi ? En SARL (comme en Java) `put` **renvoie l'ancienne valeur** ; `m[k] = v` en
  Python ne renvoie rien. Plutôt que traduire faux, on refuse de l'utiliser comme valeur.
- `get` d'une clé absente : en SARL `null`, en Python `None` (`m.get(k)`). C'est fidèle ; documente-le.

**Le cas `TreeMap`** (décision du contrat, §8) : une `TreeMap` garde ses clés **triées** ; un `dict` Python
garde l'ordre d'**insertion**. Ici, le tri n'est observable qu'en parcourant la map ou en l'affichant, et
le sous-ensemble n'a ni `for` ni affichage de collection (`info` les refuse). Les 4 opérations ci-dessus
ne dépendent pas de l'ordre : traduire par un `dict` est donc fidèle. Toute opération qui dépend de l'ordre
est refusée. Écris dans les limites : « TreeMap : seul put/get/size/containsKey, pas de parcours ».

**Les règles de typage** (`typeOfMapLiteral`, `typeOfNew` pour `HashMap` / `TreeMap`) :
- `new HashMap<K,V>()` et `new TreeMap<K,V>()` : **deux** arguments de type, **aucun** argument au constructeur,
  sinon `UNSUPPORTED_FEATURE`.
- Une clé de type collection (`List`, `Set`, `Map`) est refusée (`UNSUPPORTED_FEATURE`) : Python ne sait pas
  hacher une liste.
- `#{k1 -> v1, k2 -> v2}` → `ImmutableMap<K,V>` (littéral immuable : `put` refusé, `UNSUPPORTED_FEATURE`) où `K` est le type de la première clé et `V` celui de la première valeur ;
  les autres doivent être compatibles (`Types.isAssignable`), sinon `TYPE_MISMATCH`.
- Les arguments de `put`, `get`, `containsKey` doivent être compatibles avec `K` et `V` : `TYPE_MISMATCH`.
  Mauvais nombre d'arguments : `WRONG_ARGUMENT_COUNT`.

**Les imports** (`checkImport`) : liste blanche. On accepte `java.util.List`, `java.util.Set`, `java.util.Map`,
`java.util.ArrayList`, `java.util.HashSet`, `java.util.HashMap`, `java.util.TreeMap` et `java.util.*`. Tout le reste
→ `UNSUPPORTED_FEATURE`, à la position de l'import, avec le nom complet dans le message. Où l'appeler : dans
`analyze`, pour chaque élément de `program.imports()`. Les collections fonctionnent avec ou sans `import`
(décision du contrat).

## Étape 5 : écrire le Générateur (45 min)

| SARL | Python |
|---|---|
| `#{"a" -> 1}` | `{"a": 1}` |
| `new HashMap<K,V>()` et `new TreeMap<K,V>()` | `{}` |
| `m.put(k, v)` | `m[k] = v` |
| `m.get(k)` | `m.get(k)` |
| `m.size()` | `len(m)` |
| `m.containsKey(k)` | `(k in m)` |

`generateMapCall(MemberCall call)` (valeurs : `get`, `size`, `containsKey`) et `generateMapStatement(MemberCall call)` (l'instruction `put`, qui écrit sa ligne avec `line(...)`) sont appelées par le Membre 2 quand le receveur est une map. Tu écris le
receveur et les arguments avec `expression(...)`. **Ne réécris pas** la traduction des expressions.
`put` est généré par **`generateMapStatement`**, qui écrit directement la ligne `m[k] = v` : ce n'est pas une
expression Python, donc `generateMapCall` ne le traite jamais (il ne gère que `get`, `size` et `containsKey`).

**Les imports ne produisent aucune ligne Python** : il n'y a donc rien à écrire pour eux dans le Générateur
(ça fait partie de la décision : « émettre uniquement les imports Python correspondant à des
fonctionnalités effectivement supportées » : ici, aucune). Un test vérifie qu'il n'y a pas de ligne `import`
dans le Python.

## Étape 6 : les tests, `MapImportTest.java` (1 h)

| Test | SARL (dans `agent A { on Initialize { ... } }`) | Ce qu'on vérifie |
|---|---|---|
| `HashMap` vide | `var m = new HashMap<String, Integer>()` | `m = {}` |
| littéral avec paires | `var m = #{"a" -> 1, "b" -> 2}` | `m = {"a": 1, "b": 2}` |
| `put` / `get` / `size` / `containsKey` | `m.put("a", 1)` etc. | `m["a"] = 1`, `m.get("a")`, `len(m)`, `("a" in m)` |
| type de valeur incompatible | `m.put("a", "x")` sur `Map<String, Integer>` | échec, `TYPE_MISMATCH` |
| type de clé incompatible | `m.put(1, 1)` sur `Map<String, Integer>` | échec, `TYPE_MISMATCH` |
| opération inconnue | `m.remove("a")` | échec, `UNSUPPORTED_FEATURE` |
| `put` utilisé comme valeur | `var x = m.put("a", 1)` | échec, `UNSUPPORTED_FEATURE` |
| `put` sur un littéral | `var m = #{"a" -> 1} m.put("b", 2)` | échec, `UNSUPPORTED_FEATURE` (littéral immuable) |
| types boîtes | `var m = new HashMap<String, Integer>() m.put("Max", 20)` | succès, `m["Max"] = 20` |
| clé de type collection | `new HashMap<List<Integer>, Integer>()` | échec, `UNSUPPORTED_FEATURE` |
| import autorisé | `import java.util.Map` avant l'agent | succès ; aucune ligne d'import venant de `java` dans le Python (la seule ligne `import` est `from sarl_runtime import Agent`) |
| import inconnu | `import java.io.File` | échec, `UNSUPPORTED_FEATURE` |
| `TreeMap` restreinte | `var t = new TreeMap<String, Integer>() t.put("b", 1)` | succès, `t = {}` puis `t["b"] = 1` |
| `TreeMap` dépendante de l'ordre | `t.firstKey()` | échec, `UNSUPPORTED_FEATURE` (« dépend de l'ordre trié ») |

Les tests qui utilisent `var m = ...` ou des appels avec arguments attendent les PR des Membres 1 et 2 : écris-les
`@Disabled` puis active-les quand elles sont fusionnées. Pour la partie import et le littéral, tu n'attends
personne.

## Étape 7 : pousser

`git add .`, `git commit -m "Membre 4 niveau 2 : maps, TreeMap, imports"`, `git push -u origin membre4-maps-imports`,
Pull Request relue par un autre membre avant fusion. Mets à jour `docs/NIVEAU2_LIMITES.md` avec la matrice
des opérations et la décision `TreeMap`.

## Checklist de fin

- [ ] `parseMapRest`, `parseImport` écrits
- [ ] `typeOfMapCall`, `typeOfMapLiteral`, `typeOfNew` (maps), `checkImport` écrits ; matrice documentée
- [ ] `generateMapLiteral`, `generateMapCall`, `generateMapStatement`, `generateNew` (maps) écrits
- [ ] `TreeMap` : opérations dépendantes de l'ordre refusées, limites écrites
- [ ] `put` refusé comme valeur ; imports hors liste blanche refusés
- [ ] les tests de `MapImportTest` verts ; `mvn test` entièrement vert
- [ ] tu sais expliquer chaque ligne (règle du groupe : signaler l'aide d'une IA par un commentaire)

## Questions pour vérifier que tu maîtrises ton code

1. Que se passe-t-il, étape par étape, pour `m.put("a", 1)` ? Qui reconnaît quoi (Lexer, Parser, Semantic,
   Générateur) ?
2. Pourquoi `put` est-il de type `void` dans notre sous-ensemble ?
3. Pourquoi peut-on traduire une `TreeMap` par un `dict`, **ici**, sans tricher ? Que faudrait-il pour que
   ça devienne faux ? (Un `for` sur la map, un affichage de la map, `firstKey`.)
4. Pourquoi refuse-t-on une clé de type `List` ?
5. Pourquoi les imports ne produisent-ils aucune ligne Python ? Pourquoi les vérifier quand même ?
6. Comment le Parser distingue-t-il `#{1, 2}` (ensemble) de `#{"a" -> 1}` (map) ?
7. Qu'est-ce qui différencie `m.get(k)` en SARL et en Python pour une clé absente ?

## Si tu es bloqué

- Affiche le Python (`System.out.println(r.python())`) et les erreurs (`r.errors()`).
- Une méthode du Membre 2 ou 3 n'existe pas encore : teste avec le littéral et l'import, qui ne dépendent de
  personne.
- Dis-le au groupe avant de modifier un nœud ou un fichier partagé.
