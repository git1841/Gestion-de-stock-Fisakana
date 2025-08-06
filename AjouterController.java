package com.votreport.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.converter.DoubleStringConverter;
import java.sql.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import java.util.Optional;
import javafx.scene.control.TableCell; // Import ajouté
import javafx.scene.control.TableRow; // Import ajouté
import javafx.scene.layout.HBox;
import javafx.scene.control.TextInputDialog;


public class AjouterController {

    @FXML private TableView<Product> productsTable;
    
    @FXML private TableColumn<Product, String> colReference;
    @FXML private TableColumn<Product, String> colDesignation;
    @FXML private TableColumn<Product, String> colDescription;
    @FXML private TableColumn<Product, Double> colPrixAchat;
    @FXML private TableColumn<Product, Double> colPrixVente;
    @FXML private TableColumn<Product, Void> colActions; // Déclaration corrigée

    
    private ObservableList<Product> productsData = FXCollections.observableArrayList();
    private Controller controller;
    private Product newProductRow = new Product(-1, "", "", "", 0.0, 0.0);

    @FXML
    public void initialize() {
        // Configuration des colonnes
        configureColumn(colReference, "reference");
        configureColumn(colDesignation, "designation");
        configureColumn(colDescription, "description");
        configureColumn(colPrixAchat, "prixAchat", new DoubleStringConverter());
        configureColumn(colPrixVente, "prixVente", new DoubleStringConverter());
        
        // Configuration de la colonne Actions
        colActions.setCellFactory(param -> new TableCell<Product, Void>() {
    private final Button deleteButton = new Button("✕");
    private final Button stockButton = new Button("Stock");
    private final HBox buttonsContainer = new HBox(5, deleteButton, stockButton);

    {
        // Style et action du bouton Supprimer
        deleteButton.getStyleClass().add("delete-button");
        deleteButton.setOnAction(event -> {
            Product product = getTableView().getItems().get(getIndex());
            deleteProduct(product);
        });

        // Style et action du bouton Stock
        stockButton.getStyleClass().add("stock-button");
        stockButton.setOnAction(event -> {
            Product product = getTableView().getItems().get(getIndex());
            addToStock(product);
        });
    }

    @Override
    protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        setGraphic(empty ? null : buttonsContainer);
    }
});
        
        productsTable.setItems(productsData);
        productsTable.setEditable(true);
        
        // Ajouter la ligne d'ajout à la fin
        productsData.add(newProductRow);
        
        // Écouter les changements de focus
        productsTable.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) { // Quand la table perd le focus
                checkAndSaveNewProduct();
            }
        });
        
        // Écouter les changements de sélection
        productsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (oldSelection != null && oldSelection == newProductRow) {
                checkAndSaveNewProduct();
            }
        });
        
        // Écouter la touche Entrée
        productsTable.setOnKeyPressed(this::handleKeyPressed);
        
        loadProducts();
    }

    private void handleKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            Product selectedProduct = productsTable.getSelectionModel().getSelectedItem();
            if (selectedProduct != null) {
                saveOrUpdateProduct(selectedProduct);
            }
        }
    }

    private void configureColumn(TableColumn<Product, String> column, String property) {
        column.setCellFactory(TextFieldTableCell.forTableColumn());
        column.setOnEditCommit(event -> {
            Product product = event.getRowValue();
            if ("reference".equals(property)) {
                product.setReference(event.getNewValue());
            } else if ("designation".equals(property)) {
                product.setDesignation(event.getNewValue());
            } else if ("description".equals(property)) {
                product.setDescription(event.getNewValue());
            }
            saveOrUpdateProduct(product);
        });
    }

    private void configureColumn(TableColumn<Product, Double> column, String property, DoubleStringConverter converter) {
        column.setCellFactory(TextFieldTableCell.forTableColumn(converter));
        column.setOnEditCommit(event -> {
            Product product = event.getRowValue();
            if ("prixAchat".equals(property)) {
                product.setPrixAchat(event.getNewValue());
            } else if ("prixVente".equals(property)) {
                product.setPrixVente(event.getNewValue());
            }
            saveOrUpdateProduct(product);
        });
    }

    private void saveOrUpdateProduct(Product product) {
        if (product.getId() == -1) {
            // C'est la ligne d'ajout
            if (isProductValid(product)) {
                saveNewProduct(product);
            }
        } else {
            // C'est un produit existant
            updateProductInDatabase(product);
        }
    }

    private void checkAndSaveNewProduct() {
        if (isProductValid(newProductRow)) {
            saveNewProduct(newProductRow);
        }
    }

    private boolean isProductValid(Product product) {
        return product.getReference() != null && !product.getReference().isEmpty() && 
               product.getDesignation() != null && !product.getDesignation().isEmpty() &&
               product.getPrixAchat() > 0 && 
               product.getPrixVente() > 0;
    }

    private void deleteProduct(Product product) {
        if (product.getId() == -1) {
            // C'est la ligne d'ajout, on la réinitialise
            newProductRow.setReference("");
            newProductRow.setDesignation("");
            newProductRow.setDescription("");
            newProductRow.setPrixAchat(0.0);
            newProductRow.setPrixVente(0.0);
        } else {
            // Demander confirmation
            Alert alert = new Alert(AlertType.CONFIRMATION);
            alert.setTitle("Confirmer la suppression");
            alert.setHeaderText("Supprimer le produit : " + product.getReference());
            alert.setContentText("Êtes-vous sûr de vouloir supprimer ce produit définitivement?");
            
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/fskn", "root", "");
                     PreparedStatement pstmt = conn.prepareStatement(
                         "DELETE FROM Produit WHERE id_produit = ?")) {
                    
                    pstmt.setInt(1, product.getId());
                    int rowsDeleted = pstmt.executeUpdate();
                    
                    if (rowsDeleted > 0) {
                        productsData.remove(product);
                        if (controller != null) {
                            controller.refreshStock();
                        }
                    }
                } catch (SQLException e) {
                    System.err.println("Erreur lors de la suppression du produit:");
                    e.printStackTrace();
                }
            }
        }
    }

    private void saveNewProduct(Product product) {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/fskn", "root", "");
             PreparedStatement pstmt = conn.prepareStatement(
                 "INSERT INTO Produit (reference, designation, description, prix_achat, prix_vente) VALUES (?, ?, ?, ?, ?)")) {
            
            pstmt.setString(1, product.getReference());
            pstmt.setString(2, product.getDesignation());
            pstmt.setString(3, product.getDescription());
            pstmt.setDouble(4, product.getPrixAchat());
            pstmt.setDouble(5, product.getPrixVente());
            pstmt.executeUpdate();
            
            // Réinitialiser la ligne d'ajout
            newProductRow.setReference("");
            newProductRow.setDesignation("");
            newProductRow.setDescription("");
            newProductRow.setPrixAchat(0.0);
            newProductRow.setPrixVente(0.0);
            
            // Recharger les produits
            loadProducts();
            
            if (controller != null) {
                controller.refreshStock();
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout du produit:");
            e.printStackTrace();
        }
    }

    private void updateProductInDatabase(Product product) {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/fskn", "root", "");
             PreparedStatement pstmt = conn.prepareStatement(
                 "UPDATE Produit SET reference = ?, designation = ?, description = ?, prix_achat = ?, prix_vente = ? WHERE id_produit = ?")) {
            
            pstmt.setString(1, product.getReference());
            pstmt.setString(2, product.getDesignation());
            pstmt.setString(3, product.getDescription());
            pstmt.setDouble(4, product.getPrixAchat());
            pstmt.setDouble(5, product.getPrixVente());
            pstmt.setInt(6, product.getId());
            
            int rowsUpdated = pstmt.executeUpdate();
            
            System.out.println("Produit mis à jour: " + product.getReference() + 
                               ", ID: " + product.getId() + 
                               ", lignes affectées: " + rowsUpdated);
            
            if (controller != null) {
                controller.refreshStock();
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour du produit " + product.getReference() + ":");
            e.printStackTrace();
        }
    }

    private void loadProducts() {
        ObservableList<Product> existingProducts = FXCollections.observableArrayList();
        
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/fskn", "root", "");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM Produit ORDER BY reference")) {
            
            while (rs.next()) {
                existingProducts.add(new Product(
                    rs.getInt("id_produit"),
                    rs.getString("reference"),
                    rs.getString("designation"),
                    rs.getString("description"),
                    rs.getDouble("prix_achat"),
                    rs.getDouble("prix_vente")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du chargement des produits:");
            e.printStackTrace();
        }
        
        // Mettre à jour la liste avec les produits existants + la ligne d'ajout
        productsData.setAll(existingProducts);
        productsData.add(newProductRow);
    }

    public void setController(Controller controller) {
        this.controller = controller;
    }
    
    public static class Product {
        private int id;
        private String reference;
        private String designation;
        private String description;
        private double prixAchat;
        private double prixVente;

        public Product(int id, String reference, String designation, String description, double prixAchat, double prixVente) {
            this.id = id;
            this.reference = reference;
            this.designation = designation;
            this.description = description;
            this.prixAchat = prixAchat;
            this.prixVente = prixVente;
        }

        public int getId() { return id; }
        public String getReference() { return reference; }
        public String getDesignation() { return designation; }
        public String getDescription() { return description; }
        public double getPrixAchat() { return prixAchat; }
        public double getPrixVente() { return prixVente; }

        public void setId(int id) { this.id = id; }
        public void setReference(String reference) { this.reference = reference; }
        public void setDesignation(String designation) { this.designation = designation; }
        public void setDescription(String description) { this.description = description; }
        public void setPrixAchat(double prixAchat) { this.prixAchat = prixAchat; }
        public void setPrixVente(double prixVente) { this.prixVente = prixVente; }
        
        @Override
        public String toString() {
            return reference + " - " + designation + " (" + prixAchat + "/" + prixVente + ")";
        }
    }

private void addToStock(Product product) {
    if (product.getId() == -1) {
        showAlert("Erreur", "Veuillez d'abord enregistrer le produit avant de l'ajouter au stock.");
        return;
    }

    TextInputDialog dialog = new TextInputDialog("1");
    dialog.setTitle("Ajout au stock principal");
    dialog.setHeaderText("Produit: " + product.getReference());
    dialog.setContentText("Quantité à ajouter:");

    Optional<String> result = dialog.showAndWait();
    result.ifPresent(quantityStr -> {
        try {
            int quantity = Integer.parseInt(quantityStr);
            if (quantity <= 0) throw new NumberFormatException();

            try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/fskn", "root", "");
                 PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO StockPrincipal (id_produit, quantite, date_arrivee, statut) " +
                     "VALUES (?, ?, CURRENT_TIMESTAMP, 'en_stock')")) {

                pstmt.setInt(1, product.getId());
                pstmt.setInt(2, quantity);
                pstmt.executeUpdate();

                showAlert("Succès", quantity + " unité(s) ajoutée(s) au stock principal!");
                
            } catch (SQLException e) {
                showAlert("Erreur", "Échec de l'ajout au stock: " + e.getMessage());
            }
        } catch (NumberFormatException e) {
            showAlert("Erreur", "Veuillez entrer une quantité valide (nombre entier positif)");
        }
    });
}

private void showAlert(String title, String message) {
    Alert alert = new Alert(AlertType.INFORMATION);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
}



}










/*

colActions.setCellFactory(param -> new TableCell<Product, Void>() {
    private final Button deleteButton = new Button("✕");
    private final Button stockButton = new Button("Stock");
    private final HBox buttonsContainer = new HBox(5, deleteButton, stockButton);

    {
        // Style et action du bouton Supprimer
        deleteButton.getStyleClass().add("delete-button");
        deleteButton.setOnAction(event -> {
            Product product = getTableView().getItems().get(getIndex());
            deleteProduct(product);
        });

        // Style et action du bouton Stock
        stockButton.getStyleClass().add("stock-button");
        stockButton.setOnAction(event -> {
            Product product = getTableView().getItems().get(getIndex());
            addToStock(product);
        });
    }

    @Override
    protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        setGraphic(empty ? null : buttonsContainer);
    }
});













*/