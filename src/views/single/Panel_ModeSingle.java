/*
  Copyright (C) 2010 Aurelien Da Campo
  
  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
*/

package views.single;

import i18n.Langue;

import java.awt.*;
import java.awt.event.*;
import java.io.File;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import views.View_MenuPrincipal;
import views.ManageFonts;
import views.LookInterface;
import views.Panel_MenuPrincipal;
import views.common.View_HTML;

import exceptions.NoPositionAvailableException;
import models.game.Game;
import models.game.Game_Single;
import models.map.*;
import models.player.Team;
import models.player.Player;
import models.utils.Tips;
import models.utils.SoundManagement;
import models.utils.HighScores;
import models.utils.Tools;
import models.utils.Score;

/**
 * 单人游戏模式主菜单.
 * <p>
 * 显示一个菜单，允许玩家选择地图. 
 * <p>
 * 地图上的按钮为静态的，以节省开销.
 * 
 * @author Aurelien Da Campo
 * @version 1.0 | 15 decembre 2009
 * @since jdk1.6.0_16
 */
public class Panel_ModeSingle extends JPanel implements ActionListener, Runnable
{
	// constantes statiques
    private static final long serialVersionUID 	= 1L;
	private static final ImageIcon I_QUITTER 	= new ImageIcon("img/icones/door_out.png");
	private static final ImageIcon I_AIDE 		= new ImageIcon("img/icones/help.png");
	private static final ImageIcon I_SCORE      = new ImageIcon("img/icones/star.png");
	private static final int IMAGE_MENU_LARGEUR = 120;
	private static final int IMAGE_MENU_HAUTEUR = 120;
	private static final ImageIcon icoCADENAS      = new ImageIcon("img/icones/lock.png");
  
	private final int MARGES_PANEL                 = 40;
    

	// elements du formulaire
	JLabel lblTitre = new JLabel(Langue.getTexte(Langue.ID_TITRE_PARTIE_SOLO));
	
	private final JMenuItem itemAPropos	    	= new JMenuItem(Langue.getTexte(Langue.ID_TXT_BTN_A_PROPOS),I_AIDE);
	private final JMenuItem itemQuitter	   		= new JMenuItem(Langue.getTexte(Langue.ID_TXT_BTN_QUITTER),I_QUITTER);
	// 显示得分按钮
	private final JMenuItem itemMSElementTD     = new JMenuItem(ElementTD.NOM);
    private final JMenuItem itemMSSpiral        = new JMenuItem(Spiral.NOM);
    private final JMenuItem itemMSDesert        = new JMenuItem(Desert.NOM);
    private final JMenuItem itemMSWaterWorld    = new JMenuItem(WaterWorld.NOM);
	// 四种游戏地图按钮
	private final JButton[] boutonsTerrains     = new JButton[4]; 
	private final JButton[] boutonsScore        = new JButton[4]; 
	private final JButton bRetour              = new JButton(Langue.getTexte(Langue.ID_TXT_BTN_RETOUR));
	
	private JProgressBar chargementTerrain;
	private Thread thread;
    private boolean chargementTermine;
	private JFrame parent;
	
	private JLabel lblInfo = new JLabel(Langue.getTexte(Langue.ID_TXT_CLIQUER_SUR_TERRAIN));

	/**
	 * Constructeur de la fenetre du menu principal
	 */
	public Panel_ModeSingle(JFrame parent)
	{
		//-------------------------------
		//-- preferances de le fenetre --
		//-------------------------------
	    this.parent = parent;
	    setLayout(new BorderLayout());
		parent.setTitle(Langue.getTexte(Langue.ID_TITRE_PARTIE_SOLO)+" - ASD Tower Defense");

		setBorder(new EmptyBorder(new Insets(MARGES_PANEL, MARGES_PANEL,
                MARGES_PANEL, MARGES_PANEL)));
		
		setBackground(LookInterface.COULEUR_DE_FOND_PRI);

		//--------------------------------------
        //-- chargement des scores et étoiles --
        //--------------------------------------
		
	    String[] nomTerrains = new String[]{"ElementTD","Spiral","Desert","WaterWorld"};
        Score[] scoresMax    = new Score[4];
        HighScores ms;
        int nbEtoiles = 0;
        
        for(int i=0; i < nomTerrains.length; i++)
        {  
            ms = new HighScores(nomTerrains[i]);
            
            if(ms.getScores().size() > 0)
            {
                Score score = ms.getScores().get(0); 
                nbEtoiles += score.getNbEtoiles();
                scoresMax[i] = score;
            }
            else
                scoresMax[i] = new Score(" ",0,0);
        }
		
		
		//----------------------------
        //-- création du formulaire --
        //----------------------------
		
		JPanel pFormulaire = new JPanel(new BorderLayout());
		pFormulaire.setOpaque(false);
		
		
		//------------------------------
        //-- 标题 + 星级评定 --
        //------------------------------
		JPanel pNord = new JPanel(new BorderLayout());
		pNord.setOpaque(false);
		
		// titre
		lblTitre.setFont(ManageFonts.POLICE_TITRE);
		lblTitre.setForeground(LookInterface.COULEUR_TEXTE_PRI);
		pNord.add(lblTitre,BorderLayout.WEST);
		
		// étoiles
		JPanel pNbEtoiles = new JPanel(new FlowLayout());
		pNbEtoiles.setOpaque(false);
		
		JLabel lblNbEtoiles = new JLabel(nbEtoiles+" x");
		lblNbEtoiles.setFont(ManageFonts.POLICE_SOUS_TITRE);
		lblNbEtoiles.setForeground(LookInterface.COULEUR_TEXTE_PRI);
		
		pNbEtoiles.add(lblNbEtoiles);
		pNbEtoiles.add(new JLabel(I_SCORE));
		pNord.add(pNbEtoiles,BorderLayout.EAST);
		
        pFormulaire.add(pNord,BorderLayout.NORTH);
		
		
		//-----------------------------
		//-- chargement des terrains --
		//-----------------------------
		
		// attent que toutes les images soit complementements chargees
		MediaTracker tracker = new MediaTracker(this);
		tracker.addImage(ElementTD.IMAGE_MENU, 0);
		tracker.addImage(Spiral.IMAGE_MENU, 1);
		tracker.addImage(Desert.IMAGE_MENU, 2);
		tracker.addImage(WaterWorld.IMAGE_MENU, 3);
		
		try { 
			tracker.waitForAll(); 
		} 
		catch (InterruptedException e){ 
			e.printStackTrace(); 
		}
		
		// creation des boutons
		boutonsTerrains[0] = new JButton(new ImageIcon(
							Tools.redimentionner(ElementTD.IMAGE_MENU,
									IMAGE_MENU_LARGEUR,IMAGE_MENU_HAUTEUR)));
		
		boutonsTerrains[1] = new JButton(new ImageIcon(
							Tools.redimentionner(Spiral.IMAGE_MENU,
									IMAGE_MENU_LARGEUR,IMAGE_MENU_HAUTEUR)));
		
		boutonsTerrains[2] = new JButton(new ImageIcon(
							Tools.redimentionner(Desert.IMAGE_MENU,
									IMAGE_MENU_LARGEUR,IMAGE_MENU_HAUTEUR)));
		
		boutonsTerrains[3] = new JButton(new ImageIcon(
							Tools.redimentionner(WaterWorld.IMAGE_MENU,
									IMAGE_MENU_LARGEUR,IMAGE_MENU_HAUTEUR)));

		// ajout des boutons au panel et ajout des ecouteurs
		JPanel pBoutonsTerrains = new JPanel(new FlowLayout());
		
		pBoutonsTerrains.setBorder(new EmptyBorder(new Insets(60, 0, 0, 0)));
		
		
		pBoutonsTerrains.setOpaque(false);
		
		for(int i=0; i < boutonsTerrains.length; i++)
		{
		    JButton bouton = boutonsTerrains[i];
		    ManageFonts.setStyle(bouton);
		    
		    
		    JPanel pInfoTerrain = new JPanel(new BorderLayout());
		    pInfoTerrain.setOpaque(false);
		    
		    
		    bouton.addActionListener(this);
		    pInfoTerrain.add(bouton,BorderLayout.NORTH);
		    
		    // recuperation du meilleur score
		    Score score = scoresMax[i];
		    
		    pInfoTerrain.add(new Panel_Star(score),BorderLayout.CENTER);
		    
		    
		    if(score.getValeur() > 0)
		    {
		        String txt = score.getNomJoueur()+" - "+score.getValeur()+"";
		    
		        JButton bScore = new JButton(txt);
		    
        	    boutonsScore[i] = bScore;
        	    
        	    ManageFonts.setStyle(bScore);
        	    bScore.addActionListener(this);

        	    pInfoTerrain.add(bScore,BorderLayout.SOUTH);
		    }
		    else
		    {
		        JPanel tmp = new JPanel();
		        tmp.setPreferredSize(new Dimension(1,30));
		        tmp.setOpaque(false);
		        pInfoTerrain.add(tmp,BorderLayout.SOUTH);
		    }
		    //-----------------------------------------
	        //-- bloquage des terrains - progression --
	        //-----------------------------------------
		    
		    if(i == 1 && nbEtoiles < 1)
		    {
		        bouton.setEnabled(false);
		        
		        JLabel lbl = new JLabel(Langue.getTexte(Langue.ID_TXT_1_ETOILE_MIN),icoCADENAS,0);
                lbl.setForeground(LookInterface.COULEUR_TEXTE_PRI);
		        
		        pInfoTerrain.add(lbl,BorderLayout.SOUTH);
		    }
		        
		    if(i == 2 && nbEtoiles < 3)
		    {
                bouton.setEnabled(false);
                
                JLabel lbl = new JLabel(String.format(Langue.getTexte(Langue.ID_TXT_X_ETOILES_MIN),3),icoCADENAS,0);
                lbl.setForeground(LookInterface.COULEUR_TEXTE_PRI);
                
		        pInfoTerrain.add(lbl,BorderLayout.SOUTH);
		    }
		    
		    if(i == 3 && nbEtoiles < 7)
		    {
		        bouton.setEnabled(false);
		        
		        JLabel lbl = new JLabel(String.format(Langue.getTexte(Langue.ID_TXT_X_ETOILES_MIN),7),icoCADENAS,0);
                lbl.setForeground(LookInterface.COULEUR_TEXTE_PRI);
                
                pInfoTerrain.add(lbl,BorderLayout.SOUTH);
		    }

		    // ajout au panel
		    pBoutonsTerrains.add(pInfoTerrain);
		}

		JPanel pCentre = new JPanel(new BorderLayout());
		pCentre.setOpaque(false);
		
		pCentre.add(pBoutonsTerrains,BorderLayout.NORTH);
		
		JLabel lblAstuce = new JLabel("\""+Tips.getAstuceAleatoirement()+"\"");
		lblAstuce.setFont(ManageFonts.POLICE_SOUS_TITRE);
		lblAstuce.setForeground(LookInterface.COULEUR_TEXTE_PRI);
		
		JPanel pPourCentrer = new JPanel();
		pPourCentrer.setOpaque(false);
		pPourCentrer.add(lblAstuce);
		pPourCentrer.setBorder(new EmptyBorder(0, 0, 100, 0));
		pCentre.add(pPourCentrer,BorderLayout.SOUTH);
        
		pFormulaire.add(pCentre,BorderLayout.CENTER);
		
		
		
	    //-------------------
        //-- bouton retour --
        //-------------------
		
		JPanel pFond = new JPanel(new BorderLayout());
		
		
		
		
		pFond.setOpaque(false);
		pFond.setBorder(new EmptyBorder(0, 0, 0, 100));
		
		
		bRetour.addActionListener(this);
		bRetour.setPreferredSize(new Dimension(80,50));
		ManageFonts.setStyle(bRetour);
		pFond.add(bRetour,BorderLayout.WEST);
        pFormulaire.add(pFond,BorderLayout.SOUTH);


        lblInfo.setFont(ManageFonts.POLICE_INFO);
        //lblInfo.setForeground(GestionnaireDesPolices.COULEUR_INFO);
        lblInfo.setForeground(LookInterface.COULEUR_TEXTE_PRI);
        
        pFond.add(lblInfo,BorderLayout.EAST);
        
		add(pFormulaire,BorderLayout.CENTER);
	}

    /**
     * Gestionnaire des evenements. 
     * <p>
     * Cette methode est appelee en cas d'evenement
     * sur un objet ecouteur de ActionListener
     * 
     * @param ae l'evenement associe
     */
	public void actionPerformed(ActionEvent ae)
	{
		Object source = ae.getSource();
		
		// quitter
		if(source == itemQuitter)
			System.exit(0); // Fermeture correcte du logiciel
		
		// a propos
		else if(source == itemAPropos)
		    new View_HTML(Langue.getTexte(Langue.ID_TXT_BTN_A_PROPOS), new File(Langue.getTexte(Langue.ID_ADRESSE_A_PROPOS)),parent);
		
		// 地图
		else if(source == boutonsTerrains[0])
		{
		    Game jeu = new Game_Single();
		    lancerJeu(jeu, new ElementTD(jeu));
		}
		else if(source == boutonsTerrains[1])
		{
		    Game jeu = new Game_Single();
            lancerJeu(jeu, new Spiral(jeu));
		}
		else if(source == boutonsTerrains[2])
		{ 
		    Game jeu = new Game_Single();
		    lancerJeu(jeu, new Desert(jeu));
		}
		else if(source == boutonsTerrains[3])
		{
		    Game jeu = new Game_Single();
		    lancerJeu(jeu, new WaterWorld(jeu));
		}
		else if(source == itemMSElementTD)
		    new View_TopScores(ElementTD.NOM, parent);
		else if(source == itemMSSpiral)
            new View_TopScores(Spiral.NOM, parent);
		else if(source == itemMSDesert)
            new View_TopScores(Desert.NOM, parent);
		else if(source == itemMSWaterWorld)
            new View_TopScores(WaterWorld.NOM, parent);
		else if(source == bRetour)
		{
		    parent.getContentPane().removeAll();
            parent.getContentPane().add(new Panel_MenuPrincipal(parent),
                    BorderLayout.CENTER);
            parent.getContentPane().validate();
		}
		else if(source == boutonsScore[0])
            new View_TopScores("ElementTD", parent);    
        else if(source == boutonsScore[1])
            new View_TopScores("Spiral", parent); 
        else if(source == boutonsScore[2])
            new View_TopScores("Desert", parent); 
        else if(source == boutonsScore[3])
            new View_TopScores("WaterWorld", parent);   
	}

	/**
	 * Permet de lancer un jeu.
	 * 
	 * Elle créer les équipes pour un jeu solo
	 * 
	 * @param jeu le jeu
	 * @param terrain le terrain
	 */
    private void lancerJeu(Game jeu, Field terrain)
    {
        SoundManagement.arreterTousLesSons(View_MenuPrincipal.FICHIER_MUSIQUE_MENU);
        
        actionnerBarreDeChargement();
        
        terrain.initialiser();
        jeu.setTerrain(terrain);
        Team equipe = jeu.getEquipes().get(0); // les equipes sont créer par le terrain
        Player joueur = new Player("Joueur");
        
        try{
            equipe.ajouterJoueur(joueur);
        } 
        catch (NoPositionAvailableException e){
            e.printStackTrace();
        }
        
        jeu.setJoueurPrincipal(joueur);
        jeu.initialiser();
        new View_JeuSolo(jeu);
        
        chargementTermine = true;
        parent.dispose();
    }

    synchronized private void actionnerBarreDeChargement()
    {  
        thread = new Thread(this);
        thread.start();   
    }

    @Override
    public void run()
    {
       /* version.setText("   CHARGEMENT DE LA CARTE");
        version.setForeground(Color.BLACK);*/
        chargementTerrain = new JProgressBar();
        add(chargementTerrain,BorderLayout.SOUTH);
        validate();
        
        int pourcent = 0;
        
        while(!chargementTermine)
        {
            pourcent = (pourcent+2)%100;
     
            chargementTerrain.setValue(pourcent);
            // TODO EXCEPTION ICI java.lang.ClassCastException
            //chargementTerrain.paintImmediately(0,0,1000,200);
            
            try{
                Thread.sleep(10);
            } 
            catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }
}
