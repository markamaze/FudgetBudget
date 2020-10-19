package com.fudgetbudget;

import com.fudgetbudget.model.ProjectedTransaction;
import com.fudgetbudget.model.RecordedTransaction;
import com.fudgetbudget.model.Transaction;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class StorageControl {
    private final String filesDir;

    public StorageControl(String filesDirectory) { this.filesDir = filesDirectory; }

    public Transaction getTransaction(String path){
        return Transaction.getInstance( getDocumentFromFile( new File( path ) ) );
    }
    public ArrayList<Transaction> readTransactions(Object type){
        File[] transactions_file_set = readAppFile("app_transactions").listFiles();
        ArrayList<Transaction> found_transactions = new ArrayList<>(  );

        if( type instanceof String ){
            if(type.toString().contentEquals( "all_active" ))
                Arrays.asList(transactions_file_set).stream().forEach( file -> {
                    Transaction transaction = Transaction.getInstance( getDocumentFromFile( file ) );
                    if( transactionIsActive(transaction) ) found_transactions.add( transaction );
                });
        }
        else if( type instanceof Integer ){
            if( (int)type == R.string.transaction_type_income )
                Arrays.asList(transactions_file_set).stream().forEach( file -> {
                    Transaction transaction = Transaction.getInstance( getDocumentFromFile( file ) );
                    if(transactionIsActive( transaction ) && transaction.getIncomeFlag())
                        found_transactions.add( transaction );
                } );

            else if( (int)type == R.string.transaction_type_expense )
                Arrays.asList(transactions_file_set).stream().forEach( file -> {
                    Transaction transaction = Transaction.getInstance( getDocumentFromFile( file ) );

                    if(transactionIsActive( transaction) && !transaction.getIncomeFlag())
                        found_transactions.add( transaction );
                } );
            else if( (int)type == R.string.transaction_type_unscheduled )
                Arrays.asList( transactions_file_set ).stream().forEach( file -> {
                    Transaction transaction = Transaction.getInstance( getDocumentFromFile( file ) );
                    if( !transactionIsActive( transaction )) found_transactions.add( transaction );
                } );

        }
        //this seems pointless: why read a transaction where the search parameter is the transaction
        else if( type instanceof Transaction ){
            File transactionFile = new File(((Transaction)type).getPath().getPath());
            Transaction transaction = Transaction.getInstance( getDocumentFromFile( transactionFile ) );
            if(transactionFile.exists()) found_transactions.add( transaction );
        }

        return found_transactions;
    }
    private boolean transactionIsActive(Transaction transaction){
        LocalDate date = (LocalDate) transaction.getProperty( R.string.date_tag );
        if(date == null) return false;
        return true;
    }


    public LinkedHashMap<LocalDate, ProjectedTransaction> readProjections(Transaction transaction){
        LinkedHashMap<LocalDate, ProjectedTransaction> projections = new LinkedHashMap<>(  );

        File transactionFile = new File(transaction.getPath().getPath());
        Document transactionDocument = getDocumentFromFile( transactionFile );

        if(transactionDocument == null) return projections;

        NodeList storedProjections = transactionDocument.getElementsByTagName("projections").item( 0 ).getChildNodes();
        for(int i = 0; i < storedProjections.getLength(); i++){
            Element projectionElement = (Element) storedProjections.item( i );
            ProjectedTransaction projectedTransaction = ProjectedTransaction.getInstance( transaction, projectionElement );
            projections.put( projectedTransaction.getScheduledProjectionDate(), projectedTransaction );
        }

        return projections;
    }
    public ArrayList<RecordedTransaction> readRecords(Object object){
        File[] recordsFiles = readAppFile("app_records").listFiles();
        ArrayList<RecordedTransaction> foundRecords = new ArrayList<>(  );

        if(recordsFiles == null || recordsFiles.length <= 0) return foundRecords;

        if(object instanceof Integer && Integer.parseInt( object.toString() ) == R.string.records_type_all)
            Arrays.stream(recordsFiles).forEach( file -> {
                Element recordDocument = getDocumentFromFile( file ).getDocumentElement();
                File transactionFile = new File( recordDocument.getAttribute( "transactionURI" ));
                Transaction transaction = Transaction.getInstance( getDocumentFromFile( transactionFile ) );

                NodeList recordNodes = recordDocument.getChildNodes();

                for(int i = 0; i < recordNodes.getLength(); i++){
                    RecordedTransaction recordedTransaction = RecordedTransaction.getInstance( (Element) recordNodes.item( i ), transaction );
                    foundRecords.add( recordedTransaction );
                }
            } );
        else if( object instanceof Transaction ) {
            Transaction transaction = (Transaction) object;
            Document transactionRecords = null;
            int index = 0;
            while(transactionRecords == null && index < recordsFiles.length){
                Document recordDocument = getDocumentFromFile( recordsFiles[index] );
                URI transactionURI = URI.create( recordDocument.getDocumentElement().getAttribute( "transactionURI" ));

                if(transactionURI == transaction.getPath() ) transactionRecords = recordDocument;
                index++;
            }

            NodeList recordNodes = transactionRecords.getChildNodes();
            for( int i = 0; i < recordNodes.getLength(); i++){
                RecordedTransaction recordedTransaction = RecordedTransaction.getInstance( (Element) recordNodes.item( i ), transaction );
                foundRecords.add( recordedTransaction );
            }
        }


        Collections.sort(foundRecords);
        return foundRecords;
    }
    public Object readSettingsValue(String setting) {
        File settingsFile = readAppFile( "settings" );
        Document settingsDocument = getDocumentFromFile( settingsFile );

        NodeList settingsParameters = settingsDocument.getElementsByTagName( "settings_parameter" );
        int index = 0;

        while( index < settingsParameters.getLength()){
            Element settingItem = (Element) settingsParameters.item( index );
            if(settingItem.getAttribute( "key" ).contentEquals( setting ))
                return settingItem.getTextContent();
            index++;
        }
        return null;
    }

    public boolean writeTransaction(Transaction transaction){
        File transactionFile = new File(transaction.getPath().getPath());
        Document transactionDocument;

        if(!transactionFile.exists()){
            transactionFile.setReadable( true );
            transactionFile.setWritable( true );
            try { transactionFile.createNewFile(); }
            catch (IOException e) { e.printStackTrace(); return false; }
            transactionDocument = getNewTransactionDocument(transaction);
        } else transactionDocument = getDocumentFromFile( transactionFile );

        updateTransactionProperties(transactionDocument, transaction);

        return writeDocumentToFile( transactionDocument );
    }
    public boolean writeProjection(ProjectedTransaction projectedTransaction){
        File transactionFile = new File(projectedTransaction.getPath().getPath());
        Document transactionDocument;
        if(!transactionFile.exists()) return false;
        else transactionDocument = getDocumentFromFile( transactionFile );

        updateStoredProjection( transactionDocument, projectedTransaction );

        return writeDocumentToFile( transactionDocument );
    }
    public boolean writeRecord(RecordedTransaction recordedTransaction){
        String[] uri = recordedTransaction.getPath().getPath().split( "/" );
        uri[uri.length - 2] = "app_records";

        File recordFile = new File(String.join( "/", uri ));
        Document recordDocument = getDocumentFromFile( recordFile );

        if(recordDocument == null) recordDocument = getNewRecordDocument( recordedTransaction );
        updateRecord( recordDocument, recordedTransaction );

        return writeDocumentToFile( recordDocument );
    }
    public boolean writeSettingsValue(String key, String value) {
        File settingsFile = readAppFile( "settings" );
        Document settingsDocument = getDocumentFromFile( settingsFile );

        if(settingsDocument == null) settingsDocument = getNewSettingsDocument();
        updateSettings( settingsDocument, key, value );

        return writeDocumentToFile( settingsDocument );
    }

    public boolean deleteProjection(ProjectedTransaction projection) {
        File transactionFile = new File(projection.getPath().getPath());
        Document transactionDocument;
        if(!transactionFile.exists()) return false;
        else transactionDocument = getDocumentFromFile( transactionFile );

        NodeList storedProjectionsNode = (transactionDocument.getElementsByTagName( "projections" ));
        Element projectionElement;
        NodeList storedProjections;
        Element foundProjection = null;

        if(storedProjectionsNode == null || storedProjectionsNode.getLength() < 1) return false;

        projectionElement = (Element) storedProjectionsNode.item( 0 );
        storedProjections = projectionElement.getChildNodes();


        int index = 0;
        while(foundProjection == null && index < storedProjections.getLength()){
            Element storedProjection = (Element) storedProjections.item( index );
            String storedDateString = storedProjection.getAttribute( "scheduledProjectionDate" );
            LocalDate storedProjectionDate = LocalDate.parse( storedDateString, DateTimeFormatter.BASIC_ISO_DATE );
            LocalDate scheduledProjectionDate = projection.getScheduledProjectionDate();

            if( storedProjectionDate.isEqual( scheduledProjectionDate ) ) foundProjection = storedProjection;
            index++;
        }

        if(foundProjection != null) projectionElement.removeChild( foundProjection );

        writeDocumentToFile( transactionDocument );

        return true;
    }
    public boolean deleteRecord(RecordedTransaction record) {
        String[] recordPath = (record.getPath().getPath()).split( "/" );
        recordPath[recordPath.length - 2] = "app_records";
        File recordFile = new File(String.join( "/", recordPath ));

        if(!recordFile.exists()) return false;

        Document recordDocument = getDocumentFromFile( recordFile );
        NodeList records = recordDocument.getDocumentElement().getChildNodes();
        Element foundRecord = null;

        int index = 0;
        while( foundRecord == null && index < records.getLength()){
            Element storedRecord = (Element) records.item( index );
            UUID storedRecordId = UUID.fromString( storedRecord.getAttribute( "record_id" ) );
            UUID recordId = record.getRecordId();
            if( storedRecordId.compareTo( recordId ) == 0) foundRecord = storedRecord;
            index++;
        }

        if(foundRecord != null) recordDocument.getDocumentElement().removeChild( foundRecord );

        return writeDocumentToFile( recordDocument );
    }
    public boolean deleteTransaction(Transaction object) {
        //todo: handle problem with this implementation
        //  problem -> i'm still not certain how records should be stored, but if stored within the Transaction Document,
        //              then I need to differentiate between active transactions, and those which only are storing records

        File transactionFile = new File(object.getPath().getPath());

        if(transactionFile.exists()) return transactionFile.delete();
        else return false;
    }

    private void updateTransactionProperties(Document transactionDocument, Transaction transaction) {
        NodeList propertyNodes = transactionDocument.getElementsByTagName( "transaction_property" );

        for(int i = 0; i < propertyNodes.getLength(); i++){
            Element propertyElement = (Element) propertyNodes.item( i );
            int key = Integer.parseInt( propertyElement.getAttribute( "key" ));
            String valueString = String.valueOf( transaction.getProperty( key ) );
            propertyElement.setTextContent( valueString );
        }

    }
    private void updateStoredProjection(Document transactionDocument, ProjectedTransaction projectedTransaction){
        Element projectionsSetElement = (Element) transactionDocument.getElementsByTagName( "projections" ).item( 0 );
        Element projectionElement = null;
        NodeList storedProjectionsNodeList = projectionsSetElement.getChildNodes();

        int i = 0;
        while (i < storedProjectionsNodeList.getLength() && projectionElement == null) {
            Element storedProjection = (Element) storedProjectionsNodeList.item( i );
            if (storedProjection.getAttribute( "scheduledProjectionDate" )
                    .contentEquals( projectedTransaction.getScheduledProjectionDate().format( DateTimeFormatter.BASIC_ISO_DATE ) ))
                projectionElement = storedProjection;
            i++;
        }

        if(projectionElement == null) {
            projectionElement = getNewProjectionElement(transactionDocument, projectedTransaction);
            projectionsSetElement.appendChild( (Node)projectionElement );
        }


        updateProjectionProperties(projectionElement, projectedTransaction);


    }
    private void updateProjectionProperties(Element projectionElement, ProjectedTransaction projectedTransaction){
        NodeList propertyNodes = projectionElement.getElementsByTagName( "projection_property" );
        projectionElement.setAttribute( "scheduledProjectionDate", projectedTransaction.getScheduledProjectionDate().format( DateTimeFormatter.BASIC_ISO_DATE ) );

        for(int i = 0; i < propertyNodes.getLength(); i++){
            Element propertyElement = (Element) propertyNodes.item( i );
            int key = Integer.parseInt( propertyElement.getAttribute( "key" ));

            propertyElement.setTextContent( String.valueOf( projectedTransaction.getProperty( key ) ) );
        }
    }
    private Document getNewTransactionDocument(Transaction transaction){
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder;
        Document document;

        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
            document = documentBuilder.newDocument();
            document.setDocumentURI( transaction.getPath().getPath() );
            Element element = document.createElement("transaction");
            element.setAttribute( "id", transaction.getId().toString() );
            element.setIdAttribute( "id", true );
            element.setAttribute( "incomeFlag", String.valueOf( transaction.getIncomeFlag() ));



            Element label_node = document.createElement( "transaction_property" );
            label_node.setAttribute( "key", String.valueOf(R.string.label_tag ) );

            Element value_node = document.createElement( "transaction_property" );
            value_node.setAttribute( "key", String.valueOf(R.string.amount_tag ) );

            Element date_node = document.createElement( "transaction_property" );
            date_node.setAttribute( "key", String.valueOf(R.string.date_tag ) );

            Element note_node = document.createElement( "transaction_property" );
            note_node.setAttribute( "key", String.valueOf(R.string.note_tag ) );

            Element recurrance_node = document.createElement( "transaction_property" );
            recurrance_node.setAttribute( "key", String.valueOf(R.string.recurrence_tag ) );

            Element projectedTransactions = document.createElement( "projections" );

            label_node.setTextContent( "" );
            value_node.setTextContent( "" );
            date_node.setTextContent( "" );
            note_node.setTextContent( "" );
            recurrance_node.setTextContent( "" );

            element.appendChild( label_node );
            element.appendChild( value_node );
            element.appendChild( date_node );
            element.appendChild( note_node );
            element.appendChild( recurrance_node );
            element.appendChild( projectedTransactions );


            document.appendChild( element );
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            throw new Error("trouble loading transactionFile");
        }

        return document;
    }
    private Element getNewProjectionElement(Document document, ProjectedTransaction projectedTransaction){

        Element element = document.createElement( "storedProjection" );
        element.setAttribute( "scheduledProjectionDate", projectedTransaction.getScheduledProjectionDate().format( DateTimeFormatter.BASIC_ISO_DATE ) );


        Element value_node = document.createElement( "projection_property" );
        value_node.setAttribute( "key", String.valueOf(R.string.amount_tag ) );

        Element date_node = document.createElement( "projection_property" );
        date_node.setAttribute( "key", String.valueOf(R.string.date_tag ) );

        Element note_node = document.createElement( "projection_property" );
        note_node.setAttribute( "key", String.valueOf(R.string.note_tag ) );

        value_node.setTextContent( "" );
        date_node.setTextContent( "" );
        note_node.setTextContent( "" );

        element.appendChild( value_node );
        element.appendChild( date_node );
        element.appendChild( note_node );

        return element;

    }

    private void updateRecord(Document recordDocument, RecordedTransaction recordedTransaction) {
        Element recordsSetElement = recordDocument.getDocumentElement();
        NodeList records = recordsSetElement.getChildNodes();
        Element recordElement = null;

        int index = 0;
        while(recordElement == null && index < records.getLength()){
            Element childElement = (Element)records.item( index );
            UUID childID = UUID.fromString( childElement.getAttribute( "record_id" ) );
            UUID recordId = recordedTransaction.getRecordId();
            if(childID.compareTo(recordId) == 0) recordElement = childElement;
            index++;
        }

        if(recordElement == null) {
            recordElement = getNewRecordElement(recordDocument, recordedTransaction);
            recordsSetElement.appendChild( recordElement );
        }

        updateRecordProperties(recordElement, recordedTransaction);
    }
    private void updateRecordProperties(Element recordElement, RecordedTransaction recordedTransaction){
        NodeList propertyNodes = recordElement.getElementsByTagName( "record_property" );

        for(int i = 0; i < propertyNodes.getLength(); i++){
            Element propertyElement = (Element) propertyNodes.item( i );
            int key = Integer.parseInt( propertyElement.getAttribute( "key" ));

            propertyElement.setTextContent( String.valueOf( recordedTransaction.getProperty( key ) ) );
        }
    }
    private Document getNewRecordDocument(RecordedTransaction recordedTransaction){
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder;
        Document document;

        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
            document = documentBuilder.newDocument();

            //TODO: consider dropping the getPath() from Transactions and instead build paths using the Id
            String[] recordPath = (recordedTransaction.getPath().getPath()).split( "/" );
            recordPath[recordPath.length - 2] = "app_records";
            document.setDocumentURI( String.join( "/", recordPath ) );

            Element element = document.createElement("transactionRecords");
            element.setAttribute( "transactionURI", recordedTransaction.getPath().getPath() );

            document.appendChild( element );
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            throw new Error("trouble loading recordFile");
        }

        return document;
    }
    private Element getNewRecordElement(Document document, RecordedTransaction recordedTransaction){

        Element element = document.createElement( "record" );
        element.setAttribute( "record_id", recordedTransaction.getRecordId().toString() );
        element.setIdAttribute( "record_id", true );

        Element value_node = document.createElement( "record_property" );
        value_node.setAttribute( "key", String.valueOf(R.string.amount_tag ) );

        Element date_node = document.createElement( "record_property" );
        date_node.setAttribute( "key", String.valueOf(R.string.date_tag ) );

        Element note_node = document.createElement( "record_property" );
        note_node.setAttribute( "key", String.valueOf(R.string.note_tag ) );

        value_node.setTextContent( "" );
        date_node.setTextContent( "" );
        note_node.setTextContent( "" );

        element.appendChild( value_node );
        element.appendChild( date_node );
        element.appendChild( note_node );

        return element;
    }

    private Document getNewSettingsDocument(){
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder;
        Document document;

        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
            document = documentBuilder.newDocument();
            document.setDocumentURI( filesDir + "/settings" );
            Element element = document.createElement("settings");


            Element periodsToProject = document.createElement( "settings_parameter" );
            periodsToProject.setAttribute( "key", "projection_periods_to_project" );

            Element thresholdValue = document.createElement( "settings_parameter" );
            thresholdValue.setAttribute( "key", "balance_threshold" );


            periodsToProject.setTextContent( "1" );
            thresholdValue.setTextContent( "100" );

            element.appendChild( periodsToProject );
            element.appendChild( thresholdValue );


            document.appendChild( element );
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            throw new Error("trouble loading settings document");
        }

        return document;
    }
    private void updateSettings(Document settingsDocument, String key, String value){
        NodeList settingsNodes = settingsDocument.getElementsByTagName( "settings_parameter" );

        for(int i = 0; i < settingsNodes.getLength(); i++){
            Element propertyElement = (Element) settingsNodes.item( i );
            String stored_key = propertyElement.getAttribute( "key" );
            if(stored_key.contentEquals( key )){
                propertyElement.setTextContent( value );
                return;
            }
        }
    }

    private File readAppFile(String path) {
        File file = new File(this.filesDir, path);

        if( !file.exists() && (path.contentEquals( "settings" ) )){
            try {
                file.setReadable( true );
                file.setWritable( true );
                file.createNewFile();

                Document settingsDocument = getNewSettingsDocument();
                writeDocumentToFile( settingsDocument );
                file = readAppFile( "settings" );

            } catch (IOException e) { e.printStackTrace(); }
        }

        else if(!file.exists()) {
            try {
                file.mkdir();
                file.setReadable( true );
                file.setWritable( true );
                file.createNewFile();
            }
            catch (IOException e) { e.printStackTrace(); }

        }
        return file;
    }
    private Document getDocumentFromFile(File file){

        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

            Document document = documentBuilder.parse(file);

            document.setDocumentURI( file.getPath() );
            return document;

        } catch (Exception e){
            e.printStackTrace();
            String[] path = file.toURI().getPath().split( "/" );
            if(path[path.length-1].contentEquals( "settings" ))
                return getNewSettingsDocument();

            else return null;
        }
    }
    private boolean writeDocumentToFile(Document document) {

        try {
            File transaction_file = new File(document.getDocumentURI());

            if(!transaction_file.exists()){
                transaction_file.setWritable( true );
                transaction_file.setReadable( true );
                transaction_file.createNewFile();
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(document);
            StreamResult streamResult = new StreamResult(transaction_file);
            transformer.transform(source, streamResult);
            return true;
        } catch (TransformerException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }

}
