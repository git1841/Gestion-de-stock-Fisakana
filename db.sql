
-- Création de la table StockPrincipal
CREATE TABLE StockPrincipal (
    id_stock_principal INT AUTO_INCREMENT PRIMARY KEY,
    nomproduit VARCHAR(50) UNIQUE NOT NULL,
    unite VARCHAR(50) UNIQUE NOT NULL,
    quantite INT NOT NULL DEFAULT 0,
    date_arrivee DATETIME NOT NULL,
    description TEXT NULL,
    prix_unitaire DECIMAL(10, 2) NOT NULL,
    prix_total DECIMAL(10, 2) NOT NULL,
    
);

-- Création de la table SousEntite
CREATE TABLE SousEntite (
    id_sous_entite INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    adresse VARCHAR(255) NULL,
);

-- Création de la table StockSousEntite
CREATE TABLE StockSousEntite (
    id_stock_sous_entite INT AUTO_INCREMENT PRIMARY KEY,
    id_stock_principal INT NOT NULL,
    id_sous_entite INT NOT NULL,
    quantite INT NOT NULL DEFAULT 0,
    date_transfert DATETIME NOT NULL,
    derniere_mise_a_jour DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (id_stock_principal) REFERENCES StockPrincipal(id_stock_principal),
    FOREIGN KEY (id_sous_entite) REFERENCES SousEntite(id_sous_entite)
);

-- Création de la table Transfert
CREATE TABLE Transfert (
    id_transfert INT AUTO_INCREMENT PRIMARY KEY,
    id_stock_principal INT,
    id_sous_entite INT NULL,
    quantite INT NOT NULL,
    date_transfert DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_stock_principal) REFERENCES StockPrincipal(id_stock_principal),
    FOREIGN KEY (id_sous_entite) REFERENCES SousEntite(id_sous_entite),
);

-- Création de la table Vente
CREATE TABLE Vente (
    id_vente INT AUTO_INCREMENT PRIMARY KEY,
    id_stock_principal INT NOT NULL,
    id_sous_entite INT NOT NULL,
    id_stock_sous_entite INT NOT NULL,
    quantite INT NOT NULL,
    prix_vent_unite DECIMAL(10, 2) NOT NULL,
    montant_total DECIMAL(10, 2) NOT NULL,
    date_vente DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_produit) REFERENCES Produit(id_produit),
    FOREIGN KEY (id_sous_entite) REFERENCES SousEntite(id_sous_entite)
);

-- Création de la table Utilisateur
CREATE TABLE Utilisateur (
    id_utilisateur INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(50) NOT NULL,
    prenom VARCHAR(50) NOT NULL,
    mot_de_passe VARCHAR(255) NOT NULL,
);

-- Création de la table Statistiques
CREATE TABLE Statistiques (
    id_statistique INT AUTO_INCREMENT PRIMARY KEY,
    id_vente INT,
    mois_annee DATE NOT NULL,
    chiffre_affaire DECIMAL(12, 2) NULL,
    produits_vendus INT NULL,
    benefice DECIMAL(12, 2) NULL,
    UNIQUE KEY unique_mois (mois_annee)
);