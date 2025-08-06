/*
package com.votreport.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.*;
import java.sql.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.converter.IntegerStringConverter;
import javafx.scene.layout.HBox;


public class StockSousEntiteController {

    @FXML private TableView<StockProduit> stockTable;
    @FXML private TableColumn<StockProduit, String> colProduit;
    @FXML private TableColumn<StockProduit, String> colReference;
    @FXML private TableColumn<StockProduit, Integer> colQuantite;
    @FXML private TableColumn<StockProduit, Void> colActions;

    private ObservableList<StockProduit> stockData = FXCollections.observableArrayList();
    private StockProduit newStockRow = new StockProduit(-1, -1, "", "", 0);
    private int idSousEntite;
    private Controller mainController;

    public void setIdSousEntite(int idSousEntite) {
        this.idSousEntite = idSousEntite;
        loadStock();
    }

    public void setMainController(Controller controller) {
        this.mainController = controller;
    }

    @FXML
    public void initialize() {
        // Configuration des colonnes
        colProduit.setCellValueFactory(cellData -> cellData.getValue().designationProperty());
        colReference.setCellValueFactory(cellData -> cellData.getValue().referenceProperty());
        colQuantite.setCellValueFactory(cellData -> cellData.getValue().quantiteProperty().asObject());
        
        // Rendre la colonne quantité éditable
        colQuantite.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        colQuantite.setOnEditCommit(event -> {
            StockProduit stock = event.getRowValue();
            stock.setQuantite(event.getNewValue());
            saveOrUpdateStock(stock);
        });
        
        // Colonne Actions
        colActions.setCellFactory(param -> new TableCell<StockProduit, Void>() {
            private final Button btnDelete = new Button("✕");
            
            {
                btnDelete.getStyleClass().add("delete-button");
                btnDelete.setOnAction(event -> {
                    StockProduit stock = getTableView().getItems().get(getIndex());
                    deleteStock(stock);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnDelete);
            }
        });

        stockTable.setItems(stockData);
        stockTable.setEditable(true);
        stockData.add(newStockRow);
        stockTable.setOnKeyPressed(this::handleKeyPressed);
    }

    private void handleKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            StockProduit selected = stockTable.getSelectionModel().getSelectedItem();
            if (selected != null) saveOrUpdateStock(selected);
        }
    }

    private void loadStock() {
        stockData.clear();
        String sql = "SELECT s.id_stock_sous_entite, p.id_produit, p.designation, p.reference, s.quantite " +
                     "FROM StockSousEntite s " +
                     "JOIN Produit p ON s.id_produit = p.id_produit " +
                     "WHERE s.id_sous_entite = ?";
        
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/fskn", "root", "");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, idSousEntite);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                stockData.add(new StockProduit(
                    rs.getInt("id_stock_sous_entite"),
                    rs.getInt("id_produit"),
                    rs.getString("designation"),
                    rs.getString("reference"),
                    rs.getInt("quantite")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        stockData.add(newStockRow);
    }

    private void saveOrUpdateStock(StockProduit stock) {
        if (stock.getIdStock() == -1) {
            if (isStockValid(stock)) saveNewStock(stock);
        } else {
            updateStockInDatabase(stock);
        }
    }

    private boolean isStockValid(StockProduit stock) {
        return stock.getIdProduit() > 0 && stock.getQuantite() > 0;
    }

    private void saveNewStock(StockProduit stock) {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/fskn", "root", "");
             PreparedStatement pstmt = conn.prepareStatement(
                 "INSERT INTO StockSousEntite (id_produit, id_sous_entite, quantite) VALUES (?, ?, ?)")) {
            
            pstmt.setInt(1, stock.getIdProduit());
            pstmt.setInt(2, idSousEntite);
            pstmt.setInt(3, stock.getQuantite());
            pstmt.executeUpdate();
            
            // Réinitialiser la ligne d'ajout
            newStockRow.setIdProduit(-1);
            newStockRow.setDesignation("");
            newStockRow.setReference("");
            newStockRow.setQuantite(0);
            
            loadStock();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ajouter le produit au stock");
        }
    }

    private void updateStockInDatabase(StockProduit stock) {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/fskn", "root", "");
             PreparedStatement pstmt = conn.prepareStatement(
                 "UPDATE StockSousEntite SET quantite = ? WHERE id_stock_sous_entite = ?")) {
            
            pstmt.setInt(1, stock.getQuantite());
            pstmt.setInt(2, stock.getIdStock());
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de mettre à jour la quantité");
        }
    }

    private void deleteStock(StockProduit stock) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmer la suppression");
        alert.setHeaderText("Supprimer : " + stock.getDesignation());
        alert.setContentText("Êtes-vous sûr de vouloir retirer ce produit du stock?");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/fskn", "root", "");
                     PreparedStatement pstmt = conn.prepareStatement(
                         "DELETE FROM StockSousEntite WHERE id_stock_sous_entite = ?")) {
                    
                    pstmt.setInt(1, stock.getIdStock());
                    pstmt.executeUpdate();
                    loadStock();
                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert("Erreur", "Impossible de supprimer le produit du stock");
                }
            }
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static class StockProduit {
        private final IntegerProperty idStock = new SimpleIntegerProperty();
        private final IntegerProperty idProduit = new SimpleIntegerProperty();
        private final StringProperty designation = new SimpleStringProperty();
        private final StringProperty reference = new SimpleStringProperty();
        private final IntegerProperty quantite = new SimpleIntegerProperty();

        public StockProduit(int idStock, int idProduit, String designation, String reference, int quantite) {
            this.idStock.set(idStock);
            this.idProduit.set(idProduit);
            this.designation.set(designation);
            this.reference.set(reference);
            this.quantite.set(quantite);
        }

        // Getters
        public int getIdStock() { return idStock.get(); }
        public int getIdProduit() { return idProduit.get(); }
        public String getDesignation() { return designation.get(); }
        public String getReference() { return reference.get(); }
        public int getQuantite() { return quantite.get(); }

        // Setters
        public void setIdStock(int idStock) { this.idStock.set(idStock); }
        public void setIdProduit(int idProduit) { this.idProduit.set(idProduit); }
        public void setDesignation(String designation) { this.designation.set(designation); }
        public void setReference(String reference) { this.reference.set(reference); }
        public void setQuantite(int quantite) { this.quantite.set(quantite); }

        // Property getters
        public IntegerProperty idStockProperty() { return idStock; }
        public IntegerProperty idProduitProperty() { return idProduit; }
        public StringProperty designationProperty() { return designation; }
        public StringProperty referenceProperty() { return reference; }
        public IntegerProperty quantiteProperty() { return quantite; }
    }

}

idStockProperty


*/
////////////////////////////////////


package com.votreport.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.*;
import java.sql.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.converter.IntegerStringConverter;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.votreport.controllers.AjouteProduitSousEntiteController;




import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

import javafx.scene.layout.StackPane;


public class StockSousEntiteController {

    @FXML private TableView<StockProduit> stockTable;
    @FXML private TableColumn<StockProduit, String> colProduit;
    @FXML private TableColumn<StockProduit, String> colReference;
    @FXML private TableColumn<StockProduit, Integer> colQuantite;
    @FXML private TableColumn<StockProduit, String> colDateTransfert;
    @FXML private TableColumn<StockProduit, String> colDerniereMAJ;
    @FXML private TableColumn<StockProduit, Void> colActions;
    @FXML private StackPane contentPane;


    private ObservableList<StockProduit> stockData = FXCollections.observableArrayList();
    private StockProduit newStockRow = new StockProduit(-1, -1, "", "", 0, "", "");
    private int idSousEntite;
    private String nomSousEntite;
    private Controller mainController;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    public void setIdSousEntite(int idSousEntite) {
        this.idSousEntite = idSousEntite;
        loadNomSousEntite();
        loadStock();
    }

    public void setMainController(Controller controller) {
        this.mainController = controller;
    }

    private void loadNomSousEntite() {
        String sql = "SELECT nom FROM SousEntite WHERE id_sous_entite = ?";
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/fskn", "root", "");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, idSousEntite);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                this.nomSousEntite = rs.getString("nom");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void handleKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            StockProduit selected = stockTable.getSelectionModel().getSelectedItem();
            if (selected != null) saveOrUpdateStock(selected);
        }
    }

    @FXML
    public void initialize() {
        // Configuration des colonnes
        colProduit.setCellValueFactory(cellData -> cellData.getValue().designationProperty());
        colReference.setCellValueFactory(cellData -> cellData.getValue().referenceProperty());
        colQuantite.setCellValueFactory(cellData -> cellData.getValue().quantiteProperty().asObject());
        colDateTransfert.setCellValueFactory(cellData -> cellData.getValue().dateTransfertProperty());
        colDerniereMAJ.setCellValueFactory(cellData -> cellData.getValue().derniereMajProperty());
        
        // Rendre la colonne quantité éditable
        colQuantite.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        colQuantite.setOnEditCommit(event -> {
            StockProduit stock = event.getRowValue();
            stock.setQuantite(event.getNewValue());
            saveOrUpdateStock(stock);
        });
        
        // Colonne Actions
        colActions.setCellFactory(param -> new TableCell<StockProduit, Void>() {
            private final Button btnDelete = new Button("✕");
            private final Button btnVendre = new Button("Vendre");
            private final HBox container = new HBox(5, btnVendre, btnDelete);
            
            {
                btnDelete.getStyleClass().add("delete-button");
                btnDelete.setOnAction(event -> {
                    StockProduit stock = getTableView().getItems().get(getIndex());
                    deleteStock(stock);
                });
                
                btnVendre.getStyleClass().add("sell-button");
                btnVendre.setOnAction(event -> {
                    StockProduit stock = getTableView().getItems().get(getIndex());
                    showVenteDialog(stock);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });

        stockTable.setItems(stockData);
        stockTable.setEditable(true);
        stockData.add(newStockRow);
        stockTable.setOnKeyPressed(this::handleKeyPressed);
    }

    private void showVenteDialog(StockProduit stock) {
        TextInputDialog dialog = new TextInputDialog("1");
        dialog.setTitle("Enregistrer une vente");
        dialog.setHeaderText("Vente de " + stock.getDesignation());
        dialog.setContentText("Quantité vendue:");
        
        dialog.showAndWait().ifPresent(quantiteStr -> {
            try {
                int quantite = Integer.parseInt(quantiteStr);
                if (quantite <= 0) {
                    showAlert("Erreur", "La quantité doit être positive");
                    return;
                }
                
                if (quantite > stock.getQuantite()) {
                    showAlert("Erreur", "Quantité en stock insuffisante");
                    return;
                }
                
                enregistrerVente(stock, quantite);
                // Mettre à jour la quantité en stock
                stock.setQuantite(stock.getQuantite() - quantite);
                updateStockInDatabase(stock);
                
            } catch (NumberFormatException e) {
                showAlert("Erreur", "Veuillez entrer un nombre valide");
            }
        });
    }

    private void enregistrerVente(StockProduit stock, int quantite) {
        String sql = "INSERT INTO Vente (id_produit, id_sous_entite, quantite, prix_unitaire, montant_total, vendu_par) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/fskn", "root", "");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            // Récupérer le prix de vente du produit
            double prixUnitaire = getPrixVenteProduit(stock.getIdProduit());
            double montantTotal = prixUnitaire * quantite;
            
            pstmt.setInt(1, stock.getIdProduit());
            pstmt.setInt(2, idSousEntite);
            pstmt.setInt(3, quantite);
            pstmt.setDouble(4, prixUnitaire);
            pstmt.setDouble(5, montantTotal);
            pstmt.setString(6, this.nomSousEntite);
            pstmt.executeUpdate();
            
            showAlert("Succès", "Vente enregistrée avec succès!");
            
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors de l'enregistrement de la vente");
        }
    }

    private double getPrixVenteProduit(int idProduit) throws SQLException {
        String sql = "SELECT prix_vente FROM Produit WHERE id_produit = ?";
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/fskn", "root", "");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, idProduit);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getDouble("prix_vente");
            }
        }
        return 0;
    }

    public void loadStock() {
        stockData.clear();
        String sql = "SELECT s.id_stock_sous_entite, s.id_produit, p.designation, p.reference, " +
                     "s.quantite, s.date_transfert, s.derniere_mise_a_jour " +
                     "FROM StockSousEntite s " +
                     "JOIN Produit p ON s.id_produit = p.id_produit " +
                     "WHERE s.id_sous_entite = ?";
        
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/fskn", "root", "");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, idSousEntite);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                String dateTransfert = formatDate(rs.getTimestamp("date_transfert"));
                String derniereMaj = formatDate(rs.getTimestamp("derniere_mise_a_jour"));
                
                stockData.add(new StockProduit(
                    rs.getInt("id_stock_sous_entite"),
                    rs.getInt("id_produit"),
                    rs.getString("designation"),
                    rs.getString("reference"),
                    rs.getInt("quantite"),
                    dateTransfert,
                    derniereMaj
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        stockData.add(newStockRow);
    }

    private String formatDate(Timestamp timestamp) {
        if (timestamp == null) return "";
        return timestamp.toLocalDateTime().format(dateFormatter);
    }

    // ... (autres méthodes restent inchangées)

    private void saveOrUpdateStock(StockProduit stock) {
        if (stock.getIdStock() == -1) {
            if (isStockValid(stock)) saveNewStock(stock);
        } else {
            updateStockInDatabase(stock);
        }
    }

    private boolean isStockValid(StockProduit stock) {
        return stock.getIdProduit() > 0 && stock.getQuantite() > 0;
    }

    private void saveNewStock(StockProduit stock) {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/fskn", "root", "");
             PreparedStatement pstmt = conn.prepareStatement(
                 "INSERT INTO StockSousEntite (id_produit, id_sous_entite, quantite) VALUES (?, ?, ?)")) {
            
            pstmt.setInt(1, stock.getIdProduit());
            pstmt.setInt(2, idSousEntite);
            pstmt.setInt(3, stock.getQuantite());
            pstmt.executeUpdate();
            
            // Réinitialiser la ligne d'ajout
            newStockRow.setIdProduit(-1);
            newStockRow.setDesignation("");
            newStockRow.setReference("");
            newStockRow.setQuantite(0);
            
            loadStock();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ajouter le produit au stock");
        }
    }

    private void updateStockInDatabase(StockProduit stock) {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/fskn", "root", "");
             PreparedStatement pstmt = conn.prepareStatement(
                 "UPDATE StockSousEntite SET quantite = ? WHERE id_stock_sous_entite = ?")) {
            
            pstmt.setInt(1, stock.getQuantite());
            pstmt.setInt(2, stock.getIdStock());
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de mettre à jour la quantité");
        }
    }

    private void deleteStock(StockProduit stock) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmer la suppression");
        alert.setHeaderText("Supprimer : " + stock.getDesignation());
        alert.setContentText("Êtes-vous sûr de vouloir retirer ce produit du stock?");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/fskn", "root", "");
                     PreparedStatement pstmt = conn.prepareStatement(
                         "DELETE FROM StockSousEntite WHERE id_stock_sous_entite = ?")) {
                    
                    pstmt.setInt(1, stock.getIdStock());
                    pstmt.executeUpdate();
                    loadStock();
                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert("Erreur", "Impossible de supprimer le produit du stock");
                }
            }
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ... (autres méthodes restent inchangées)

    public static class StockProduit {
        private final IntegerProperty idStock = new SimpleIntegerProperty();
        private final IntegerProperty idProduit = new SimpleIntegerProperty();
        private final StringProperty designation = new SimpleStringProperty();
        private final StringProperty reference = new SimpleStringProperty();
        private final IntegerProperty quantite = new SimpleIntegerProperty();
        private final StringProperty dateTransfert = new SimpleStringProperty();
        private final StringProperty derniereMaj = new SimpleStringProperty();

        public StockProduit(int idStock, int idProduit, String designation, String reference, 
                          int quantite, String dateTransfert, String derniereMaj) {
            this.idStock.set(idStock);
            this.idProduit.set(idProduit);
            this.designation.set(designation);
            this.reference.set(reference);
            this.quantite.set(quantite);
            this.dateTransfert.set(dateTransfert);
            this.derniereMaj.set(derniereMaj);
        }

        // Getters/Setters
        public int getIdStock() { return idStock.get(); }
        public void setIdStock(int idStock) { this.idStock.set(idStock); }
        public IntegerProperty idStockProperty() { return idStock; }

        public int getIdProduit() { return idProduit.get(); }
        public String getDesignation() { return designation.get(); }
        public String getReference() { return reference.get(); }
        public int getQuantite() { return quantite.get(); }
        
        // ... (autres getters/setters)
        public void setIdProduit(int idProduit) { this.idProduit.set(idProduit); }
        public void setDesignation(String designation) { this.designation.set(designation); }
        public void setReference(String reference) { this.reference.set(reference); }
        public void setQuantite(int quantite) { this.quantite.set(quantite); }

        
        public String getDateTransfert() { return dateTransfert.get(); }
        public void setDateTransfert(String dateTransfert) { this.dateTransfert.set(dateTransfert); }
        public StringProperty dateTransfertProperty() { return dateTransfert; }


        
        public String getDerniereMaj() { return derniereMaj.get(); }
        public void setDerniereMaj(String derniereMaj) { this.derniereMaj.set(derniereMaj); }
        public StringProperty derniereMajProperty() { return derniereMaj; }

        public IntegerProperty idProduitProperty() { return idProduit; }
        public StringProperty designationProperty() { return designation; }
        public StringProperty referenceProperty() { return reference; }
        public IntegerProperty quantiteProperty() { return quantite; }
    }

private void loadAjouterProduitView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/votreport/views/ajouterproduitsousentite.fxml"));
            Parent ajouterProduitView = loader.load();
            
            AjouteProduitSousEntiteController controller = loader.getController();
            controller.setIdSousEntite(idSousEntite);
            controller.setParentController(this);
            
            // Efface le contenu actuel et ajoute la nouvelle vue
            contentPane.getChildren().clear();
            contentPane.getChildren().add(ajouterProduitView);
            
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger l'interface d'ajout");
        }
    }
    
    public void returnToStockView() {
        // Recharge la vue principale du stock
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/votreport/views/stocksousentite.fxml"));
            Parent stockView = loader.load();
            
            StockSousEntiteController controller = loader.getController();
            controller.setIdSousEntite(idSousEntite);
            controller.setMainController(mainController);
            
            contentPane.getChildren().clear();
            contentPane.getChildren().add(stockView);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleAjouterProduit() {
        loadAjouterProduitView();
    }


}

/////////////////






/*
public static class StockProduit {
        private final IntegerProperty idStock = new SimpleIntegerProperty();
        private final IntegerProperty idProduit = new SimpleIntegerProperty();
        private final StringProperty designation = new SimpleStringProperty();
        private final StringProperty reference = new SimpleStringProperty();
        private final IntegerProperty quantite = new SimpleIntegerProperty();

        public StockProduit(int idStock, int idProduit, String designation, String reference, int quantite) {
            this.idStock.set(idStock);
            this.idProduit.set(idProduit);
            this.designation.set(designation);
            this.reference.set(reference);
            this.quantite.set(quantite);
        }

        // Getters
        public int getIdStock() { return idStock.get(); }
        public int getIdProduit() { return idProduit.get(); }
        public String getDesignation() { return designation.get(); }
        public String getReference() { return reference.get(); }
        public int getQuantite() { return quantite.get(); }

        // Setters
        public void setIdStock(int idStock) { this.idStock.set(idStock); }
        public void setIdProduit(int idProduit) { this.idProduit.set(idProduit); }
        public void setDesignation(String designation) { this.designation.set(designation); }
        public void setReference(String reference) { this.reference.set(reference); }
        public void setQuantite(int quantite) { this.quantite.set(quantite); }

        // Property getters
        public IntegerProperty idStockProperty() { return idStock; }
        public IntegerProperty idProduitProperty() { return idProduit; }
        public StringProperty designationProperty() { return designation; }
        public StringProperty referenceProperty() { return reference; }
        public IntegerProperty quantiteProperty() { return quantite; }
    }

    */