package com.votreport.controllers;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import javafx.scene.control.Label; // Import manquant
import javafx.fxml.FXMLLoader; // Import manquant
import javafx.scene.control.Button; // Import manquant
import javafx.scene.layout.VBox; // Import manquant
import java.io.IOException; // Import manquant
import javafx.scene.Parent; // Import manquant à ajouter
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;

import java.sql.*;


public class Controller {
    
    @FXML private Label lblStockPrincipal;

    @FXML
    private BorderPane rootPane;

    @FXML
    private Button btnToggleTheme;
    @FXML private Button btnAsousantiter;

    private boolean isDarkTheme = true;

    @FXML
    private void initialize() {
        // Appliquer le thème sombre par défaut
        btnAsousantiter.setOnAction(event -> showAjouterSousEntite());

        rootPane.getStyleClass().add("theme-dark");

        // Configurer l'action du bouton de thème
        btnToggleTheme.setOnAction(event -> toggleTheme());

        // Ajouter l'animation de fond
        addBackgroundAnimation();
    }

    ///////////////////



    /////////////////

    private void addBackgroundAnimation() {
        // Récupérer le centre actuel
        Node currentCenter = rootPane.getCenter();

        // Créer un StackPane pour superposer l'animation et le contenu
        StackPane stack = new StackPane();

        // Créer le pane d'animation
        Pane animationPane = createAnimationPane();
        animationPane.setMouseTransparent(true); // Ne pas capturer les événements de la souris

        // Ajouter l'animation et le contenu actuel au StackPane
        stack.getChildren().addAll(animationPane, currentCenter);

        // Définir le StackPane comme nouveau centre
        rootPane.setCenter(stack);
    }

    private Pane createAnimationPane() {
        Pane pane = new Pane();
        pane.setStyle("-fx-background-color: transparent;");

        // Créer 15 cercles animés
        for (int i = 0; i < 15; i++) {
            Circle circle = new Circle();
            // Rayon initial aléatoire entre 20 et 50
            double radius = 20 + Math.random() * 30;
            circle.setRadius(radius);

            // Position initiale aléatoire (sur une zone plus large pour couvrir l'écran)
            double x = Math.random() * 1000;
            double y = Math.random() * 800;
            circle.setCenterX(x);
            circle.setCenterY(y);

            // Couleur pastel aléatoire
            String[] colors = {"#FFCCCB", "#FFECB3", "#C8E6C9", "#B3E5FC", "#D1C4E9"};
            String color = colors[(int)(Math.random() * colors.length)];
            circle.setFill(Color.web(color));

            // Mouvement aléatoire
            double deltaX = (Math.random() - 0.5) * 500; // Déplacement horizontal
            double deltaY = (Math.random() - 0.5) * 400; // Déplacement vertical

            // Échelle aléatoire entre 0.5 et 1.5
            double scaleFrom = 0.5 + Math.random();
            double scaleTo = 0.5 + Math.random();

            // Opacité aléatoire entre 0.2 et 0.8
            double opacityFrom = 0.2 + Math.random() * 0.6;
            double opacityTo = 0.2 + Math.random() * 0.6;

            // Durée aléatoire entre 5 et 15 secondes
            double duration = 5 + Math.random() * 10;

            // Transition de translation
            TranslateTransition translate = new TranslateTransition(Duration.seconds(duration), circle);
            translate.setFromX(0);
            translate.setToX(deltaX);
            translate.setFromY(0);
            translate.setToY(deltaY);
            translate.setAutoReverse(true);
            translate.setCycleCount(TranslateTransition.INDEFINITE);

            // Transition d'échelle
            ScaleTransition scale = new ScaleTransition(Duration.seconds(duration), circle);
            scale.setFromX(scaleFrom);
            scale.setToX(scaleTo);
            scale.setFromY(scaleFrom);
            scale.setToY(scaleTo);
            scale.setAutoReverse(true);
            scale.setCycleCount(ScaleTransition.INDEFINITE);

            // Transition de fondu
            FadeTransition fade = new FadeTransition(Duration.seconds(duration), circle);
            fade.setFromValue(opacityFrom);
            fade.setToValue(opacityTo);
            fade.setAutoReverse(true);
            fade.setCycleCount(FadeTransition.INDEFINITE);

            // Combiner les transitions
            ParallelTransition parallel = new ParallelTransition(translate, scale, fade);
            parallel.setDelay(Duration.seconds(Math.random() * 5)); // Délai aléatoire
            parallel.play();

            pane.getChildren().add(circle);
        }

        return pane;
    }

    private void toggleTheme() {
        // Créer une transition de fondu pour le changement de thème
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), rootPane);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(event -> {
            // Supprimer l'ancien thème
            rootPane.getStyleClass().removeAll("theme-dark", "theme-light");

            // Appliquer le nouveau thème
            if (isDarkTheme) {
                rootPane.getStyleClass().add("theme-light");
                btnToggleTheme.setText("🌙");
            } else {
                rootPane.getStyleClass().add("theme-dark");
                btnToggleTheme.setText("☀");
            }

            // Inverser l'état du thème
            isDarkTheme = !isDarkTheme;

            // Transition de fondu pour réapparaître
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), rootPane);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
    }
    @FXML private Button btnAjouterProduit; // Ajout de la référence au bouton


/////////////
    @FXML 
    private void showAjouterProduit() {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/votreport/views/ajouter.fxml"));
        VBox ajouterView = loader.load(); // Utiliser VBox au lieu de Parent
        
        AjouterController ajouterController = loader.getController();
        ajouterController.setController(this);
        
        // Configure la vue pour qu'elle prenne tout l'espace disponible
        ajouterView.prefHeightProperty().bind(rootPane.heightProperty());
        ajouterView.prefWidthProperty().bind(rootPane.widthProperty());
        
        rootPane.setCenter(ajouterView);
    } catch (Exception e) {
        e.printStackTrace();
    }
}
    
    // Méthode pour rafraîchir l'affichage du stock
    public void refreshStock() {
        try {
            int count = getProductCount();
            lblStockPrincipal.setText(count + " produits");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // Méthode pour compter les produits
    private int getProductCount() throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/fskn", "root", "");
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM Produit");
        rs.next();
        int count = rs.getInt(1);
        rs.close();
        stmt.close();
        conn.close();
        return count;
    }
    
    



    @FXML 
    private void showTransfererProduit() {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/votreport/views/transferer.fxml"));
        BorderPane ajouterVie = loader.load(); // ✅ Correction du cast
        
        TransfererController controller = loader.getController(); // ✅
        controller.setController(this);
        
        // Configure la vue pour qu'elle prenne tout l'espace disponible
        ajouterVie.prefHeightProperty().bind(rootPane.heightProperty());
        ajouterVie.prefWidthProperty().bind(rootPane.widthProperty());
        
        rootPane.setCenter(ajouterVie);
    } catch (Exception e) {
        e.printStackTrace();
    }
}



// Dans la section @FXML

// Dans la méthode initialize()

// Nouvelle méthode pour charger la vue
@FXML
private void showAjouterSousEntite() {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/votreport/views/ajoutersousentite.fxml"));
        Parent view = loader.load();
        
        SousEntiteController controller = loader.getController();
        controller.setMainController(this);
        
        rootPane.setCenter(view);
    } catch (IOException e) {
        e.printStackTrace();
        showAlert("Erreur", "Impossible de charger la vue des sous-entités");
    }
}

public void showAlert(String title, String message) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
}

public void showMainView() {
    // Implémentez votre logique pour revenir à la vue principale
    // Exemple: rootPane.setCenter(votreVuePrincipale);
}



@FXML
public void showStockSousEntite(int idSousEntite) {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/votreport/views/stocksousentite.fxml"));
        Parent view = loader.load();
        
        StockSousEntiteController controller = loader.getController();
        controller.setIdSousEntite(idSousEntite);
        controller.setMainController(this);
        
        rootPane.setCenter(view);
    } catch (IOException e) {
        e.printStackTrace();
        showAlert("Erreur", "Impossible de charger le stock de la sous-entité");
    }
}

// Ajoutez cette méthode dans Controller.java
@FXML
private void showVente() {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/votreport/views/vente.fxml"));
        Parent view = loader.load();
        
        VenteController controller = loader.getController();
        controller.setMainController(this);
        
        rootPane.setCenter(view);
    } catch (IOException e) {
        e.printStackTrace();
        showAlert("Erreur", "Impossible de charger la vue des ventes");
    }
}





}




//////////////////////


