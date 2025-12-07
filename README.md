# 🎮 Jeu de Labyrinthe Multijoueur Distribué

Un jeu de labyrinthe multijoueur temps réel utilisant une **architecture distribuée hybride** (Java RMI, UDP, JMS) et démontrant l'interopérabilité avec un client **Python**.

![Aperçu du Jeu](https://via.placeholder.com/800x400?text=Capture+d%27%C3%A9cran+du+Jeu+Labyrinthe)
*(Remplacez ce lien par une vraie capture d'écran de votre jeu)*

## 🚀 Fonctionnalités Clés

Ce projet a été conçu pour valider des compétences techniques avancées en systèmes répartis :

* **Connexion Fiable (RMI) :** Gestion de session et téléchargement de la carte via des objets complexes sérialisés (`MazeState`).
* **Temps Réel (UDP) :** Déplacements fluides des joueurs sans latence grâce au protocole UDP.
* **Messagerie Asynchrone (JMS) :** Chat en direct et notifications d'événements (victoire, connexion) via **ActiveMQ**.
* **Interopérabilité (Python) :** Un "Bot Intelligent" codé en Python qui interagit avec le serveur Java via des sockets UDP bruts.
* **Algorithme Avancé :** Génération de labyrinthe par *Recursive Backtracker* avec boucles (pour éviter les culs-de-sac simples).

## 🛠️ Architecture Technique

Le projet repose sur une architecture hybride optimisée :

| Composant | Technologie | Rôle |
| :--- | :--- | :--- |
| **Serveur de Jeu** | Java RMI + UDP | Gère l'état du monde, les collisions et la synchronisation. |
| **Client Graphique** | Java Swing | Interface utilisateur, affichage du labyrinthe et chat. |
| **Broker de Message** | Apache ActiveMQ | Gère les Topics `maze.chat` et `maze.events` (JMS). |
| **Bot Autonome** | Python (Sockets) | Client UDP tiers démontrant l'ouverture du système. |

## 📋 Prérequis

* **Java JDK 8** (ou supérieur).
* **Apache ActiveMQ 5.16.x** (Classic).
* **Python 3.x** (pour le bot optionnel).
* Bibliothèque : `activemq-all-5.16.7.jar`.

## ⚙️ Installation et Configuration

1.  **Cloner le projet :**
    ```bash
    git clone [https://github.com/votre-repo/maze-multiplayer.git](https://github.com/votre-repo/maze-multiplayer.git)
    cd maze-multiplayer
    ```

2.  **Configurer l'adresse IP :**
    * Ouvrez `src/common/Constants.java`.
    * Modifiez `SERVER_IP` avec l'adresse IP de votre machine serveur (ex: `192.168.1.100`).
    * *Note : Le client Python (`bot_player.py`) doit aussi avoir cette IP.*

3.  **Compiler le projet :**
    Assurez-vous d'avoir le jar `activemq-all-5.16.7.jar` dans un dossier `lib`.
    ```bash
    mkdir out
    javac -cp "lib/activemq-all-5.16.7.jar" -d out src/common/*.java src/server/*.java src/client/*.java
    ```

## ▶️ Instructions de Démarrage

L'ordre de lancement est important.

### 1. Démarrer ActiveMQ
Lancez le broker de messages (indispensable pour le chat).
```bash
# Dans le dossier bin d'ActiveMQ
./activemq start****
