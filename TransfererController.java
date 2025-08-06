package com.votreport.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.cell.PropertyValueFactory;
import java.util.Optional;
import javafx.application.Platform;

import javafx.scene.control.cell.PropertyValueFactory;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.GridPane;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.util.Pair;
import java.text.SimpleDateFormat;

import javafx.geometry.Insets;



public class TransfererController {

    @FXML private TableView<ProductStock> produitsTable;
    @FXML private TableColumn<ProductStock, String> refColumn;
    @FXML private TableColumn<ProductStock, String> designationColumn;
    @FXML private TableColumn<ProductStock, Integer> quantiteColumn;
    
    @FXML private ListView<SousEntite> sousEntiteList;
    @FXML private ListView<TransferItem> transferList;
    @FXML private TextField quantiteField;
    @FXML private Label statusLabel;

    @FXML private TableColumn<ProductStock, Void> colInfo;

    
    private ObservableList<ProductStock> produits = FXCollections.observableArrayList();
    private ObservableList<SousEntite> sousEntites = FXCollections.observableArrayList();
    private ObservableList<TransferItem> transferItems = FXCollections.observableArrayList();
    
    private ProductStock selectedProduct;
    private SousEntite selectedSousEntite;
    
    private Controller Controller;

    public void setController(Controller controller) {
        this.Controller = controller;
    }
    
    @FXML
    private void initialize() {




        // Configure table columns
        refColumn.setCellValueFactory(new PropertyValueFactory<>("reference"));
        designationColumn.setCellValueFactory(new PropertyValueFactory<>("designation"));
        quantiteColumn.setCellValueFactory(new PropertyValueFactory<>("quantite"));

         // Ajoutez la colonne d'info
        colInfo.setCellFactory(param -> new TableCell<ProductStock, Void>() {
            private final Button infoButton = new Button("i");
            
            {
                infoButton.getStyleClass().add("info-button");
                infoButton.setOnAction(event -> {
                    ProductStock product = getTableView().getItems().get(getIndex());
                    showProductDetails(product);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : infoButton);
            }
        });
        
        // Load data
        loadProduitsStockPrincipal();
        loadSousEntite();
        
        // Set data to views
        produitsTable.setItems(produits);
        sousEntiteList.setItems(sousEntites);
        transferList.setItems(transferItems);

        System.out.println("[DEBUG] Hauteur de la TableView: " + produitsTable.getHeight());
        System.out.println("[DEBUG] Visible: " + produitsTable.isVisible());
        System.out.println("[DEBUG] Nombre d'éléments dans la table: " + produitsTable.getItems().size());
            
        // Setup selection listeners
        produitsTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                selectedProduct = newSelection;
                if (newSelection != null) {
                    statusLabel.setText("Produit sélectionné: " + newSelection.getDesignation());
                }
            });
        
        sousEntiteList.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                selectedSousEntite = newSelection;
                if (newSelection != null) {
                    statusLabel.setText("Destination: " + newSelection.getNom());
                }
            });
        
        // Setup drag and drop
        setupDragAndDrop();
    }
    
    private void setupDragAndDrop() {
        // Allow drag from products table
        produitsTable.setOnDragDetected(event -> {
            if (selectedProduct != null) {
                Dragboard db = produitsTable.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.putString(selectedProduct.getReference());
                db.setContent(content);
                event.consume();
            }
        });
        
        // Allow drop on transfer list
        transferList.setOnDragOver(event -> {
            if (event.getGestureSource() != transferList && 
                event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });
        
        // Handle drop event
        transferList.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            
            if (db.hasString() && selectedProduct != null) {
                // Ask for quantity
                Optional<Integer> quantity = askForQuantity(selectedProduct.getQuantite());
                
                if (quantity.isPresent() && quantity.get() > 0) {
                    transferItems.add(new TransferItem(
                        selectedProduct.getIdProduit(),
                        selectedProduct.getReference(),
                        selectedProduct.getDesignation(),
                        quantity.get()
                    ));
                    success = true;
                    statusLabel.setText("Produit ajouté au transfert");
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }
  
    private Optional<Integer> askForQuantity(int max) {
        TextInputDialog dialog = new TextInputDialog("1");
        dialog.setTitle("Quantité à transférer");
        dialog.setHeaderText("Transférer " + selectedProduct.getDesignation());
        dialog.setContentText("Quantité (max: " + max + "):");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                int qte = Integer.parseInt(result.get());
                if (qte > 0 && qte <= max) {
                    return Optional.of(qte);
                }
            } catch (NumberFormatException e) {
                // Invalid input
            }
        }
        return Optional.empty();
    }
    
    @FXML
    private void validerTransfert() {
        if (transferItems.isEmpty()) {
            showAlert("Aucun transfert", "Ajoutez des produits à transférer");
            return;
        }
        
        if (selectedSousEntite == null) {
            showAlert("Destination manquante", "Sélectionnez une sous-entité");
            return;
        }
        
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/fskn", "root", "")) {
            conn.setAutoCommit(false);
            
            for (TransferItem item : transferItems) {
                // 1. Update principal stock
                String updatePrincipal = "UPDATE StockPrincipal SET quantite = quantite - ? WHERE id_produit = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updatePrincipal)) {
                    stmt.setInt(1, item.getQuantite());
                    stmt.setInt(2, item.getIdProduit());
                    stmt.executeUpdate();
                }
                
                // 2. Update or create sub-entity stock
                String checkStock = "SELECT * FROM StockSousEntite WHERE id_produit = ? AND id_sous_entite = ?";
                try (PreparedStatement stmt = conn.prepareStatement(checkStock)) {
                    stmt.setInt(1, item.getIdProduit());
                    stmt.setInt(2, selectedSousEntite.getId());
                    ResultSet rs = stmt.executeQuery();
                    
                    if (rs.next()) {
                        // Update existing stock
                        String updateStock = "UPDATE StockSousEntite SET quantite = quantite + ? WHERE id_stock_sous_entite = ?";
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateStock)) {
                            updateStmt.setInt(1, item.getQuantite());
                            updateStmt.setInt(2, rs.getInt("id_stock_sous_entite"));
                            updateStmt.executeUpdate();
                        }
                    } else {
                        // Create new stock entry
                        String insertStock = "INSERT INTO StockSousEntite (id_produit, id_sous_entite, quantite, date_transfert) VALUES (?, ?, ?, NOW())";
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertStock)) {
                            insertStmt.setInt(1, item.getIdProduit());
                            insertStmt.setInt(2, selectedSousEntite.getId());
                            insertStmt.setInt(3, item.getQuantite());
                            insertStmt.executeUpdate();
                        }
                    }
                }
                
                // 3. Record transfer
                String insertTransfer = "INSERT INTO Transfert (id_produit, id_sous_entite_destination, quantite, date_transfert) VALUES (?, ?, ?, NOW())";
                try (PreparedStatement stmt = conn.prepareStatement(insertTransfer)) {
                    stmt.setInt(1, item.getIdProduit());
                    stmt.setInt(2, selectedSousEntite.getId());
                    stmt.setInt(3, item.getQuantite());
                    stmt.executeUpdate();
                }
            }
            
            conn.commit();
            statusLabel.setText("Transfert effectué avec succès!");
            transferItems.clear();
            loadProduitsStockPrincipal();
            
        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("Erreur lors du transfert: " + e.getMessage());
        }
    }
    

/////////



    private void loadProduitsStockPrincipal() {
        produits.clear();
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/fskn", "root", "")) {
            String sql = "SELECT p.id_produit, p.reference, p.designation, SUM(sp.quantite) as quantite " +
                         "FROM Produit p " +
                         "JOIN StockPrincipal sp ON p.id_produit = sp.id_produit " +
                         "WHERE sp.statut = 'en_stock' " +
                         "GROUP BY p.id_produit, p.reference, p.designation " +
                         "HAVING quantite > 0";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                produits.add(new ProductStock(
                    rs.getInt("id_produit"),
                    rs.getString("reference"),
                    rs.getString("designation"),
                    rs.getInt("quantite")
                ));

            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    

    //////////////
    private void loadSousEntite() {
        sousEntites.clear();
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/fskn", "root", "")) {
            String sql = "SELECT id_sous_entite, nom FROM SousEntite";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                sousEntites.add(new SousEntite(
                    rs.getInt("id_sous_entite"),
                    rs.getString("nom")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    private void retourAccueil() {
        /*try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/votreport/views/acc.fxml"));
            BorderPane accView = loader.load();
            Controller.getRootPane().setCenter(accView);
        } catch (Exception e) {
            e.printStackTrace();
        }*/
       System.out.println("ok");
    }

    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Inner classes for data models



    public static class ProductStock {
        private final int idProduit;
        private final String reference;
        private final String designation;
        private final int quantite;
        
        public ProductStock(int idProduit, String reference, String designation, int quantite) {
            this.idProduit = idProduit;
            this.reference = reference;
            this.designation = designation;
            this.quantite = quantite;
        }
        
        public int getIdProduit() { return idProduit; }
        public String getReference() { return reference; }
        public String getDesignation() { return designation; }
        public int getQuantite() { return quantite; }
    }



    
    public static class SousEntite {
        private final int id;
        private final String nom;
        
        public SousEntite(int id, String nom) {
            this.id = id;
            this.nom = nom;
        }
        
        public int getId() { return id; }
        public String getNom() { return nom; }
        
        @Override
        public String toString() {
            return nom;
        }
    }
    
    public static class TransferItem {
        private final int idProduit;
        private final String reference;
        private final String designation;
        private final int quantite;
        
        public TransferItem(int idProduit, String reference, String designation, int quantite) {
            this.idProduit = idProduit;
            this.reference = reference;
            this.designation = designation;
            this.quantite = quantite;
        }
        
        public int getIdProduit() { return idProduit; }
        public String getReference() { return reference; }
        public String getDesignation() { return designation; }
        public int getQuantite() { return quantite; }
        
        @Override
        public String toString() {
            return quantite + " x " + designation;
        }
    }

    // TransfererController.java
// Ajoutez ces importations


private void showProductDetails(ProductStock product) {
    try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/fskn", "root", "")) {
        // Récupérer les détails du produit
        String productSql = "SELECT * FROM Produit WHERE id_produit = ?";
        PreparedStatement productStmt = conn.prepareStatement(productSql);
        productStmt.setInt(1, product.getIdProduit());
        ResultSet productRs = productStmt.executeQuery();
        
        if (!productRs.next()) {
            showAlert("Erreur", "Produit non trouvé dans la base de données");
            return;
        }
        
        // Créer le dialogue
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Détails du Produit");
        dialog.setHeaderText(product.getDesignation() + " (" + product.getReference() + ")");
        
        // Créer le contenu
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        // Informations produit
        grid.add(new Label("Référence:"), 0, 0);
        grid.add(new Label(productRs.getString("reference")), 1, 0);
        
        grid.add(new Label("Désignation:"), 0, 1);
        grid.add(new Label(productRs.getString("designation")), 1, 1);
        
        grid.add(new Label("Description:"), 0, 2);
        grid.add(new Label(productRs.getString("description")), 1, 2);
        
        grid.add(new Label("Prix d'achat:"), 0, 3);
        grid.add(new Label(String.format("%.2f", productRs.getDouble("prix_achat"))), 1, 3);
        
        grid.add(new Label("Prix de vente:"), 0, 4);
        grid.add(new Label(String.format("%.2f", productRs.getDouble("prix_vente"))), 1, 4);
        
        grid.add(new Label("Date de création:"), 0, 5);
        grid.add(new Label(new SimpleDateFormat("dd/MM/yyyy HH:mm").format(productRs.getTimestamp("date_creation"))), 1, 5);
        
        // Récupérer les stocks dans les sous-entités
        String stockSql = "SELECT se.nom, sse.quantite, sse.date_transfert, sse.derniere_mise_a_jour " +
                          "FROM StockSousEntite sse " +
                          "JOIN SousEntite se ON sse.id_sous_entite = se.id_sous_entite " +
                          "WHERE sse.id_produit = ?";
        PreparedStatement stockStmt = conn.prepareStatement(stockSql);
        stockStmt.setInt(1, product.getIdProduit());
        ResultSet stockRs = stockStmt.executeQuery();
        
        int row = 6;
        if (stockRs.next()) {
            grid.add(new Label("Stock dans les sous-entités:"), 0, row++);
            grid.add(new Label("Sous-entité"), 0, row);
            grid.add(new Label("Quantité"), 1, row);
            grid.add(new Label("Date transfert"), 2, row);
            grid.add(new Label("Dernière mise à jour"), 3, row);
            row++;
            
            do {
                grid.add(new Label(stockRs.getString("nom")), 0, row);
                grid.add(new Label(String.valueOf(stockRs.getInt("quantite"))), 1, row);
                grid.add(new Label(new SimpleDateFormat("dd/MM/yyyy HH:mm").format(stockRs.getTimestamp("date_transfert"))), 2, row);
                grid.add(new Label(new SimpleDateFormat("dd/MM/yyyy HH:mm").format(stockRs.getTimestamp("derniere_mise_a_jour"))), 3, row);
                row++;
            } while (stockRs.next());
        } else {
            grid.add(new Label("Aucun stock dans les sous-entités"), 0, row++);
        }
        
        // Ajouter le contenu au dialogue
        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(400);
        
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setContent(scrollPane);
        dialogPane.getButtonTypes().add(ButtonType.OK);
        
        dialog.showAndWait();
        
    } catch (SQLException e) {
        e.printStackTrace();
        showAlert("Erreur", "Impossible de charger les détails du produit: " + e.getMessage());
    }
}


}







/*


// TransfererController.java
// Ajoutez cette importation

public class TransfererController {
    // ... autres déclarations existantes ...

    // Ajoutez cette colonne

    @FXML
    private void initialize() {
        // ... configuration existante ...

        // Configurez les colonnes existantes
        refColumn.setCellValueFactory(new PropertyValueFactory<>("reference"));
        designationColumn.setCellValueFactory(new PropertyValueFactory<>("designation"));
        quantiteColumn.setCellValueFactory(new PropertyValueFactory<>("quantite"));

        // Ajoutez la colonne d'info
        colInfo.setCellFactory(param -> new TableCell<ProductStock, Void>() {
            private final Button infoButton = new Button("ℹ️");
            
            {
                infoButton.getStyleClass().add("info-button");
                infoButton.setOnAction(event -> {
                    ProductStock product = getTableView().getItems().get(getIndex());
                    showProductDetails(product);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : infoButton);
            }
        });
        
        // ... reste du code existant ...
    }

    // ... autres méthodes ...
}


















*/