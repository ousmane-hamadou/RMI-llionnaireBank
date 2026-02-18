# RMI-llionaireBank - Système de Transfert d'Argent Distribué

## Cadre Académique
*   **Cours :** Systèmes Distribués
*   **Enseignant :** Pr. Nlong
*   **Institution :** Université de Ngaoundéré
*   **Projet :** Travail de Groupe

## Membres du Groupe
1.  **OUSMANE HAMADOU**
2.  **DAMLAR DINGAM RUTH**
3.  **MAHAMAT ZAKARIA OUCHAR**
4.  **ISSA BRAHIM MAHAMAT**
5.  **AL-MINE ORO KONDI**
6.  **YOUNOUSSA IBRAHIMA**
7.  **ARABI MAHAMAT SALEH**

---

## Présentation du Projet
Ce projet implémente une solution de transfert d'argent multi-agences basée sur une architecture **Java RMI (Remote Method Invocation)**. L'application simule le fonctionnement de 3 (ou plus) agences bancaires interconnectées, permettant des opérations de guichet fluides et synchronisées sur l'ensemble du cluster.

### Architecture du Système & Fonctionnalités
*   **Topologie :** Réseau de 3+ serveurs (un par agence) interconnectés en mode "Peer-to-Peer" pour la validation distribuée.
*   **Persistence :** Gestion des mandats en mémoire locale (In-Memory Storage) sur chaque serveur émetteur.
*   **Opérations de Guichet :** Issuing (Envoi), Cashing (Retrait), Cancelling (Annulation), Tracking (Suivi).
*   **Génération Globale :** Les numéros de mandats (**REF**) sont uniques et séquentiels sur tout le réseau.
*   **Interopérabilité :** Un opérateur peut émettre, retirer ou annuler un mandat depuis n'importe quel guichet du réseau grâce à la communication inter-serveurs.

---

## Fonctionnalités Implémentées
Le jargon bancaire pour une expérience utilisateur réaliste :

1.  **ISSUING (Envoi) :** Émission d'un nouveau mandat avec génération globale de numéro unique (**MTCN**).
2.  **CASHING (Retrait) :** Paiement d'un mandat au bénéficiaire au guichet (**Payout**).
3.  **CANCELLING (Annulation) :** Invalidation définitive d'une transaction (**Void**).
4.  **TRACKING (Suivi) :** Consultation de l'état du mandat (**Pending**, **Collected**, **Cancelled**).

---

## Stack Technique & Dépendances
Le projet nécessite **Java 21** (ou supérieur) pour supporter les **Records**, le *Switch Pattern Matching*.

### Dépendances principales :
*   **Lombok :** Génération automatique du code (Getters, Setters, Loggers).
*   **Picocli :** Interface en ligne de commande (CLI) interactive et typée.
*   **Resilience4j (Retry) :** Gestion de la résilience lors des connexions entre les serveurs distribués.
*   **Logback :** Journalisation (Logging) structurée des événements serveurs et des erreurs réseaux.

---

## Guide d'Exécution
Le projet utilise de Gradle. Avant toute exécution, assurez-vous d'avoir généré la distribution locale via la commande `./gradlew installDist`.

### 1. Lancement des Serveurs d'Agence (Cluster RMI)
Chaque agence fait office de **Nœud (Node)** dans le système distribué. Pour que le retrait d'un mandat puisse se faire depuis n'importe quel guichet, chaque serveur doit être lancé en spécifiant ses pairs (**remote-peers**) afin de former le cluster.

**Syntaxe de la commande :**
`./server/build/install/server/bin/server --name=[NOM_SERVEUR] --ip=[ADDRESS_IP] --port=[PORT] --remote-peers=[IP_PEER_1:PORT_PEER_1:NOM_PEER_1],[IP_PEER_2:PORT_PEER_2:NOM_PEER_2],[...]`

*   **Agence du Quartier 1 (Port par default 1099) :**
    ```bash
    ./server/build/install/server/bin/server --name=Agence1 --ip=192.168.1.10 --remote-peers=192.168.1.11:1100:Agence2,192.168.1.12:1101:Agence3
    ```
*   **Agence du Quartier B (Port 1100) :**
    ```bash
    ./server/build/install/server/bin/server --name=Agence2 --ip=192.168.1.11 --port=1100 --remote-peers=192.168.1.10:1099:Agence1,192.168.1.12:1101:Agence3
    ```
*   **Agence du Quartier C (Port 1101 - Version courte des options) :**
    ```bash
    ./server/build/install/server/bin/server -n=Agence3 -i=192.168.1.12 -p=1101 -r=192.168.1.10:Agence1:1099,192.168.1.11:Agence2:1100
    ```
*Note : Le système utilise un mécanisme qui attend (avec timeout) que tous les serveurs soient en ligne avant de valider l'initialisation complète.*

---

### 2. Lancement du Terminal Opérateur (Client RMI)
Le terminal permet à l'agent de guichet de traiter les opérations des clients. Il se connecter son agence pour effectuer des opérations globales.

**Syntaxe de la commande :**
`./counter/build/install/counter/bin/counter --server-ip=[IP_SERVER_AGENCE] --port=[PORT_SERVER_AGENCE] --name=[NOM_AGENCE]`

*   **Connexion à l'agence locale (Configuration par défaut) :**
    ```bash
    # Se connecte a l'agence du Quartier 1
    ./counter/build/install/counter/bin/counter --server-ip=192.168.1.10 --port=1099 --name=Agence1
    ```
Une fois le terminal lancé, l'opérateur utilise le **REF** (numéro de mandat généré globalement) pour effectuer un **Cashing** (Retrait) ou un **Tracking** (Suivi) sur n'importe quel nœud du réseau.

---

*Projet réalisé pour le cours de Systèmes Distribués - Université de Ngaoundéré - 2026*
