/**
 * Copyright (C) 2000 - 2009 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of
 * the GPL, you may redistribute this Program in connection with Free/Libre
 * Open Source Software ("FLOSS") applications as described in Silverpeas's
 * FLOSS exception.  You should have recieved a copy of the text describing
 * the FLOSS exception, and it is also available here:
 * "http://repository.silverpeas.com/legal/licensing"
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Titre :        dbBuilder
 * Description :  Builder des BDs Silverpeas
 * Copyright :    Copyright (c) 2001
 * Soci�t� :      Silverpeas
 * @author ATH
 * @version 1.0
 * Modifications:
 * 11/2004 - DLE - Modification ordre de passage des scripts (init apr�s contraintes)
 */
package com.silverpeas.dbbuilder;

import java.io.*;
import java.util.*;
import org.jdom.*;
import com.stratelia.dbConnector.DBConnexion;
import com.silverpeas.FileUtil.*;

/**
 * @Description :
 *
 * @Copyright   : Copyright (c) 2001
 * @Soci�t�     : Silverpeas
 * @author STR
 * @version 1.0
 */
public class DBBuilder {

  // Version application
  public static final String DBBuilderAppVersion = "V5";
  // Params
  private static String dbType = null;
  private static String jdbcDriver;
  private static String dataSource = null;
  private static String userName = null;
  private static String password = null;
  private static String action = null;
  private static Boolean verbose = null;
  private static Boolean simulate = null;
  private static String moduleName = null;
  // Installation
  private static final String HOME_KEY = "dbbuilder.home";
  private static String dbbuilderHome = null;
  private static final String DATA_KEY = "dbbuilder.data";
  private static String dbbuilderData = null;
  // Fichier log
  protected static File fileLog = null;
  protected static PrintWriter bufLog = null;
  // session SQL
  private static DBConnexion con;
  static public final String CREATE_TABLE_TAG = "create_table";
  static public final String CREATE_INDEX_TAG = "create_index";
  static public final String CREATE_CONSTRAINT_TAG = "create_constraint";
  static public final String CREATE_DATA_TAG = "init";
  static public final String DROP_TABLE_TAG = "drop_table";
  static public final String DROP_INDEX_TAG = "drop_index";
  static public final String DROP_CONSTRAINT_TAG = "drop_constraint";
  static public final String DROP_DATA_TAG = "clean";
  static public String login = " ";
  static public String passwd = " ";
  private static final String[] TAGS_TO_MERGE_4_INSTALL = {
    DBBuilderFileItem.CREATE_TABLE_TAG,
    DBBuilderFileItem.CREATE_INDEX_TAG,
    DBBuilderFileItem.CREATE_CONSTRAINT_TAG,
    DBBuilderFileItem.CREATE_DATA_TAG};
  private static final String[] TAGS_TO_MERGE_4_UNINSTALL = {
    DBBuilderFileItem.DROP_CONSTRAINT_TAG,
    DBBuilderFileItem.DROP_INDEX_TAG,
    DBBuilderFileItem.DROP_DATA_TAG,
    DBBuilderFileItem.DROP_TABLE_TAG};
  private static final String[] TAGS_TO_MERGE_4_ALL = {
    DBBuilderFileItem.DROP_CONSTRAINT_TAG,
    DBBuilderFileItem.DROP_INDEX_TAG,
    DBBuilderFileItem.DROP_DATA_TAG,
    DBBuilderFileItem.DROP_TABLE_TAG,
    DBBuilderFileItem.CREATE_TABLE_TAG,
    DBBuilderFileItem.CREATE_INDEX_TAG,
    DBBuilderFileItem.CREATE_CONSTRAINT_TAG,
    DBBuilderFileItem.CREATE_DATA_TAG};
  private static final String[] TAGS_TO_MERGE_4_OPTIMIZE = {
    DBBuilderFileItem.DROP_INDEX_TAG,
    DBBuilderFileItem.CREATE_INDEX_TAG};
  private static final String DBREPOSITORY_SUBDIR = "dbRepository"; // entr�e sur l'arborescence dbRepository
  private static final String CONTRIB_FILES_SUBDIR = "data"; // sous r�pertoire data
  private static final String LOG_FILES_SUBDIR = "log"; // sous r�pertoire log
  private static final String TEMP_FILES_SUBDIR = "temp"; // sous r�pertoire temp
  private static final String ACTION_CONNECT = "-C";
  private static final String ACTION_INSTALL = "-I";
  private static final String ACTION_UNINSTALL = "-U";
  private static final String ACTION_OPTIMIZE = "-O";
  private static final String ACTION_ALL = "-A";
  private static final String ACTION_STATUS = "-S";
  private static final String ACTION_CONSTRAINTS_INSTALL = "-CI";
  private static final String ACTION_CONSTRAINTS_UNINSTALL = "-CU";
  private static final String ACTION_ENFORCE_UNINSTALL = "-FU";
  // R�pertoire des DB Contribution Files
  private static final String DIR_CONTRIBUTIONFILESROOT = getHome() +
      File.separator + DBREPOSITORY_SUBDIR + File.separator + CONTRIB_FILES_SUBDIR;
  // R�pertoire racine des DB Pieces Contribution File
  private static final String DIR_DBPIECESFILESROOT = getHome() + File.separator + DBREPOSITORY_SUBDIR;
  // R�pertoire temp
  private static final String DIR_TEMP = getHome() + File.separator + TEMP_FILES_SUBDIR;
  protected static final String FIRST_DBCONTRIBUTION_FILE = "dbbuilder-contribution.xml";
  protected static final String MASTER_DBCONTRIBUTION_FILE = "master-contribution.xml";
  protected static final String REQUIREMENT_TAG = "requirement"; // pr� requis � v�rifier pour prise en comptes
  protected static final String DEPENDENCY_TAG = "dependency"; // ordonnancement � v�rifier pour prise en comptes
  protected static final String FILE_TAG = "file";
  protected static final String FILENAME_ATTRIB = "name";
  protected static final String PRODUCT_TAG = "product";
  protected static final String PRODUCTNAME_ATTRIB = "name";
  //mes variables rajout�e
  private static Properties dbBuilderResources = new Properties();

  /**
   * @param args
   * @see
   */
  public static void main(String[] args) {

    try {
      // Ouverture des traces
      System.out.println("Start Database build using Silverpeas DBBuilder v. " + DBBuilderAppVersion + " (" + new java.util.Date() + ").");
      fileLog = new File(getHome() + File.separator + LOG_FILES_SUBDIR + File.separator + "DBBuilder.log");
      bufLog = new PrintWriter(new BufferedWriter(new FileWriter(fileLog.getAbsolutePath(), true)));
      displayMessageln(System.getProperty("line.separator") + "************************************************************************");
      displayMessageln("Start Database Build using Silverpeas DBBuilder v. " + DBBuilderAppVersion + " (" + new java.util.Date() + ").");

      // Lecture des variables d'environnement � partir de dbBuilderSettings
      try {
        dbBuilderResources.load(DBBuilder.class.getClassLoader().getResourceAsStream("com/stratelia/silverpeas/dbBuilder/settings/dbBuilderSettings.properties"));
      } catch (java.util.MissingResourceException e) {
        // fichier de ressources absent -> pas grave
        dbBuilderResources = null;
      }
      // remarque : toute autre erreur est fatale -> elle sera catch�e ds le try global

      // Lecture des param�tres d'entr�e
      testParams(args);

      if (simulate != null && simulate.booleanValue() && (dbType.equals("oracle") || dbType.equals("ORACLE"))) {
        throw new Exception("Simulate mode is not allowed for Oracle target databases.");
      }

      displayMessageln(System.getProperty("line.separator") + "Parameters are :");
      displayMessageln("\tRDBMS         : " + dbType);
      displayMessageln("\tJdbcDriver    : " + jdbcDriver);
      displayMessageln("\tDataSource    : " + dataSource);
      displayMessageln("\tUserName      : " + userName);
      displayMessageln("\tAction        : " + action);
      displayMessageln("\tVerbose mode  : " + verbose);
      displayMessageln("\tSimulate mode : " + simulate);

      // Chargement du pilote JDBC
      Class.forName(jdbcDriver);
      // Ouvre connexion JDBC
      Properties props = new Properties();
      props.put("user", userName); // L'ID du user � connecter � la source de donn�es
      props.put("password", password); // Le mot de passe du user � connecter � la base de donn�es
      con = DBConnexion.getInstance();
      con.dbConnexionInitialize(dataSource, props);
      if (ACTION_CONNECT.equals(action)) {
        // un petit message et puis c'est tout
        displayMessageln(System.getProperty("line.separator") + "Database successfully connected.");
        System.out.println(System.getProperty("line.separator") + "Database successfully connected.");
      } else {
        // Modules en place sur la BD avant install
        displayMessageln(System.getProperty("line.separator") + "DB Status before build :");
        Vector packagesIntoDB = new Vector();
        packagesIntoDB = checkDBStatus();
        // initialisation d'un vecteur des instructions SQL � passer en fin d'upgrade
        // pour mettre � niveau les versions de modules en base
        Vector<String> sqlMetaInstructions = new Vector<String>();
        File dirXml = new File(getDBContributionDir());
        DBXmlDocument destXml = new DBXmlDocument(dirXml, MASTER_DBCONTRIBUTION_FILE);
        if (!destXml.getPath().exists()) {
          destXml.getPath().createNewFile();
          BufferedWriter destXmlOut = new BufferedWriter(new FileWriter(destXml.getPath(), false));
          destXmlOut.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
          destXmlOut.newLine();
          destXmlOut.write("<allcontributions>");
          destXmlOut.newLine();
          destXmlOut.write("</allcontributions>");
          destXmlOut.newLine();
          destXmlOut.flush();
          destXmlOut.close();
        }
        destXml.load();
        Vector processesToCacheIntoDB = new Vector();

        File[] listeFileXml = dirXml.listFiles();
        Arrays.sort(listeFileXml);

        List<DBXmlDocument> listeDBXmlDocument = new ArrayList<DBXmlDocument>(listeFileXml.length);

        // Ouverture de tous les fichiers de configurations

        displayMessageln(System.getProperty("line.separator") + "Ignored contribution files are :");
        int ignoredFiles = 0;

        for (File f : listeFileXml) {
          if (f.isFile() && FileUtil.getExtension(f).equals("xml") && !(f.getName().equalsIgnoreCase(FIRST_DBCONTRIBUTION_FILE)) && !(f.getName().equalsIgnoreCase(MASTER_DBCONTRIBUTION_FILE))) {
            DBXmlDocument fXml = new DBXmlDocument(dirXml, f.getName());
            fXml.load();
            // v�rification des d�pendances
            // & prise en compte uniquement si dependences OK
            if (!checkRequired(listeFileXml, fXml)) {
              displayMessageln("\t" + f.getName() + " (because of unresolved requirements).");
              ignoredFiles++;
            } else if (ACTION_ENFORCE_UNINSTALL.equals(action)) {
              displayMessageln("\t" + f.getName() + " (because of " + ACTION_ENFORCE_UNINSTALL + " mode).");
              ignoredFiles++;
            } else {
              listeDBXmlDocument.add(fXml);
            }
          }
        }
        if (ignoredFiles == 0) {
          displayMessageln("\t(none)");
        }

        // pr�pare une HashMap des modules pr�sents en fichiers de contribution
        HashMap packagesIntoFile = new HashMap();
        // ATH � compl�ter ici algo de traitement de l'ordonnancement
        try {
          DBXmlDocument[] bidon = checkDependencies(listeDBXmlDocument);
        } catch (Exception e) {
          e.printStackTrace();
        }
        List<DBXmlDocument> orderedlisteDBXmlDocument = listeDBXmlDocument;

        int j = 0;
        displayMessageln(System.getProperty("line.separator") + "Merged contribution files are :");
        displayMessageln(action);
        if (!ACTION_ENFORCE_UNINSTALL.equals(action)) {
          displayMessageln("\t" + FIRST_DBCONTRIBUTION_FILE);
          j++;
        }
        for (DBXmlDocument currentDoc : orderedlisteDBXmlDocument) {
          displayMessageln("\t" + currentDoc.getName());
          j++;
        }
        if (j == 0) {
          displayMessageln("\t(none)");
        }
        // merge des diffrents fichiers de contribution �ligibles :
        displayMessageln(System.getProperty("line.separator") + "Build decisions are :");
        // d'abord le fichier dbbuilder-contribution ...
        DBXmlDocument fileXml = null;
        if (!ACTION_ENFORCE_UNINSTALL.equals(action)) {
          try {
            fileXml = new DBXmlDocument(dirXml, FIRST_DBCONTRIBUTION_FILE);
            fileXml.load();
          } catch (Exception e) {
            // contribution de dbbuilder non trouve -> on continue, on est certainement en train
            // de desinstaller la totale
            fileXml = null;
          }
          if (fileXml != null) {
            DBBuilderFileItem dbbuilderItem = new DBBuilderFileItem(fileXml);
            packagesIntoFile.put(dbbuilderItem.getModule(), null);
            mergeActionsToDo(dbbuilderItem, destXml, processesToCacheIntoDB, sqlMetaInstructions);
          }
        }

        // ... puis les autres
        for (DBXmlDocument currentDoc : orderedlisteDBXmlDocument) {
          DBBuilderFileItem tmpdbbuilderItem = new DBBuilderFileItem(currentDoc);
          packagesIntoFile.put(tmpdbbuilderItem.getModule(), null);
          mergeActionsToDo(tmpdbbuilderItem, destXml, processesToCacheIntoDB, sqlMetaInstructions);
        }

        // ... et enfin les pi�ces BD � d�sinstaller
        // ... attention, l'ordonnancement n'�tant pas dispo, on les traite dans
        // l'ordre inverse pour faire passer busCore a la fin, de nombreuses contraintes
        // des autres modules referencant les PK de busCore
        List itemsList = new ArrayList();

        boolean foundDBBuilder = false;
        Iterator iterPackagesIntoDB = packagesIntoDB.iterator();
        while (iterPackagesIntoDB.hasNext()) {
          String p = (String) iterPackagesIntoDB.next();
          if (!packagesIntoFile.containsKey(p)) {
            // Package en base et non en contribution -> candidat � desinstallation
            if ("dbbuilder".equalsIgnoreCase(p)) // le module a desinstaller est dbbuilder, on le garde sous le coude pour le traiter en dernier
            {
              foundDBBuilder = true;
            } else if (action.equals(ACTION_ENFORCE_UNINSTALL)) {
              if (p.equals(moduleName)) {
                itemsList.add(0, p);
              } else;
            } else {
              itemsList.add(0, p);
            }
          } 
        } 

        if (foundDBBuilder) {
          if (action.equals(ACTION_ENFORCE_UNINSTALL)) {
            if (moduleName.equals("dbbuilder")) {
              itemsList.add(itemsList.size(), "dbbuilder");
            } else;
          } else {
            itemsList.add(itemsList.size(), "dbbuilder");
          }
        }

        Iterator iterItems = itemsList.iterator();
        while (iterItems.hasNext()) {
          String p = (String) iterItems.next();
          displayMessageln( "**** Treating " + p + " ****");
          DBBuilderDBItem tmpdbbuilderItem = new DBBuilderDBItem(p);
          mergeActionsToDo(tmpdbbuilderItem, destXml, processesToCacheIntoDB, sqlMetaInstructions);
        } 

        // Trace
        destXml.setName("res.txt");
        destXml.save();

        displayMessageln(System.getProperty("line.separator") + "Build parts are :");

        // Traitement des pi�ces s�lectionn�es
        // remarque : durant cette phase, les erreurs sont trait�es -> on les catche en
        // retour sans les retraiter
        if (action.equals(ACTION_INSTALL)) {
          processDB(destXml, processesToCacheIntoDB, sqlMetaInstructions, TAGS_TO_MERGE_4_INSTALL);

        } else if (action.equals(ACTION_UNINSTALL) || action.equals(ACTION_ENFORCE_UNINSTALL)) {

          processDB(destXml, processesToCacheIntoDB, sqlMetaInstructions, TAGS_TO_MERGE_4_UNINSTALL);

        } else if (action.equals(ACTION_OPTIMIZE)) {

          processDB(destXml, processesToCacheIntoDB, sqlMetaInstructions, TAGS_TO_MERGE_4_OPTIMIZE);

        } else if (action.equals(ACTION_ALL)) {

          processDB(destXml, processesToCacheIntoDB, sqlMetaInstructions, TAGS_TO_MERGE_4_ALL);

        } else if (action.equals(ACTION_STATUS)) {
          // nothing to do
        } else if (action.equals(ACTION_CONSTRAINTS_INSTALL)) {
          // nothing to do
        } else if (action.equals(ACTION_CONSTRAINTS_UNINSTALL)) {
          // nothing to do
        }
        // Modules en place sur la BD en final
        displayMessageln(System.getProperty("line.separator") + "Finally DB Status :");
        checkDBStatus();

      } 

      displayMessageln(System.getProperty("line.separator") + "Database build successfully done(" + new java.util.Date() + ").");
      System.out.println(System.getProperty("line.separator") + "Database build successfully done (" + new java.util.Date() + ").");

    } catch (Exception e) {
      e.printStackTrace();
      printError(e.getMessage(), e);
      displayMessageln(e.getMessage());
      // e.printStackTrace();
      displayMessageln(System.getProperty("line.separator") + "Database build failed (" + new java.util.Date() + ").");
      System.out.println(System.getProperty("line.separator") + "Database build failed (" + new java.util.Date() + ").");
    } finally {
      bufLog.close();
    }
  } // main

  // ---------------------------------------------------------------------
  private static void testParams(String[] args) throws Exception {

    String usage = new String("DBBuilder usage: DBBuilder <action> -T <Targeted DB Server type> -D <JDBC Driver> -d <Datasource> -l <user login> -p <user Password> [-v(erbose)] [-s(imulate)]\n" +
        "where <action> == -C(onnection only) | -I(nstall only) | -U(ninstall only) | -O(ptimize only) | -A(ll) | -S(tatus) | -FU(Force Uninstall) <module> \n " +
        // "where <action> == -C(onnection only) | -I(nstall only) | -U(ninstall only) | -O(ptimize only) | -A(ll) | -S(tatus) | -FU(Force Uninstall) <module> | -CI(Contraints Install) | -CU(Constraints Unistall)\n" +
        "      <Targeted DB Server type> == MSSQL | ORACLE | POSTGRES\n");

    // Quelque chose � faire ?
//		if (args.length != 13) {
//			printError(usage);
//		} 

    boolean getDBType = false;
    boolean getDriver = false;
    boolean getDataSource = false;
    boolean getUserName = false;
    boolean getPassword = false;
    boolean getModuleName = false;

    for (int i = 0; i < args.length; i++) {
      String curArg = args[i];

      if (curArg.equals("-T")) {
        if (getDBType || getDataSource || getUserName || getPassword || getDriver || getModuleName) {
          printError(usage);
          throw new Exception();
        }
        if (dbType != null) {
          printError(usage);
          throw new Exception();
        }
        getDBType = true;
      } else if (curArg.equals("-D")) {
        if (getDBType || getDataSource || getUserName || getPassword || getDriver || getModuleName) {
          printError(usage);
          throw new Exception();
        }
        if (jdbcDriver != null) {
          printError(usage);
          throw new Exception();
        }
        getDriver = true;
      } else if (curArg.equals("-d")) {
        if (getDBType || getDataSource || getUserName || getPassword || getDriver || getModuleName) {
          printError(usage);
          throw new Exception();
        }
        if (dataSource != null) {
          printError(usage);
          throw new Exception();
        }
        getDataSource = true;
      } else if (curArg.equals("-l")) {
        if (getDBType || getDataSource || getUserName || getPassword || getDriver || getModuleName) {
          printError(usage);
          throw new Exception();
        }
        if (userName != null) {
          printError(usage);
          throw new Exception();
        }
        getUserName = true;
      } else if (curArg.equals("-p")) {
        if (getDBType || getDataSource || getUserName || getPassword || getDriver || getModuleName) {
          printError(usage);
          throw new Exception();
        }
        if (password != null) {
          printError(usage);
          throw new Exception();
        }
        getPassword = true;
      } else if (curArg.equals("-C") || curArg.equals("-I") || curArg.equals("-U") || curArg.equals("-O") || curArg.equals("-A") || curArg.equals("-S") || curArg.equals("-CI") || curArg.equals("-CU") || curArg.equals("-FU")) {
        if (getDBType || getDataSource || getUserName || getPassword || getDriver || getModuleName) {
          printError(usage);
          throw new Exception();
        }
        if (action != null) {
          printError(usage);
          throw new Exception();
        }
        action = curArg;
        if (curArg.equals("-FU")) {
          getModuleName = true;
        }
      } else if (curArg.equals("-v")) {
        if (getDBType || getDataSource || getUserName || getPassword || getDriver || getModuleName) {
          printError(usage);
          throw new Exception();
        }
        if (verbose != null) {
          printError(usage);
          throw new Exception();
        }
        verbose = new Boolean(true);
      } else if (curArg.equals("-s")) {
        if (getDBType || getDataSource || getUserName || getPassword || getDriver || getModuleName) {
          printError(usage);
          throw new Exception();
        }
        if (simulate != null) {
          printError(usage);
          throw new Exception();
        }
        simulate = new Boolean(true);
      } else {
        if (!getDBType && !getDataSource && !getUserName && !getPassword && !getDriver && !getModuleName) {
          printError(usage);
          throw new Exception();
        }
        if (getDBType) {
          dbType = curArg.toLowerCase();
        } else if (getDriver) {
          jdbcDriver = curArg;
        } else if (getDataSource) {
          dataSource = curArg;
        } else if (getUserName) {
          userName = curArg;
        } else if (getPassword) {
          password = curArg;
        } else if (getModuleName) {
          moduleName = curArg;
        }

        getDBType = false;
        getDriver = false;
        getDataSource = false;
        getUserName = false;
        getPassword = false;
        getModuleName = false;
      } 
    } 

    if (dbType == null || dataSource == null || userName == null || password == null || action == null) {
      printError(usage);
      throw new Exception();
    }

    if (moduleName == null && getModuleName) {
      printError(usage);
      throw new Exception();
    }

    if (verbose == null) {
      verbose = new Boolean(false);
    }
    if (simulate == null) {
      simulate = new Boolean(false);
    }

  } // testParams(String[] args)

  // ---------------------------------------------------------------------

   private static void printError(String errMsg, Exception ex) {
    printError(errMsg);
    if (bufLog != null) {
      ex.printStackTrace(bufLog);
      bufLog.close();
    }
  }
   
  private static void printError(String errMsg) {
    if (bufLog != null) {
      displayMessageln(System.getProperty("line.separator") + errMsg);
      bufLog.close();
    }
    System.out.println(System.getProperty("line.separator") + errMsg + System.getProperty("line.separator"));
//		System.exit( 1 );

  } // printError( String errMsg )

  // ---------------------------------------------------------------------
  public static void displayMessageln(String msg) {

    // displayMessage(msg + "\n");
    displayMessage(msg + System.getProperty("line.separator"));

  } // displayMessageln( String msg )

  // ---------------------------------------------------------------------
  private static void displayMessage(String msg) {

    if (bufLog != null) {
      bufLog.print(msg);
      System.out.print(".");
    } else {
      System.out.print(msg);
    }

  } // displayMessage( String msg )

  // ---------------------------------------------------------------------
  private static boolean checkRequired(File[] listeFileXml, DBXmlDocument fXml) {

    // liste des d�pendences
    Element root = fXml.getDocument().getRootElement(); // Get the root element
    List listeDependencies = root.getChildren(REQUIREMENT_TAG);

    if (listeDependencies != null) {

      Iterator iterDependencies = listeDependencies.iterator();
      while (iterDependencies.hasNext()) {

        Element eltDependencies = (Element) iterDependencies.next();
        List listeDependencyFiles = eltDependencies.getChildren(FILE_TAG);
        Iterator iterDependencyFiles = listeDependencyFiles.iterator();

        while (iterDependencyFiles.hasNext()) {

          Element eltDependencyFile = (Element) iterDependencyFiles.next();
          String name = eltDependencyFile.getAttributeValue(FILENAME_ATTRIB);

          boolean found = false;
          for (int i = 0; i < listeFileXml.length; i++) {
            File f = listeFileXml[i];

            if (f.getName().equals(name)) {
              found = true;
              i = listeFileXml.length;
            }
          }

          if (found == false) {
            return false;
          }
        } 

      } 

    } 

    return true;

  } // checkRequired(File[] listeFileXml, DBXmlDocument fXml)

  // ---------------------------------------------------------------------

  /* Construit une hashmap des d�pendances (hDep) :
  pour chaque item, la cl� est le nom du produit (ie module), et la valeur un vecteur de string, chacun
  �tant le nom du produit en d�pendance
   */
  private static DBXmlDocument[] checkDependencies(List<DBXmlDocument> tfXml) {
    Map hDep = new HashMap();
    for (DBXmlDocument fXml : tfXml) {
      if (fXml != null) {
        // liste des d�pendences
        Element root = fXml.getDocument().getRootElement(); // Get the root element
        String moduleNameAtt = root.getAttributeValue(DBBuilderFileItem.MODULENAME_ATTRIB);
        List<Element> listeDependencies = (List<Element>) root.getChildren(DEPENDENCY_TAG);
        List<String> aDependencies = new ArrayList<String>();
        if (listeDependencies != null) {
          int j = 0;
          for (Element eltDependencies : listeDependencies) {
            List<Element> listeDependencyFiles = (List<Element>) eltDependencies.getChildren(PRODUCT_TAG);
            Iterator iterDependencyFiles = listeDependencyFiles.iterator();
            for (Element eltDependencyFile : listeDependencyFiles) {
              String name = eltDependencyFile.getAttributeValue(PRODUCTNAME_ATTRIB);
              aDependencies.add(name);
              j++;
            }
          }
        }
        hDep.put(moduleNameAtt, aDependencies);
      }
    }
    return null;
  }

  //---------------------------------------------------------------------
  // Accesseurs
  //---------------------------------------------------------------------
  public static String getDbType() {
    return dbType;
  }

  public static Properties getdbBuilderResources() {
    return dbBuilderResources;
  }

  public static String getDataSource() {
    return dataSource;
  }

  public static String getUserName() {
    return userName;
  }

  public static String getPassword() {
    return password;
  }

  public static String getAction() {
    return action;
  }

  public static String getJdbcDriver() {
    return jdbcDriver;
  }

  public static boolean getVerbose() {
    return verbose.booleanValue();
  }

  public static boolean getSimulate() {
    return simulate.booleanValue();
  }

  public static String getDBContributionDir() {
    return DIR_CONTRIBUTIONFILESROOT.concat(File.separator + getDbType());
  }

  private static void mergeActionsToDo(DBBuilderItem pdbbuilderItem, DBXmlDocument xmlFile, Vector processesToCacheIntoDB, Vector<String> sqlMetaInstructions) {

    String package_name = pdbbuilderItem.getModule();
    String versionDB = null;
    String versionFile = null;
    try {
      versionDB = pdbbuilderItem.getVersionFromDB();
      versionFile = pdbbuilderItem.getVersionFromFile();
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }

    String[] tags_to_merge = null;
    VersionTag[] blocks_merge = null;

    if (pdbbuilderItem.getClass().getName().equals("com.silverpeas.dbbuilder.DBBuilderFileItem")) {

      DBBuilderFileItem dbbuilderItem = (DBBuilderFileItem) pdbbuilderItem;

      int iversionDB = -1;
      if (!versionDB.equals(DBBuilderFileItem.NOTINSTALLED)) {
        iversionDB = new Integer(versionDB).intValue();
      }
      int iversionFile = new Integer(versionFile).intValue();

      if (iversionDB == iversionFile) {
        if (getAction().equals(ACTION_INSTALL) || getAction().equals(ACTION_UNINSTALL) || getAction().equals(ACTION_STATUS) || getAction().equals(ACTION_CONSTRAINTS_INSTALL) || getAction().equals(ACTION_CONSTRAINTS_UNINSTALL)) {
          displayMessageln("\t" + package_name + " is up to date with version " + versionFile + ".");
        } else {
          displayMessageln("\t" + package_name + " is up to date with version " + versionFile + " and will be optimized.");
          tags_to_merge = TAGS_TO_MERGE_4_OPTIMIZE;
          blocks_merge = new VersionTag[1];
          blocks_merge[0] = new VersionTag(DBBuilderFileItem.CURRENT_TAG, versionFile);
        } 
      } else if (iversionDB > iversionFile) {
        displayMessageln("\t" + package_name + " will be ignored because this package is newer into DB than installed files.");
      } else {
        if (getAction().equals(ACTION_INSTALL) || getAction().equals(ACTION_ALL) || getAction().equals(ACTION_STATUS) || getAction().equals(ACTION_CONSTRAINTS_INSTALL) || getAction().equals(ACTION_CONSTRAINTS_UNINSTALL)) {
          if (iversionDB == -1) {
            displayMessageln("\t" + package_name + " will be installed with version " + versionFile + ".");
            tags_to_merge = TAGS_TO_MERGE_4_INSTALL;

            blocks_merge = new VersionTag[1];
            blocks_merge[0] = new VersionTag(DBBuilderFileItem.CURRENT_TAG, versionFile);

            // module nouvellement install� -> il faut stocker en base sa procedure de uninstall
            Object[] o = new Object[2];
            o[0] = package_name;
            o[1] = dbbuilderItem.getFileXml();
            processesToCacheIntoDB.add(o);

            // inscription du module en base
            sqlMetaInstructions.add("insert into SR_PACKAGES(SR_PACKAGE, SR_VERSION) values ('" + package_name + "', '" + versionFile + "')");
          } else {
            displayMessageln("\t" + package_name + " will be upgraded from " + versionDB + " to " + versionFile + ".");
            tags_to_merge = TAGS_TO_MERGE_4_INSTALL;

            blocks_merge = new VersionTag[iversionFile - iversionDB];
            for (int i = 0; i < iversionFile - iversionDB; i++) {
              String sversionFile = new String("000" + (iversionDB + i));
              sversionFile = sversionFile.substring(sversionFile.length() - 3);
              blocks_merge[i] = new VersionTag(DBBuilderFileItem.PREVIOUS_TAG, sversionFile);
            } 

            // module upgrad� -> il faut stocker en base sa nouvelle procedure de uninstall
            Object[] o = new Object[2];
            o[0] = package_name;
            o[1] = dbbuilderItem.getFileXml();
            processesToCacheIntoDB.add(o);

            // desinscription du module en base
            sqlMetaInstructions.add("update SR_PACKAGES set SR_VERSION='" + versionFile + "' where SR_PACKAGE='" + package_name + "'");
            sqlMetaInstructions.add("delete from SR_DEPENDENCIES where SR_PACKAGE = '" + package_name + "'");
            sqlMetaInstructions.add("delete from SR_SCRIPTS where SR_ITEM_ID IN (SELECT SRU.SR_ITEM_ID from SR_UNINSTITEMS SRU where SRU.SR_PACKAGE = '" + package_name + "')");
            sqlMetaInstructions.add("delete from SR_UNINSTITEMS where SR_PACKAGE = '" + package_name + "'");
          } 
        } else if (getAction().equals(ACTION_OPTIMIZE)) {
          displayMessageln("\t" + package_name + " will be optimized.");
          tags_to_merge = TAGS_TO_MERGE_4_OPTIMIZE;
          blocks_merge = new VersionTag[1];
          blocks_merge[0] = new VersionTag(DBBuilderFileItem.CURRENT_TAG, versionFile);
        } 

        // construction du xml global des actions d'upgrade de la base
        if (blocks_merge != null && tags_to_merge != null) {
          try {
            xmlFile.mergeWith(pdbbuilderItem, tags_to_merge, blocks_merge);
          } catch (Exception e) {
            e.printStackTrace();
          }
        } 
      } 


    } else if (pdbbuilderItem.getClass().getName().equalsIgnoreCase("com.silverpeas.dbbuilder.DBBuilderDBItem")) {

      if (getAction().equals(ACTION_UNINSTALL) || getAction().equals(ACTION_ALL) || getAction().equals(ACTION_ENFORCE_UNINSTALL)) {
        displayMessageln("\t" + package_name + " will be uninstalled.");
        tags_to_merge = TAGS_TO_MERGE_4_UNINSTALL;
        // desinscription du module de la base
        if (!package_name.equalsIgnoreCase("dbbuilder")) {
          System.out.println("delete from SR_");
          sqlMetaInstructions.add("delete from SR_DEPENDENCIES where SR_PACKAGE = '" + package_name + "'");
          sqlMetaInstructions.add("delete from SR_SCRIPTS where SR_ITEM_ID IN (SELECT SRU.SR_ITEM_ID from SR_UNINSTITEMS SRU where SRU.SR_PACKAGE = '" + package_name + "')");
          sqlMetaInstructions.add("delete from SR_UNINSTITEMS where SR_PACKAGE = '" + package_name + "'");
          sqlMetaInstructions.add("delete from SR_PACKAGES where SR_PACKAGE='" + package_name + "'");
        } 
        // construction du xml global des actions d'upgrade de la base
        if (tags_to_merge != null) {
          try {
            xmlFile.mergeWith(pdbbuilderItem, tags_to_merge, null);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      } 
      displayMessageln("");
      displayMessageln("*** AVERTISSEMENT ***");
      displayMessageln("\t Le Module " + package_name + " est pr�sent en BD mais n'a pas de scripts SQL fichiers");
      displayMessageln("");
      System.out.println("");
      System.out.println("*** AVERTISSEMENT ***");
      System.out.println("Le Module " + package_name + " est pr�sent en BD mais n'a pas de scripts SQL fichiers");
    }

  }

  private static void processDB(DBXmlDocument xmlFile, Vector processesToCacheIntoDB,
      Vector<String> sqlMetaInstructions, String[] tagsToProcess) throws Exception {

    // Ouvre la transaction de MAJ
    DBConnexion.getInstance().startTransaction();

    try {
      // ------------------------------------------
      // ETAPE 1 : TRAITEMENT DES ACTIONS D'UPGRADE
      // ------------------------------------------

      // Get the root element
      Element root = xmlFile.getDocument().getRootElement();

      // piece de DB Builder
      DBBuilderPiece p;

      for (int i = 0; i < tagsToProcess.length; i++) {
        // liste des pi�ces correspondant au i-eme tag a traiter
        List listeTags = root.getChildren(tagsToProcess[i]);
        int nbFiles = 0;

        Iterator iterTags = listeTags.iterator();
        while (iterTags.hasNext()) {
          Element eltTag = (Element) iterTags.next();
          String nomTag = eltTag.getName();

          // -------------------------------------
          // TRAITEMENT DES PIECES DE TYPE DB
          // -------------------------------------
          List listeRowFiles = eltTag.getChildren(DBBuilderItem.ROW_TAG);
          Iterator iterRowFiles = listeRowFiles.iterator();
          while (iterRowFiles.hasNext()) {
            Element eltFile = (Element) iterRowFiles.next();
            String name = eltFile.getAttributeValue(DBBuilderFileItem.FILENAME_ATTRIB);
            String value = eltFile.getAttributeValue(DBBuilderFileItem.FILETYPE_ATTRIB);
            Integer order = new Integer(eltFile.getAttributeValue(DBBuilderItem.DBORDER_ATTRIB));
            String delimiter = eltFile.getAttributeValue(DBBuilderFileItem.FILEDELIMITER_ATTRIB);
            String skeepdelimiter = eltFile.getAttributeValue(DBBuilderFileItem.FILEKEEPDELIMITER_ATTRIB);
            String dbprocname = eltFile.getAttributeValue(DBBuilderFileItem.FILEDBPROCNAME_ATTRIB);
            boolean keepdelimiter = (skeepdelimiter != null && skeepdelimiter.equals("YES"));
            //String classname = eltFile.getAttributeValue( DBBuilderFileItem.FILECLASSNAME_ATTRIB );
            //String methodname = eltFile.getAttributeValue( DBBuilderFileItem.FILEMETHODNAME_ATTRIB );

            displayMessageln("\t" + tagsToProcess[i] + " : internal-id : " + name + "\t type : " + value);
            nbFiles++;

            if (value.equals(DBBuilderFileItem.FILEATTRIBSTATEMENT_VALUE)) {
              // piece de type Single Statement
              p = new DBBuilderSingleStatementPiece(name, name + "(" + order + ")", nomTag, order.intValue(), verbose.booleanValue());
              // p.traceInstructions();
              p.executeInstructions();
            } else if (value.equals(DBBuilderFileItem.FILEATTRIBSEQUENCE_VALUE)) {
              // piece de type Single Statement
              p = new DBBuilderMultipleStatementPiece(name, name + "(" + order + ")", nomTag, order.intValue(), verbose.booleanValue(), delimiter, keepdelimiter);
              // p.traceInstructions();
              p.executeInstructions();
            } else if (value.equals(DBBuilderFileItem.FILEATTRIBDBPROC_VALUE)) {
              // piece de type Database Procedure
              p = new DBBuilderDBProcPiece(name, name + "(" + order + ")", nomTag, order.intValue(), verbose.booleanValue(), dbprocname);
              // p.traceInstructions();
              p.executeInstructions();
            }
          }

          // -------------------------------------
          // TRAITEMENT DES PIECES DE TYPE FICHIER
          // -------------------------------------
          List listeFiles = eltTag.getChildren(DBBuilderFileItem.FILE_TAG);
          Iterator iterFiles = listeFiles.iterator();
          while (iterFiles.hasNext()) {
            Element eltFile = (Element) iterFiles.next();
            String name = eltFile.getAttributeValue(DBBuilderFileItem.FILENAME_ATTRIB);

            if (File.separator.equals("/")) {
              name = StringUtil.sReplace("\\", "/", name);
            } else {
              name = StringUtil.sReplace("/", "\\", name);
            }

            String value = eltFile.getAttributeValue(DBBuilderFileItem.FILETYPE_ATTRIB);
            String delimiter = eltFile.getAttributeValue(DBBuilderFileItem.FILEDELIMITER_ATTRIB);
            String skeepdelimiter = eltFile.getAttributeValue(DBBuilderFileItem.FILEKEEPDELIMITER_ATTRIB);
            String dbprocname = eltFile.getAttributeValue(DBBuilderFileItem.FILEDBPROCNAME_ATTRIB);
            boolean keepdelimiter = (skeepdelimiter != null && skeepdelimiter.equals("YES"));
            String classname = eltFile.getAttributeValue(DBBuilderFileItem.FILECLASSNAME_ATTRIB);
            String methodname = eltFile.getAttributeValue(DBBuilderFileItem.FILEMETHODNAME_ATTRIB);

            displayMessageln("\t" + tagsToProcess[i] + " : name : " + name + "\t type : " + value);
            nbFiles++;

            if (value.equals(DBBuilderFileItem.FILEATTRIBSTATEMENT_VALUE)) {
              // piece de type Single Statement
              p = new DBBuilderSingleStatementPiece(DIR_DBPIECESFILESROOT + File.separator + name, nomTag, verbose.booleanValue());
              //p.traceInstructions();
              p.executeInstructions();

            } else if (value.equals(DBBuilderFileItem.FILEATTRIBSEQUENCE_VALUE)) {

              // piece de type Single Statement
              p = new DBBuilderMultipleStatementPiece(DIR_DBPIECESFILESROOT + File.separator + name, nomTag, verbose.booleanValue(), delimiter, keepdelimiter);
              // p.traceInstructions();
              p.executeInstructions();

            } else if (value.equals(DBBuilderFileItem.FILEATTRIBDBPROC_VALUE)) {

              // piece de type Database Procedure
              p = new DBBuilderDBProcPiece(DIR_DBPIECESFILESROOT + File.separator + name, nomTag, verbose.booleanValue(), dbprocname);
              // p.traceInstructions();
              p.executeInstructions();

            } else if (value.equals(DBBuilderFileItem.FILEATTRIBJAVALIB_VALUE)) {

              // piece de type Java invoke
              p = new DBBuilderDynamicLibPiece(DIR_DBPIECESFILESROOT + File.separator + name, nomTag, verbose.booleanValue(), classname, methodname);
              // p.traceInstructions();
              p.executeInstructions();
            }
          }
        }

        if (nbFiles == 0) {
          displayMessageln("\t" + tagsToProcess[i] + " : (none)");
        }
      } 

      // Mise � jour des versions en base
      if (sqlMetaInstructions.size() == 0) {
        displayMessageln("\tdbbuilder meta base maintenance : (none)");
      } else {
        displayMessageln("\tdbbuilder meta base maintenance :");
        String instructions = new String();
        for (String instruction : sqlMetaInstructions) {
          instructions = instructions.concat(instruction + "\nGO");
        }
        p = new DBBuilderMultipleStatementPiece("DBBuilder meta base", "DBBuilder meta base", instructions, verbose.booleanValue(), "\nGO", false);
        // p.traceInstructions();
        p.executeInstructions();
      }

      // ------------------------------------------------------
      // ETAPE 2 : CACHE EN BASE DES PROCESS DE DESINSTALLATION
      // ------------------------------------------------------

      displayMessageln(System.getProperty("line.separator") + "Uninstall stored parts are :");
      String[] tagsToProcessU = TAGS_TO_MERGE_4_UNINSTALL;
      for (int ip = 0; ip < processesToCacheIntoDB.size(); ip++) {

        Object[] o = (Object[]) processesToCacheIntoDB.get(ip);
        String pName = (String) o[0];
        DBXmlDocument xFile = (DBXmlDocument) o[1];

        // Get the root element
        Element rootU = xFile.getDocument().getRootElement();
        int nbFilesU = 0;

        // piece de DB Builder
        DBBuilderPiece pU;

        for (int i = 0; i < tagsToProcessU.length; i++) {

          // liste des pi�ces correspondant au i-eme tag a traiter
          List listeTagsCU = rootU.getChildren(DBBuilderFileItem.CURRENT_TAG);
          Iterator iterTagsCU = listeTagsCU.iterator();
          while (iterTagsCU.hasNext()) {
            Element eltTagCU = (Element) iterTagsCU.next();
            List listeTagsU = eltTagCU.getChildren(tagsToProcessU[i]);
            Iterator iterTagsU = listeTagsU.iterator();
            while (iterTagsU.hasNext()) {
              Element eltTagU = (Element) iterTagsU.next();
              List listeFilesU = eltTagU.getChildren(DBBuilderFileItem.FILE_TAG);
              Iterator iterFilesU = listeFilesU.iterator();
              int iFile = 1;
              while (iterFilesU.hasNext()) {
                Element eltFileU = (Element) iterFilesU.next();
                String nameU = eltFileU.getAttributeValue(DBBuilderFileItem.FILENAME_ATTRIB);

                if (File.separator.equals("/")) {
                  nameU = StringUtil.sReplace("\\", "/", nameU);
                } else {
                  nameU = StringUtil.sReplace("/", "\\", nameU);
                }
                String valueU = eltFileU.getAttributeValue(DBBuilderFileItem.FILETYPE_ATTRIB);
                String delimiterU = eltFileU.getAttributeValue(DBBuilderFileItem.FILEDELIMITER_ATTRIB);
                String skeepdelimiterU = eltFileU.getAttributeValue(DBBuilderFileItem.FILEKEEPDELIMITER_ATTRIB);
                String dbprocnameU = eltFileU.getAttributeValue(DBBuilderFileItem.FILEDBPROCNAME_ATTRIB);
                boolean keepdelimiterU = (skeepdelimiterU != null && skeepdelimiterU.equals("YES"));
                displayMessageln("\t" + tagsToProcessU[i] + " : name : " + nameU + "\t type : " + valueU);
                if (valueU.equals(DBBuilderFileItem.FILEATTRIBSTATEMENT_VALUE)) {
                  // piece de type Single Statement
                  pU = new DBBuilderSingleStatementPiece(DIR_DBPIECESFILESROOT + File.separator + nameU, tagsToProcessU[i], verbose.booleanValue());
                  pU.cacheIntoDB(pName, iFile);
                } else if (valueU.equals(DBBuilderFileItem.FILEATTRIBSEQUENCE_VALUE)) {
                  // piece de type Single Statement
                  pU = new DBBuilderMultipleStatementPiece(DIR_DBPIECESFILESROOT + File.separator + nameU, tagsToProcessU[i], verbose.booleanValue(), delimiterU, keepdelimiterU);
                  pU.cacheIntoDB(pName, iFile);
                } else if (valueU.equals(DBBuilderFileItem.FILEATTRIBDBPROC_VALUE)) {
                  // piece de type Database Procedure
                  pU = new DBBuilderDBProcPiece(DIR_DBPIECESFILESROOT + File.separator + nameU, tagsToProcessU[i], verbose.booleanValue(), dbprocnameU);
                  pU.cacheIntoDB(pName, iFile);
                }
                iFile++;
                nbFilesU++;
              } 
            } 
          }
          if (nbFilesU == 0) {
            displayMessageln("\t" + tagsToProcessU[i] + " : (none)");
          }
        } 
      } 

    } catch (Exception e) {
      // rollback
      DBConnexion.getInstance().abort();
      throw e;
    }

    displayMessageln(System.getProperty("line.separator") + "DB Status after build :");
    checkDBStatus();

    // commit ou rollback en fonction du param�tre
    if (simulate.booleanValue()) {
      DBConnexion.getInstance().abort();
      displayMessageln(System.getProperty("line.separator") + "Build rollbacked because of simulate mode.");
    } else {
      DBConnexion.getInstance().commit();
      displayMessageln(System.getProperty("line.separator") + "Build commited.");
    } 
  }

  // liste des packages en base
  private static Vector checkDBStatus() {

    Vector packagesIntoDB = new Vector();

    String selectAllPackagesFromDB = "select SR_PACKAGE as package, SR_VERSION as version from SR_PACKAGES order by SR_PACKAGE";
    List packageList = null;
    try {
      packageList = con.executeLoopQuery(selectAllPackagesFromDB);
    } catch (Exception e) {
      // displayMessageln( "\tIgnore this unfatal error due to empty database." );
    }
    if (packageList == null) {
      displayMessageln("\tNo package installed into DB.");
    } else {
      int nbValues = packageList.size();
      if (nbValues < 1) {
        displayMessageln("\tNo package installed into DB.");
      } else {
        for (int i = 0; i < nbValues; i++) {
          HashMap h = (HashMap) packageList.get(i);
          String srPackage = "(null)";
          if (h.containsKey("package")) //MSSSQL et POSTGRES
          {
            srPackage = (String) h.get("package");
            packagesIntoDB.add(srPackage);
          } else if (h.containsKey("PACKAGE")) //ORACLE
          {
            srPackage = (String) h.get("PACKAGE");
            packagesIntoDB.add(srPackage);
          } 
          String srVersion = "(null)";
          if (h.containsKey("version")) //MSSSQL et POSTGRES
          {
            srVersion = (String) h.get("version");
          } else if (h.containsKey("VERSION")) //ORACLE
          {
            srVersion = (String) h.get("VERSION");
          }

          displayMessageln("\t" + srPackage + " v. " + srVersion);
        } 
      }
    } 
    return packagesIntoDB;
  }

  // R�cup�re le r�pertoire racine d'installation
  public static String getHome() {

    if (dbbuilderHome == null) {

      if (!System.getProperties().containsKey(HOME_KEY)) {

        System.err.println("### CANNOT FIND DBBUILDER INSTALL LOCATION ###");
        System.err.println("please use \"-D" + HOME_KEY + "=<install location>\" on the command line");
        System.exit(1);
      } 

      dbbuilderHome = System.getProperty(HOME_KEY);
    } 

    return dbbuilderHome;
  }

  // R�cup�re le r�pertoire data d'installation
  public static String getData() {

    if (dbbuilderData == null) {
      if (System.getProperties().containsKey(DATA_KEY)) {
        dbbuilderData = System.getProperty(DATA_KEY);
      }
    }

    return dbbuilderData;
  }

  // R�cup�re le r�pertoire temp
  public static String getTemp() {

    return DIR_TEMP;
  }
}