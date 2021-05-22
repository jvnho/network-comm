Instructions de compilation:
- Ouvrir un terminal dans le répertoire courant, entrez ./compile.sh
(s'il le faut, entrez avant: chmod +x compile.sh)

Instructions d'exécutions:
    -pour lancer le gestionnaire de diffuseur: 
        - Dans un terminal, entrez java StreamManager 4141 (peut être un port quelconque mais doit alors être modifier dans les autres fichiers de configuration)

    - pour lancer un diffuseur:
        - Dans un terminal, entrez java Streamer diff1 msg 
    
            diff1 correspond à un fichier de configuration du diffuseur:
                LIGNE1: nom du diffuseur
                LIGNE2: adresse de multidiffusion
                LIGNE3: port de multidiffusion
                LIGNE4: port d'écoute TCP
                LIGNE5: adresse du gestionnaire de diffuseur
                LIGNE6: port d'écoute du gestionnaire de diffuseur

            msg est un fichier comportant les messages qui seront diffusés

    - pour lancer un client:
        - Dans un terminal, entrez ./client client1

            client1 correspond à un fichier de configuration client
                LIGNE1: nom du client 
                LIGNE2: adresse du gestionnaire de diffuseur
                LIGNE3: port d'écoute du gestionnaire de diffuseur

        - Choisir le diffuseur
        - Ouvrir dans Atom ou Visual Studio Code le fichier reception.txt pour voir l'ensemble des messages recus


Architecture du code:

StreamManager.java: code pour le gestionnaire de diffuseur
Format.java: vérifications de format pour le gestionnaire 
Streamer.java: code pour le diffuseur
Message.java: structure utilisé pour les messages envoyés par le diffuseur
client.c: code pour le client
checker.c: vérifications de format pour le client  
    