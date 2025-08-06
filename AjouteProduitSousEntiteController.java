package com.votreport.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.*;
import java.util.Optional;
import javafx.scene.layout.StackPane;
import javafx.beans.property.StringProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.application.Platform;

import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;
import javafx.scene.layout.VBox;





public class AjouteProduitSousEntiteController {

    @FXML private TableView<ProductStock> stockPrincipalTable;
    @FXML private TableView<ProductStock> stockSousEntiteTable;
    



    
    private ObservableList<ProductStock> stockPrincipalData = FXCollections.observableArrayList();
    private ObservableList<ProductStock> stockSousEntiteData = FXCollections.observableArrayList();
    
    private int idSousEntite;
    private StockSousEntiteController parentController;

public void setIdSousEntite(int idSousEntite) {
    this.idSousEntite = idSousEntite;
    
    // Initialiser les TableView
    initializeTableColumns();
    
    // Charger les données
    loadStockPrincipal();
    loadStockSousEntite();
    setupDragAndDrop();
}
@FXML
public void initialize() {
    // Configuration des deux tables
    configureTable(stockPrincipalTable);
    configureTable(stockSousEntiteTable);
    
    // Chargement des données
    Platform.runLater(() -> {
        if (idSousEntite > 0) {
            loadStockPrincipal();
            loadStockSousEntite();
        }
    });
}

private void configureTable(TableView<ProductStock> table) {
    // 1. Configurer les colonnes
    TableColumn<ProductStock, String> colDesignation = new TableColumn<>("Produit");
    colDesignation.setCellValueFactory(cellData -> cellData.getValue().designationProperty());
    colDesignation.setPrefWidth(200);

    TableColumn<ProductStock, String> colReference = new TableColumn<>("Référence");
    colReference.setCellValueFactory(cellData -> cellData.getValue().referenceProperty());
    colReference.setPrefWidth(150);

    TableColumn<ProductStock, Number> colQuantite = new TableColumn<>("Quantité");
    colQuantite.setCellValueFactory(cellData -> cellData.getValue().quantiteProperty());
    colQuantite.setPrefWidth(100);

    // 2. Ajouter les colonnes au TableView
    table.getColumns().setAll(colDesignation, colReference, colQuantite);

    // 3. Configurer la taille
    table.setPrefSize(600, 400);
    table.setMinSize(300, 200);

    // 4. Configurer le style des lignes
    configureRowFactory(table);
}

private void configureRowFactory(TableView<ProductStock> table) {
    table.setRowFactory(tv -> new TableRow<ProductStock>() {
        @Override
        protected void updateItem(ProductStock item, boolean empty) {
            super.updateItem(item, empty);
            
            if (item == null || empty) {
                setStyle("");
            } else {
                // Style de base
                String baseStyle = getIndex() % 2 == 0 ? 
                    "-fx-background-color: #f5f5f5;" : 
                    "-fx-background-color: white;";
                
                // Style de sélection
                if (isSelected()) {
                    setStyle(baseStyle + " -fx-font-weight: bold; -fx-background-color: #e3f2fd;");
                } else {
                    setStyle(baseStyle);
                }
            }
        }
    });
}





private void configureTableColumns(TableView<ProductStock> table) {
    TableColumn<ProductStock, ?> col1 = table.getColumns().get(0);
    col1.setCellValueFactory(new PropertyValueFactory<>("designation"));
    
    TableColumn<ProductStock, ?> col2 = table.getColumns().get(1);
    col2.setCellValueFactory(new PropertyValueFactory<>("reference"));
    
    TableColumn<ProductStock, ?> col3 = table.getColumns().get(2);
    col3.setCellValueFactory(new PropertyValueFactory<>("quantite"));
}

private void initializeTableColumns() {
    // Configurer les colonnes pour stockPrincipalTable
    TableColumn<ProductStock, String> produitColPrincipal = new TableColumn<>("Produit");
    produitColPrincipal.setCellValueFactory(cellData -> cellData.getValue().designationProperty());
    
    TableColumn<ProductStock, String> referenceColPrincipal = new TableColumn<>("Référence");
    referenceColPrincipal.setCellValueFactory(cellData -> cellData.getValue().referenceProperty());
    
    TableColumn<ProductStock, Number> quantiteColPrincipal = new TableColumn<>("Quantité");
    quantiteColPrincipal.setCellValueFactory(cellData -> cellData.getValue().quantiteProperty());
    
    stockPrincipalTable.getColumns().setAll(produitColPrincipal, referenceColPrincipal, quantiteColPrincipal);
    
    // Configurer les colonnes pour stockSousEntiteTable
    TableColumn<ProductStock, String> produitColSousEntite = new TableColumn<>("Produit");
    produitColSousEntite.setCellValueFactory(cellData -> cellData.getValue().designationProperty());
    
    TableColumn<ProductStock, String> referenceColSousEntite = new TableColumn<>("Référence");
    referenceColSousEntite.setCellValueFactory(cellData -> cellData.getValue().referenceProperty());
    
    TableColumn<ProductStock, Number> quantiteColSousEntite = new TableColumn<>("Quantité");
    quantiteColSousEntite.setCellValueFactory(cellData -> cellData.getValue().quantiteProperty());
    
    stockSousEntiteTable.getColumns().setAll(produitColSousEntite, referenceColSousEntite, quantiteColSousEntite);
    
    // Lier les données
    stockPrincipalTable.setItems(stockPrincipalData);
    stockSousEntiteTable.setItems(stockSousEntiteData);
}

    public void setParentController(StockSousEntiteController parentController) {
        this.parentController = parentController;
    }

private void loadStockPrincipal() {
    stockPrincipalData.clear();
    String sql = "SELECT p.id_produit, p.reference, p.designation, SUM(sp.quantite) as quantite " +
                 "FROM Produit p " +
                 "JOIN StockPrincipal sp ON p.id_produit = sp.id_produit " +
                 "WHERE sp.statut = 'en_stock' " +
                 "GROUP BY p.id_produit, p.reference, p.designation " +
                 "HAVING quantite > 0";
    
    try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/fskn", "root", "");
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
        
        while (rs.next()) {
            ProductStock ps = new ProductStock(
                rs.getInt("id_produit"),
                rs.getString("reference"),
                rs.getString("designation"),
                rs.getInt("quantite")
            );
            stockPrincipalData.add(ps);
            System.out.println("Ajouté: " + ps.getDesignation() + " - " + ps.getQuantite()); // Log de débogage
        }
        
        // FORCEZ le rafraîchissement du TableView
        Platform.runLater(() -> {
            stockPrincipalTable.setItems(stockPrincipalData);
            stockPrincipalTable.refresh();
            System.out.println("Nombre d'éléments dans TableView: " + stockPrincipalTable.getItems().size()); // Vérification
        });
        
    } catch (SQLException e) {
        e.printStackTrace();
        showAlert("Erreur", "Erreur lors du chargement du stock principal: " + e.getMessage());
    }
    
}

    private void loadStockSousEntite() {
        stockSousEntiteData.clear();
        String sql = "SELECT s.id_stock_sous_entite, p.id_produit, p.reference, p.designation, s.quantite " +
                     "FROM StockSousEntite s " +
                     "JOIN Produit p ON s.id_produit = p.id_produit " +
                     "WHERE s.id_sous_entite = ?";
        
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/fskn", "root", "");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, idSousEntite);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                stockSousEntiteData.add(new ProductStock(
                    rs.getInt("id_produit"),
                    rs.getString("reference"),
                    rs.getString("designation"),
                    rs.getInt("quantite")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        stockSousEntiteTable.setItems(stockSousEntiteData);
        stockSousEntiteTable.refresh();
    }

    private void setupDragAndDrop() {
        // Drag from principal table
        stockPrincipalTable.setOnDragDetected(event -> {
            ProductStock selected = stockPrincipalTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Dragboard db = stockPrincipalTable.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.putString(String.valueOf(selected.getIdProduit()));
                db.setContent(content);
                event.consume();
            }
        });

        // Drop on sous-entité table
        stockSousEntiteTable.setOnDragOver(event -> {
            if (event.getGestureSource() != stockSousEntiteTable && 
                event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        // Handle drop
        stockSousEntiteTable.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            
            if (db.hasString()) {
                int idProduit = Integer.parseInt(db.getString());
                ProductStock produit = findProductById(idProduit, stockPrincipalData);
                
                if (produit != null) {
                    Optional<Integer> quantite = askForQuantity(produit.getQuantite());
                    
                    if (quantite.isPresent() && quantite.get() > 0) {
                        transfererProduit(produit, quantite.get());
                        success = true;
                    }
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private ProductStock findProductById(int idProduit, ObservableList<ProductStock> list) {
        for (ProductStock p : list) {
            if (p.getIdProduit() == idProduit) {
                return p;
            }
        }
        return null;
    }

    private Optional<Integer> askForQuantity(int max) {
        TextInputDialog dialog = new TextInputDialog("1");
        dialog.setTitle("Quantité à transférer");
        dialog.setHeaderText("Quantité disponible: " + max);
        dialog.setContentText("Entrez la quantité:");

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

    private void transfererProduit(ProductStock produit, int quantite) {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/fskn", "root", "")) {
            conn.setAutoCommit(false);
            
            // 1. Update principal stock
            String updatePrincipal = "UPDATE StockPrincipal SET quantite = quantite - ? WHERE id_produit = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updatePrincipal)) {
                stmt.setInt(1, quantite);
                stmt.setInt(2, produit.getIdProduit());
                stmt.executeUpdate();
            }
            
            // 2. Update or create sub-entity stock
            String checkStock = "SELECT * FROM StockSousEntite WHERE id_produit = ? AND id_sous_entite = ?";
            try (PreparedStatement stmt = conn.prepareStatement(checkStock)) {
                stmt.setInt(1, produit.getIdProduit());
                stmt.setInt(2, idSousEntite);
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    // Update existing stock
                    String updateStock = "UPDATE StockSousEntite SET quantite = quantite + ? WHERE id_stock_sous_entite = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateStock)) {
                        updateStmt.setInt(1, quantite);
                        updateStmt.setInt(2, rs.getInt("id_stock_sous_entite"));
                        updateStmt.executeUpdate();
                    }
                } else {
                    // Create new stock entry
                    String insertStock = "INSERT INTO StockSousEntite (id_produit, id_sous_entite, quantite, date_transfert) VALUES (?, ?, ?, NOW())";
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertStock)) {
                        insertStmt.setInt(1, produit.getIdProduit());
                        insertStmt.setInt(2, idSousEntite);
                        insertStmt.setInt(3, quantite);
                        insertStmt.executeUpdate();
                    }
                }
            }
            
            // 3. Record transfer
            String insertTransfer = "INSERT INTO Transfert (id_produit, id_sous_entite_destination, quantite, date_transfert) VALUES (?, ?, ?, NOW())";
            try (PreparedStatement stmt = conn.prepareStatement(insertTransfer)) {
                stmt.setInt(1, produit.getIdProduit());
                stmt.setInt(2, idSousEntite);
                stmt.setInt(3, quantite);
                stmt.executeUpdate();
            }
            
            conn.commit();
            
            // Refresh data
            loadStockPrincipal();
            loadStockSousEntite();
            
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de transférer le produit: " + e.getMessage());
        }
    }

    @FXML
    private void handleAnnuler() {
        stockSousEntiteTable.getScene().getWindow().hide();
    }

    @FXML
    private void handleValider() {
        if (parentController != null) {
            parentController.loadStock();
        }
        stockSousEntiteTable.getScene().getWindow().hide();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

public static class ProductStock {
    private final SimpleIntegerProperty idProduit;
    private final SimpleStringProperty reference;
    private final SimpleStringProperty designation;
    private final SimpleIntegerProperty quantite;

    public ProductStock(int idProduit, String reference, String designation, int quantite) {
        this.idProduit = new SimpleIntegerProperty(idProduit);
        this.reference = new SimpleStringProperty(reference);
        this.designation = new SimpleStringProperty(designation);
        this.quantite = new SimpleIntegerProperty(quantite);
    }

    // Getters standards
    public int getIdProduit() { return idProduit.get(); }
    public String getReference() { return reference.get(); }
    public String getDesignation() { return designation.get(); }
    public int getQuantite() { return quantite.get(); }

    // Property getters
    public IntegerProperty idProduitProperty() { return idProduit; }
    public StringProperty referenceProperty() { return reference; }
    public StringProperty designationProperty() { return designation; }
    public IntegerProperty quantiteProperty() { return quantite; }
}


private void testAvecDonneesFactices() {
    stockPrincipalData.add(new ProductStock(1, "REF001", "Produit Test 1", 10));
    stockPrincipalData.add(new ProductStock(2, "REF002", "Produit Test 2", 5));
    stockSousEntiteData.add(new ProductStock(3, "REF003", "Produit Test 3", 8));
    
    System.out.println("Données factices ajoutées");
    stockPrincipalTable.refresh();
    stockSousEntiteTable.refresh();
}
    
private void initializeTableView() {
    // 1. Configurer les colonnes
    TableColumn<ProductStock, String> colDesignation = new TableColumn<>("Produit");
    colDesignation.setCellValueFactory(cellData -> cellData.getValue().designationProperty());
    colDesignation.setPrefWidth(200);

    TableColumn<ProductStock, String> colReference = new TableColumn<>("Référence");
    colReference.setCellValueFactory(cellData -> cellData.getValue().referenceProperty());
    colReference.setPrefWidth(150);

    TableColumn<ProductStock, Number> colQuantite = new TableColumn<>("Quantité");
    colQuantite.setCellValueFactory(cellData -> cellData.getValue().quantiteProperty());
    colQuantite.setPrefWidth(100);

    // 2. Ajouter les colonnes au TableView
    stockPrincipalTable.getColumns().setAll(colDesignation, colReference, colQuantite);

    // 3. Configurer la taille
    stockPrincipalTable.setPrefSize(600, 400);
    stockPrincipalTable.setMinSize(300, 200);

    // 4. Lier les données
    stockPrincipalTable.setItems(stockPrincipalData);

    // 5. Forcer le rendu
    Platform.runLater(() -> {
        stockPrincipalTable.requestLayout();
        System.out.println("Nouvelles dimensions: " + 
                         stockPrincipalTable.getWidth() + "x" + stockPrincipalTable.getHeight());
    });
}

}



/////////////////   loadStockPrincipal   loadStockPrincipal

/*

private void loadStockSousEntite() {
    stockSousEntiteData.clear();
    // ... votre code existant ...
    
    // Après avoir chargé les données
    stockSousEntiteTable.setItems(stockSousEntiteData);
    stockSousEntiteTable.refresh();
}


private void loadStockPrincipal() {
    stockPrincipalData.clear();
    // ... votre code existant ...
    
    // Après avoir chargé les données
    stockPrincipalTable.setItems(stockPrincipalData);
    stockPrincipalTable.refresh();
}





@FXML
public void initialize() {
    // Configuration initiale
    configureTable(stockPrincipalTable);
    configureTable(stockSousEntiteTable);
    
    // Debug visuel
    Platform.runLater(() -> {
        System.out.println("Dimensions actuelles:");
        System.out.println("TableView: " + stockPrincipalTable.getWidth() + "x" + stockPrincipalTable.getHeight());
        System.out.println("Conteneur parent: " + stockPrincipalTable.getParent().getBoundsInLocal());
    });
}

private void configureTable(TableView<ProductStock> table) {
    // Taille fixe des cellules
    table.setFixedCellSize(35);
    
    // Ajustement automatique de la hauteur
    table.prefHeightProperty().bind(
        table.fixedCellSizeProperty().multiply(Bindings.size(table.getItems()).add(2))
    );
    
    // Configuration des colonnes
    table.getColumns().forEach(col -> {
        if (col.getText().equals("Produit")) {
            col.setCellValueFactory(cellData -> cellData.getValue().designationProperty());
        } else if (col.getText().equals("Référence")) {
            col.setCellValueFactory(cellData -> cellData.getValue().referenceProperty());
        } else if (col.getText().equals("Quantité")) {
            col.setCellValueFactory(cellData -> cellData.getValue().quantiteProperty());
        }
    });
}











@FXML
public void initialize() {
    // Configuration explicite des colonnes
    configureTableColumns(stockPrincipalTable);
    configureTableColumns(stockSousEntiteTable);
    
    // Test visuel temporaire
    Platform.runLater(() -> {
        stockPrincipalTable.setStyle("-fx-border-color: red; -fx-border-width: 2;");
        System.out.println("TableView visible: " + stockPrincipalTable.isVisible());
        System.out.println("TableView dimensions: " + stockPrincipalTable.getWidth() + "x" + stockPrincipalTable.getHeight());
    });
}

private void configureTableColumns(TableView<ProductStock> table) {
    TableColumn<ProductStock, ?> col1 = table.getColumns().get(0);
    col1.setCellValueFactory(new PropertyValueFactory<>("designation"));
    
    TableColumn<ProductStock, ?> col2 = table.getColumns().get(1);
    col2.setCellValueFactory(new PropertyValueFactory<>("reference"));
    
    TableColumn<ProductStock, ?> col3 = table.getColumns().get(2);
    col3.setCellValueFactory(new PropertyValueFactory<>("quantite"));
}



public static class ProductStock {
    private final SimpleIntegerProperty idProduit;
    private final SimpleStringProperty reference;
    private final SimpleStringProperty designation;
    private final SimpleIntegerProperty quantite;

    public ProductStock(int idProduit, String reference, String designation, int quantite) {
        this.idProduit = new SimpleIntegerProperty(idProduit);
        this.reference = new SimpleStringProperty(reference);
        this.designation = new SimpleStringProperty(designation);
        this.quantite = new SimpleIntegerProperty(quantite);
    }

    // Getters standards
    public int getIdProduit() { return idProduit.get(); }
    public String getReference() { return reference.get(); }
    public String getDesignation() { return designation.get(); }
    public int getQuantite() { return quantite.get(); }

    // Property getters
    public IntegerProperty idProduitProperty() { return idProduit; }
    public StringProperty referenceProperty() { return reference; }
    public StringProperty designationProperty() { return designation; }
    public IntegerProperty quantiteProperty() { return quantite; }
}

@FXML
public void initialize() {
    // Configurez explicitement les colonnes
    TableColumn<ProductStock, String> produitCol = (TableColumn<ProductStock, String>) stockPrincipalTable.getColumns().get(0);
    produitCol.setCellValueFactory(cellData -> cellData.getValue().designationProperty());
    
    TableColumn<ProductStock, String> referenceCol = (TableColumn<ProductStock, String>) stockPrincipalTable.getColumns().get(1);
    referenceCol.setCellValueFactory(cellData -> cellData.getValue().referenceProperty());
    
    TableColumn<ProductStock, Number> quantiteCol = (TableColumn<ProductStock, Number>) stockPrincipalTable.getColumns().get(2);
    quantiteCol.setCellValueFactory(cellData -> cellData.getValue().quantiteProperty());
    
    // Faites de même pour stockSousEntiteTable
    TableColumn<ProductStock, String> produitColSE = (TableColumn<ProductStock, String>) stockSousEntiteTable.getColumns().get(0);
    produitColSE.setCellValueFactory(cellData -> cellData.getValue().designationProperty());
    
    TableColumn<ProductStock, String> referenceColSE = (TableColumn<ProductStock, String>) stockSousEntiteTable.getColumns().get(1);
    referenceColSE.setCellValueFactory(cellData -> cellData.getValue().referenceProperty());
    
    TableColumn<ProductStock, Number> quantiteColSE = (TableColumn<ProductStock, Number>) stockSousEntiteTable.getColumns().get(2);
    quantiteColSE.setCellValueFactory(cellData -> cellData.getValue().quantiteProperty());
    
    System.out.println("Initialisation des colonnes terminée");
}








private void loadStockPrincipal() {
    stockPrincipalData.clear();
    String sql = "SELECT p.id_produit, p.reference, p.designation, SUM(sp.quantite) as quantite " +
                 "FROM Produit p " +
                 "JOIN StockPrincipal sp ON p.id_produit = sp.id_produit " +
                 "WHERE sp.statut = 'en_stock' " +
                 "GROUP BY p.id_produit, p.reference, p.designation " +
                 "HAVING quantite > 0";
    
    try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/fskn", "root", "");
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
        
        while (rs.next()) {
            ProductStock ps = new ProductStock(
                rs.getInt("id_produit"),
                rs.getString("reference"),
                rs.getString("designation"),
                rs.getInt("quantite")
            );
            stockPrincipalData.add(ps);
            System.out.println("Ajouté: " + ps.getDesignation() + " - " + ps.getQuantite()); // Log de débogage
        }
        
        // FORCEZ le rafraîchissement du TableView
        Platform.runLater(() -> {
            stockPrincipalTable.setItems(stockPrincipalData);
            stockPrincipalTable.refresh();
            System.out.println("Nombre d'éléments dans TableView: " + stockPrincipalTable.getItems().size()); // Vérification
        });
        
    } catch (SQLException e) {
        e.printStackTrace();
        showAlert("Erreur", "Erreur lors du chargement du stock principal: " + e.getMessage());
    }
}

enregistrements 

private void loadStockPrincipal() {
    stockPrincipalData.clear();
    String sql = "SELECT p.id_produit, p.reference, p.designation, SUM(sp.quantite) as quantite " +
                 "FROM Produit p " +
                 "JOIN StockPrincipal sp ON p.id_produit = sp.id_produit " +
                 "WHERE sp.statut = 'en_stock' " +
                 "GROUP BY p.id_produit, p.reference, p.designation " +
                 "HAVING quantite > 0";
    
    System.out.println("Exécution de la requête SQL: " + sql); // Log
    
    try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/fskn", "root", "");
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
        
        int count = 0;
        while (rs.next()) {
            count++;
            stockPrincipalData.add(new ProductStock(
                rs.getInt("id_produit"),
                rs.getString("reference"),
                rs.getString("designation"),
                rs.getInt("quantite")
            ));
        }
        
        System.out.println(count + " enregistrements chargés dans stockPrincipalData"); // Log
        stockPrincipalTable.refresh();
        
    } catch (SQLException e) {
        e.printStackTrace();
        showAlert("Erreur", "Erreur lors du chargement du stock principal: " + e.getMessage());
    }
}
public void setIdSousEntite(int idSousEntite) {
    this.idSousEntite = idSousEntite;
    
    // Initialiser les TableView
    initializeTableColumns();
    
    // Charger les données
    loadStockPrincipal();
    loadStockSousEntite();
    setupDragAndDrop();
}

private void initializeTableColumns() {
    // Configurer les colonnes pour stockPrincipalTable
    TableColumn<ProductStock, String> produitColPrincipal = new TableColumn<>("Produit");
    produitColPrincipal.setCellValueFactory(cellData -> cellData.getValue().designationProperty());
    
    TableColumn<ProductStock, String> referenceColPrincipal = new TableColumn<>("Référence");
    referenceColPrincipal.setCellValueFactory(cellData -> cellData.getValue().referenceProperty());
    
    TableColumn<ProductStock, Number> quantiteColPrincipal = new TableColumn<>("Quantité");
    quantiteColPrincipal.setCellValueFactory(cellData -> cellData.getValue().quantiteProperty());
    
    stockPrincipalTable.getColumns().setAll(produitColPrincipal, referenceColPrincipal, quantiteColPrincipal);
    
    // Configurer les colonnes pour stockSousEntiteTable
    TableColumn<ProductStock, String> produitColSousEntite = new TableColumn<>("Produit");
    produitColSousEntite.setCellValueFactory(cellData -> cellData.getValue().designationProperty());
    
    TableColumn<ProductStock, String> referenceColSousEntite = new TableColumn<>("Référence");
    referenceColSousEntite.setCellValueFactory(cellData -> cellData.getValue().referenceProperty());
    
    TableColumn<ProductStock, Number> quantiteColSousEntite = new TableColumn<>("Quantité");
    quantiteColSousEntite.setCellValueFactory(cellData -> cellData.getValue().quantiteProperty());
    
    stockSousEntiteTable.getColumns().setAll(produitColSousEntite, referenceColSousEntite, quantiteColSousEntite);
    
    // Lier les données
    stockPrincipalTable.setItems(stockPrincipalData);
    stockSousEntiteTable.setItems(stockSousEntiteData);
}
*/


