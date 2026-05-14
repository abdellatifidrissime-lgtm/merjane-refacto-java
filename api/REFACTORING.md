# Refactoring Notes

## Contexte

Application Spring Boot gérant des commandes de produits de types différents :
**NORMAL**, **SEASONAL**, **EXPIRABLE**.

---

## Problèmes identifiés dans le code original

### 1. Logique métier dans le Controller
`MyController.processOrder()` contenait ~40 lignes de logique métier pure
(if/else sur les types, calculs de dates, gestion du stock) directement dans
la couche HTTP. Violation directe du SRP.

### 2. `type` comme `String` — Primitive Obsession
Le type du produit était un `String` ("NORMAL", "SEASONAL", "EXPIRABLE").
Pas de type-safety, erreurs silencieuses possibles, pas de support IDE.

### 3. Logique dupliquée entre Controller et Service
La logique EXPIRABLE et SEASONAL existait en deux endroits distincts avec
des comportements légèrement différents, source de bugs à l'évolution.

### 4. Persistence dans le Controller
`ProductRepository` était injecté directement dans `MyController` qui
appelait `pr.save()` — couplage fort, impossible à tester unitairement.

### 5. Injection par champ (`@Autowired`)
Tous les services utilisaient `@Autowired` sur les champs, rendant les
classes non testables sans contexte Spring complet.

### 6. Nommage cryptique
Variables `ps`, `pr`, `or`, `ns`, `p` — illisibles sans contexte.

---

## Décisions de refactoring

### ✅ Création de `ProductType` (Enum)
Remplacement du `String type` par un enum typé.
- Avantage : type-safety, support IDE, switch exhaustif garanti par le compilateur.

### ✅ Extraction de `OrderService`
Toute la logique de traitement des commandes a été extraite du Controller
vers un `OrderService` dédié.
- Chaque type de produit est géré par une méthode privée dédiée (`handleNormalProduct`,
  `handleSeasonalProduct`, `handleExpirableProduct`).
- Le switch sur `ProductType` est centralisé en un seul endroit.
- Avantage : SRP respecté, logique testable indépendamment du HTTP.

### ✅ Simplification de `MyController`
Réduit à son rôle unique : recevoir la requête HTTP, déléguer, retourner la réponse.
De 60 lignes à 15 lignes.

### ✅ Injection par constructeur partout
Remplacement de tous les `@Autowired` sur champs par injection constructeur.
- Avantage : immutabilité, testabilité sans Spring, dépendances explicites.

### ✅ Nommage expressif
Renommage de toutes les variables abrégées en noms complets et lisibles.

---

## Couverture des tests

### Avant
- 1 test unitaire trivial (`notifyDelay`)
- 1 test d'intégration global (tous les types mélangés)

### Après
- 6 tests unitaires couvrant les 3 types et leurs cas limites
- 6 tests d'intégration isolés par cas métier
- Chaque comportement métier a son propre test nommé explicitement

---

## Architecture finale
HTTP Request
└── MyController          → reçoit et délègue
└── OrderService  → logique de commande par type
└── ProductService → logique métier par produit
└── NotificationService → notifications
└── ProductRepository   → persistence

---

## Ce qui n'a pas été modifié (WARN)

Conformément aux consignes, les classes suivantes n'ont pas été touchées :
- `Order.java`
- `NotificationService.java`
- `ProcessOrderResponse.java`