package main.pt.ipleiria.estg.dei.labcif.bhh.modules;

import main.pt.ipleiria.estg.dei.labcif.bhh.database.DataWarehouseConnection;
import main.pt.ipleiria.estg.dei.labcif.bhh.exceptions.BrowserHistoryIngestModuleExpection;
import main.pt.ipleiria.estg.dei.labcif.bhh.exceptions.ConnectionException;
import main.pt.ipleiria.estg.dei.labcif.bhh.exceptions.NoCriticalException;
import main.pt.ipleiria.estg.dei.labcif.bhh.models.OperatingSystem;
import main.pt.ipleiria.estg.dei.labcif.bhh.utils.LoggerBHH;
import main.pt.ipleiria.estg.dei.labcif.bhh.utils.Utils;
import org.sleuthkit.autopsy.casemodule.Case;
import org.sleuthkit.autopsy.casemodule.NoCurrentCaseException;
import org.sleuthkit.autopsy.casemodule.services.FileManager;
import org.sleuthkit.autopsy.datamodel.ContentUtils;
import org.sleuthkit.autopsy.ingest.IngestJobContext;
import org.sleuthkit.datamodel.*;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import static main.pt.ipleiria.estg.dei.labcif.bhh.models.OperatingSystem.LINUX;
import static main.pt.ipleiria.estg.dei.labcif.bhh.models.OperatingSystem.WINDOWS_10;
import static main.pt.ipleiria.estg.dei.labcif.bhh.utils.OperatingSystemUtils.USER;
import static main.pt.ipleiria.estg.dei.labcif.bhh.utils.OperatingSystemUtils.getOS;


public abstract class BrowserModule extends Module {
    private Case currentCase;
    private LoggerBHH<BrowserModule> loggerBHH = new LoggerBHH<>(BrowserModule.class);
    private IngestJobContext context;
    private Content dataSource;
    private List<File> historyFilesFound;
    private List<File> loginFilesFound;

    protected BrowserModule(IngestJobContext context, String databaseDirectory) {
        super(databaseDirectory);
        try {
            currentCase = Case.getCurrentCaseThrows();
            this.context = context;
        } catch (NoCurrentCaseException e) {
            loggerBHH.error("Case couldn't be find. So state couldn't be initialized");
            throw new BrowserHistoryIngestModuleExpection("Case couldn't be find. So state couldn't be initialized");
        }
    }

    /**
     * Constructor used to initialize module without autopsy*/
    BrowserModule(String databaseDirectory) {
        super(databaseDirectory);
        historyFilesFound = new ArrayList<>();
        loginFilesFound = new ArrayList<>();
    }
    @Override
    public void run(Content dataSource) throws ConnectionException {
        loggerBHH.info("[" + getModuleName() +"] - Started");
        this.dataSource = dataSource;
        runHistory();
        loggerBHH.info("[" + getModuleName() +"] - Finished");
    }

    /**
     * This method is used to run module on the own machine of the one being executed*/
    @Override
    public void run(String caseDirectory) throws ConnectionException {
        loggerBHH.info("[" + getModuleName() +"] - Started");
        historyFilesFound.clear();
        loginFilesFound.clear();
        runOwnMachine(caseDirectory);
        loggerBHH.info("[" + getModuleName() +"] - Finished");
    }

    private void runOwnMachine(String caseDirectory)  {
        File originalDirectoryOfBrowser = new File(getFullPathToBrowserInstallationInCurrentMachine());
        findAllHistoryFilesInParentDirectory(originalDirectoryOfBrowser);
        String temporaryDirectory = Utils.createDirectoryIfNotExists(caseDirectory + "/temp");

        loggerBHH.info("[" + getModuleName() +"]" + " - Found " + historyFilesFound.size() + " different profiles" );

        List<File> filesToProcess = new ArrayList<>(historyFilesFound);
        filesToProcess.addAll(loginFilesFound);

        filesToProcess.forEach(file -> {
            try {
                String profileName = file.getParentFile().getName().trim();
                String temporaryDatabaseFile = temporaryDirectory + File.separator + file.getName() + "-" + USER +"-" + profileName + ".db";
                Utils.copyFile(file.getAbsolutePath(), temporaryDatabaseFile);
                runETLProcess(temporaryDatabaseFile, USER, profileName, file.getAbsolutePath(), getOS());

            } catch (IOException | ConnectionException e) {
                loggerBHH.error(e.getMessage());
            }
        });
    }


    private void runHistory() throws ConnectionException {
        try {
            FileManager fileManager = currentCase.getServices().getFileManager();

            execute(fileManager.findFiles(dataSource,  getHistoryFilename(), getPathToBrowserInstallationInWindows10()),
                    getHistoryFilename(),
                    WINDOWS_10);
            execute(fileManager.findFiles(dataSource,  getHistoryFilename(), getPathToBrowserInstallationInLinux()),
                    getHistoryFilename(),
                    LINUX);

            execute(fileManager.findFiles(dataSource, getLoginDataFilename(), getPathToBrowserInstallationInWindows10()),
                    getLoginDataFilename(),
                    WINDOWS_10);

            execute(fileManager.findFiles(dataSource, getLoginDataFilename(), getPathToBrowserInstallationInLinux()),
                    getLoginDataFilename(),
                    LINUX);
        } catch (TskCoreException e) {
            loggerBHH.warn("[" + getModuleName() +"] Issue when running: " + e.getMessage());
        }
    }
    private void execute(List<AbstractFile> files, String prefixName, OperatingSystem operatingSystem) throws ConnectionException {
        for (AbstractFile file: files) {
            String username = file.getParentPath().split("/")[2];//TODO:::: review this line of code.,...
            String fullLocationFile = file.getParentPath() + prefixName;
            //We have to copy this file to the temp directory
            String tempPath = getTempPath(currentCase, getModuleName()) + File.separator + prefixName + "-" + username + ".db";
            try {
                String profileName = file.getParent().getName();
                ContentUtils.writeToFile(file, new File(tempPath), context::dataSourceIngestIsCancelled);
                runETLProcess(tempPath, username, profileName, fullLocationFile, operatingSystem);
            } catch (IOException | NoCriticalException | TskCoreException e) {
                loggerBHH.warn(e.getMessage());//We don't want to stop the process when it is a non critical exception
            }
        }
    }

    protected void insertWordInTable(ResultSet rs, String user, String profileName, String fullLocationFile, OperatingSystem os) throws ConnectionException, SQLException, ClassNotFoundException {
        PreparedStatement preparedStatement =  DataWarehouseConnection.getConnection(databaseDirectory).prepareStatement(
                " INSERT INTO t_clean_search_in_engines (search_in_engines_words, search_in_engines_source_full, " +
                                                "search_in_engines_user_origin, search_in_engines_browser_origin," +
                                                " search_in_engines_domain, search_profile_name, search_filename_location," +
                                                "search_visit_full_date, search_visit_date, search_visit_time, search_operating_system) " +
                        " VALUES (?,?,?,?,?,?,?,?,?,?,?)");

        String encoded;
        String substring;
        String words;

        while (rs.next()) {
            encoded = rs.getString("word");
            substring = encoded.substring(0, encoded.contains("&") ? encoded.indexOf("&") : encoded.length() -1);

            //In case the string has been decoded already
            try {
                words = URLDecoder.decode(substring, "UTF-8"); }
            catch(Exception ex) {
                words = substring;
            }
            preparedStatement.setString(1, words);
            preparedStatement.setString(2, rs.getString("url_full"));
            preparedStatement.setString(3, user);
            preparedStatement.setString(4, getModuleName());
            preparedStatement.setString(5, rs.getString("url_domain"));
            preparedStatement.setString(6, profileName);
            preparedStatement.setString(7, fullLocationFile);
            preparedStatement.setString(8, rs.getString("search_visit_full_date"));
            preparedStatement.setString(9, rs.getString("search_visit_date"));
            preparedStatement.setString(10, rs.getString("search_visit_time"));
            preparedStatement.setString(11, os.name());
            preparedStatement.addBatch();

        }

        preparedStatement.executeBatch();
    }

    public String extractDomainFromFullUrlInSqliteQuery(String fullUrl, String newColumnName) {
        return "replace( SUBSTR( substr(" + fullUrl + ", instr(" + fullUrl+ ", '://')+3), 0, " +
                "instr(substr(" + fullUrl +", instr(" + fullUrl + ", '://')+3),'/')), 'www.', '') as " + newColumnName;
    }

    private void findAllHistoryFilesInParentDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            Arrays.stream(files).forEach(file -> {
                if (file.isDirectory()) {
                    findAllHistoryFilesInParentDirectory(file);
                } else if (file.getName().equals(getHistoryFilename())){
                    historyFilesFound.add(file);
                } else if (file.getName().equals(getLoginDataFilename())){
                    loginFilesFound.add(file);
                }
            });
        }
    }
    static Map<Long, String> getOperatingSystems() {
        Map<Long, String> osDetailMap = new HashMap<>();
        try {
            SleuthkitCase skCase = Case.getCurrentCaseThrows().getSleuthkitCase();
            ArrayList<BlackboardArtifact> osInfoArtifacts = skCase.getBlackboardArtifacts(BlackboardArtifact.ARTIFACT_TYPE.TSK_OS_INFO);
            for (BlackboardArtifact osInfo : osInfoArtifacts) {
                BlackboardAttribute programName = osInfo.getAttribute(new BlackboardAttribute.Type(BlackboardAttribute.ATTRIBUTE_TYPE.TSK_PROG_NAME));
                if (programName != null) {
                    String currentOsString = osDetailMap.get(osInfo.getDataSource().getId());
                    if (currentOsString == null || currentOsString.isEmpty()) {
                        currentOsString = programName.getValueString();
                    } else {
                        currentOsString = currentOsString + ", " + programName.getValueString();
                    }
                    osDetailMap.put(osInfo.getDataSource().getId(), currentOsString);
                }
            }
        } catch (TskCoreException | NoCurrentCaseException ex) {
        }
        return osDetailMap;
    }

    protected abstract String extractDateFromColumn(String oldColumn, String newColumn, String format);
    public abstract String getPathToBrowserInstallationInWindows10();
    public abstract String getPathToBrowserInstallationInLinux();
    public abstract String getFullPathToBrowserInstallationInCurrentMachine();
    public abstract String getHistoryFilename();
    public abstract String getLoginDataFilename();

    public abstract ResultSet transformWordsFromGoogle(Statement statement) throws SQLException;
    public abstract ResultSet transformWordsFromYahoo(Statement statement) throws SQLException;
    public abstract ResultSet transformWordsFromBing(Statement statement) throws SQLException;
    public abstract ResultSet transformWordsFromAsk(Statement statement) throws SQLException;

}
