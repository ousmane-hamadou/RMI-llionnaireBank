# Système Distribuée

## 1. Organisation et structure du projet

Le projet est structuré en trois modules distincts :

* **[counter](./counter/) :** Le Terminal Opérateur (Client RMI).
* **[server](./server/) :** Les Serveurs d'Agence (Nœuds du réseau).
* **[shared](./shared/) :** Contient l'ensemble des interfaces et des structures de données partagées entre les modules `counter` et `server`.

Pour plus d'information [README.md](./README.md)

## 2. Transparence de la numérotation (ID Generation)

### Fichiers concernés
* `Challenge.java`
* `IDGenerator.java`
* `IDGeneratorImpl.java`

La génération des **identifiants de transactions** est orchestrée de la manière suivante :

### 2.1. Consensus Initial
Au démarrage, le service du générateur de références d'un nœud produit un nombre aléatoire nommé **challenge**. Chaque nœud interroge ses pairs (`IDGeneratorImpl::askOtherChallenge()`) pour récupérer leurs valeurs respectives via la méthode `IDGeneratorImpl::challenge()`. Le nœud compare son nombre aux autres : si sa valeur est strictement supérieure à toutes les autres, il obtient le droit initial de générer des références (détention du **token**).

### 2.2. Génération d'ID Global : `IDGeneratorImpl::getNextId()`
Chaque nœud initialise son compteur interne à 0. Lorsqu'une transaction doit être émise, l'appel à `getNextId()` suit ce protocole :

1.  **Boucle de négociation :** Le nœud entre dans une phase d'attente active jusqu'à l'obtention du token.
    * Si le nœud possède déjà le token, il passe directement à l'incrémentation.
    * Sinon, il identifie le détenteur actuel via `IDGeneratorImpl::whoHasToken(IDGenerator asker)`.
    * Il sollicite ensuite la libération du token via `IDGeneratorImpl::latchToken()`. Le détenteur distant relâche le token s'il n'est pas en train de générer un REF et transmet la valeur actuelle de son compteur.
    * Le nœud courant met à jour son compteur interne avec la valeur reçue et devient le nouveau possesseur du token.
2.  **Incrémentation atomique :** Le système renvoie la valeur actuelle et l'incrémente pour la transaction suivante.

---

## 3. Fonctions Distantes

### 3.1. Générateur de références global
Défini dans l'interface `IDGenerator.java`, il expose les méthodes suivantes :

* **`int getNextId()`** : Méthode principale pour obtenir une référence unique globale.
* **`IDGenerator whoHasToken(IDGenerator asker)`** : Localise le détenteur du token dans le réseau. Le paramètre `asker` prévient les boucles de requêtes infinies.
* **`Challenge challenge()`** : Expose la valeur de challenge pour le consensus d'élection.
* **`int latchToken()`** : Demande le transfert de propriété du token et récupère l'état du compteur distant pour assurer la continuité des REFs.
* **`void askOtherChallenge()`** : Phase d'initialisation comparant les challenges des pairs enregistrés.
* **`String getName()`** : Identifiant unique du nœud pour l'aiguillage des opérations.

### 3.2. MoneyOrder (Transactions)
Défini dans `MoneyOrder.java`, c'est l'interface principale de dialogue entre le client RMI et le nœud d'agence :

* **`Order issuing(String from, String to, int amount)`** : Émet un mandat en récupérant un REF global et l'enregistre localement.
* **`CashedStatus cashing(int ref)`** : Procède au paiement. Si le mandat est absent localement, lance une recherche distribuée pour valider le retrait sur le nœud distant.
* **`Status cancelling(int ref)`** : Annule un mandat, localement ou à distance.
* **`Order tracking(int ref)`** : Localise un mandat à travers tout le maillage du réseau.
* **`Order updateOrderStatus(Order order, Status status)`** : Met à jour le statut d'un ordre de manière atomique (directement ou par délégation via `ordersForPrecessExternal`).

---

## 4. Routage

### 4.1. Architecture de localisation
Le routage des transactions et des recherches repose sur une architecture de localisation distribuée, geré par la méthode `MoneyOrderImpl.findOnExternalNodes()`. Lorsqu'un nœud ne trouve pas une transaction dans son **`activityLog`** local, il interroge ses pairs en parallèle.

Pour gérer cette dispersion, chaque transaction localisée est encapsulée dans une structure **`Pair`**, un wrapper associant l'objet **`Order`** à l'instance distante **`MoneyOrder`** qui en est la véritable détentrice. Cette association est ensuite mémorisée dans la **`ordersForProcessExternal`**, permettant au nœud courant de déléguer précisément les mises à jour de statut (`updateOrderStatus`) au bon détenteur sans nécessiter de nouvelle recherche.


### 4.1. Prévention des recherches circulaires
Pour empêcher les recherches circulaires, le système utilise la structure **`orderStillBeingResearched`**. Avant de propager une requête, le nœud y inscrit la référence traitée ; si une demande identique arrive alors que la référence y figure déjà, elle est immédiatement rejetée pour briser la boucle.

Ce mécanisme, couplé à l'utilisation du wrapper **`Pair`**, garantit que chaque transaction est routée efficacement vers son nœud d'origine tout en assurant que les recherches distribuées restent finies.

---

## 5. Parallélisme et Gestion de la Concurrence

### 5.1. Échelle locale
Le parallélisme est géré par des structures de données *thread-safe* natives, éliminant le besoin de mutex classiques :
* **`activityLog` (`ConcurrentSkipListSet`) :** Assure l'atomicité des écritures et le tri des mandats sans bloquer les threads de lecture.
* **`AtomicInteger` :** Garantit une incrémentation indivisible lors des émissions simultanées.
* **`CompletableFuture` :** Permet de lancer des requêtes réseau asynchrones vers tous les pairs. Le temps de réponse global est réduit à la latence du nœud le plus lent.



### 5.2. Échelle globale
La cohérence réseau repose sur deux piliers :
1.  **Immutabilité :** L'usage des **Java Records** pour la classe `Order` garantit qu'un objet ne peut être modifié. Tout changement d'état produit une nouvelle instance, supprimant les effets de bord.
2.  **Contrôle de flux :** Le **token** arbitre la création de REFs tandis que `orderStillBeingResearched` agit comme une barrière contre la saturation des ressources.

---

## 6. Passage à l'Échelle


### 6.1. Architecture en Clusters Hiérarchiques
Le passage à une échelle (par ex. Région -> Pays -> Continent) peut se reposer sur une organisation en clusters. Plutôt qu'un maillage plat où chaque nœud interroge tous les autres, le réseau segmente le trafic pour favoriser la **localité des échanges**.

* **Clusters de Proximité :** Les nœuds d'une même région forment un cluster de premier niveau. La pluspart des recherches (`tracking`) se résolvent ici avec une latence minimale.
* **Nœuds Passerelles :** Chaque cluster élit des passerelles chargées de propager les requêtes vers le niveau supérieur (Pays ou Continent par exemple) uniquement si l'ordre n'est pas trouvé localement.

### 6.2. Transparence de Localisation
Pour garantir que le client RMI n'ait pas à gérer la complexité géographique, le système implémente une transparence totale :

* **Routage plus complexe via Wrapper `Pair` :** Le wrapper **`Pair`** évolue pour encapsuler non seulement le stub RMI final, mais aussi le chemin de routage. Pour le nœud appelant, l'exécution d'un `updateOrderStatus` reste identique, que le nœud cible soit dans le meme quatier ou sur un autre continent.
* **DHT (Distributed Hash Table) :** À très grande échelle, le broadcast est remplacé par une DHT. La référence de la transaction est hachée pour identifier un "nœud annuaire" responsable de connaître la localisation exacte de l'ordre. La recherche devient ciblée au lieu de massive.

### 6.3. Fragmentation du token
Le token unique actuel devient un goulot d'étranglement à plus grande  l'échelle à cause de la latence (intercontinentale par exemple). La solution retenue est la **fragmentation de l'espace de nommage** :

* **Tokens Régionaux :** Chaque region ou pays ou continent gère son propre token pour un préfixe d'ID spécifique (ex: `CM-101`, `TD-202`).
* **Parallélisme Total :** Les nœuds de différents continents n'entrent jamais en compétition pour le même token. Cela permet une émission de mandats en parallèle à l'échelle planétaire tout en conservant l'unicité globale grâce aux préfixes.


En Resume, grâce à l'absence de **mutex** bloquants et à l'utilisation de structures atomiques, chaque nœud peut monter en charge verticalement (plus de CPU/RAM), tandis que l'architecture en clusters permet une montée en charge horizontale (plus de nœuds) sans dégrader le temps de réponse global.


## 7. Synchronisation

Pour maximiser les performances, le système remplace les verrous classiques (mutex) par des structures de données atomiques et immuables :

* **`ConcurrentSkipListSet` (`ActivityLog`)** : Permet des ajouts et lectures simultanés de transactions sans jamais geler le serveur. Là où une `ArrayList` avec mutex bloquerait tout le monde, cette structure non-bloquante assure un accès fluide et ordonné aux données.


* **`AtomicInteger` (`IDGenerator`)** : Garantit l'unicité des refs via une synchronisation matérielle. Elle évite les collisions de références sans la latence d'un verrouillage logiciel, même lors d'émissions massives de mandats.


* **`ConcurrentHashMap` (`ordersForPrecessExternal`)** : Synchronise l'aiguillage vers les nœuds distants par segmentation. Elle permet à plusieurs operations de lire ou d'écrire des routes différentes en parallèle, évitant le goulot d'étranglement d'une map protégée par un mutex global.

* **`ConcurrentSkipListSet` (`orderStillBeingResearched`)** : Agit comme une barrière atomique contre les boucles infinies. Elle vérifie instantanément si une recherche est en cours, permettant de rejeter les doublons réseau sans introduire de délai de verrouillage.

* **`Java Record` (`Order`)** : Assure la cohérence par l'**immutabilité**. Puisque la donnée ne peut pas changer, elle est naturellement *thread-safe*. Le besoin de mutex pour protéger l'accès aux champs disparaît.


* **`Pair` (Wrapper Order/MoneyOrder)** : Encapsule la donnée et son lien distant, supprimant le besoin de synchroniser une table de correspondance externe. La transaction transporte son propre "moyen d'action", simplifiant le routage global.
