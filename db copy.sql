-- Création de la table Produit
CREATE TABLE Produit (
    id_produit INT AUTO_INCREMENT PRIMARY KEY,
    reference VARCHAR(50) UNIQUE NOT NULL,
    designation VARCHAR(100) NOT NULL,
    description TEXT NULL,
    prix_achat DECIMAL(10, 2) NOT NULL,
    prix_vente DECIMAL(10, 2) NOT NULL,
    date_creation DATETIME DEFAULT CURRENT_TIMESTAMP
);


-- Création de la table StockPrincipal
CREATE TABLE StockPrincipal (
    id_stock_principal INT AUTO_INCREMENT PRIMARY KEY,
    id_produit INT NOT NULL,
    quantite INT NOT NULL DEFAULT 0,
    date_arrivee DATETIME NOT NULL,
    date_sortie DATETIME NULL,
    statut ENUM('en_stock', 'sorti', 'transféré') DEFAULT 'en_stock',
    FOREIGN KEY (id_produit) REFERENCES Produit(id_produit)
);

-- Création de la table SousEntite
CREATE TABLE SousEntite (
    id_sous_entite INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    adresse VARCHAR(255) NULL,
    responsable VARCHAR(100) NULL
);

-- Création de la table StockSousEntite
CREATE TABLE StockSousEntite (
    id_stock_sous_entite INT AUTO_INCREMENT PRIMARY KEY,
    id_produit INT NOT NULL,
    id_sous_entite INT NOT NULL,
    quantite INT NOT NULL DEFAULT 0,
    date_transfert DATETIME NOT NULL,
    derniere_mise_a_jour DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (id_produit) REFERENCES Produit(id_produit),
    FOREIGN KEY (id_sous_entite) REFERENCES SousEntite(id_sous_entite)
);

-- Création de la table Transfert
CREATE TABLE Transfert (
    id_transfert INT AUTO_INCREMENT PRIMARY KEY,
    id_produit INT NOT NULL,
    id_sous_entite_source INT NULL,
    id_sous_entite_destination INT NOT NULL,
    quantite INT NOT NULL,
    date_transfert DATETIME DEFAULT CURRENT_TIMESTAMP,
    effectue_par VARCHAR(100) NULL,
    FOREIGN KEY (id_produit) REFERENCES Produit(id_produit),
    FOREIGN KEY (id_sous_entite_source) REFERENCES SousEntite(id_sous_enti te),
    FOREIGN KEY (id_sous_entite_destination) REFERENCES SousEntite(id_sous_entite)
);

-- Création de la table Vente
CREATE TABLE Vente (
    id_vente INT AUTO_INCREMENT PRIMARY KEY,
    id_produit INT NOT NULL,
    id_sous_entite INT NOT NULL,
    quantite INT NOT NULL,
    prix_unitaire DECIMAL(10, 2) NOT NULL,
    montant_total DECIMAL(10, 2) NOT NULL,
    date_vente DATETIME DEFAULT CURRENT_TIMESTAMP,
    vendu_par VARCHAR(100) NULL,
    FOREIGN KEY (id_produit) REFERENCES Produit(id_produit),
    FOREIGN KEY (id_sous_entite) REFERENCES SousEntite(id_sous_entite)
);

-- Création de la table Utilisateur
CREATE TABLE Utilisateur (
    id_utilisateur INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(50) NOT NULL,
    prenom VARCHAR(50) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    mot_de_passe VARCHAR(255) NOT NULL,
    role ENUM('admin', 'gestionnaire', 'vendeur') NOT NULL
);

-- Création de la table Statistiques
CREATE TABLE Statistiques (
    id_statistique INT AUTO_INCREMENT PRIMARY KEY,
    mois_annee DATE NOT NULL,
    chiffre_affaire DECIMAL(12, 2) NULL,
    produits_vendus INT NULL,
    benefice DECIMAL(12, 2) NULL,
    UNIQUE KEY unique_mois (mois_annee)
);