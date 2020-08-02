package com.e.fudgetbudget.model;

import android.content.Context;

import com.e.fudgetbudget.R;

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
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

class StorageControl {
    private final Context context;

    StorageControl(Context context) { this.context = context; }

    ArrayList<Transaction> readTransactions(Object type){
        File[] transactions_file_set = readAppFile("app_transactions").listFiles();
        ArrayList<Transaction> found_transactions = new ArrayList<>(  );

        if(type.toString().contentEquals( "income" ))
            Arrays.asList(transactions_file_set).stream().forEach( file -> {
                Transaction transaction = Transaction.getInstance( getDocumentFromFile( file ) );
                if(transactionIsActive( transaction ) && (boolean)transaction.getProperty( R.string.income_tag ))
                    found_transactions.add( transaction );
            } );

        else if(type.toString().contentEquals( "expense" ))
            Arrays.asList(transactions_file_set).stream().forEach( file -> {
                Transaction transaction = Transaction.getInstance( getDocumentFromFile( file ) );

                if(transactionIsActive( transaction) && !(boolean)transaction.getProperty(R.string.income_tag ))
                        found_transactions.add( transaction );
            } );

        else if(type.toString().contentEquals( "all_active" ))
            Arrays.asList(transactions_file_set).stream().forEach( file -> {
                Transaction transaction = Transaction.getInstance( getDocumentFromFile( file ) );
                if( transactionIsActive(transaction) ) found_transactions.add( transaction );
            });
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


    LinkedHashMap<LocalDate, ProjectedTransaction> readProjections(Transaction transaction){
        LinkedHashMap<LocalDate, ProjectedTransaction> projections = new LinkedHashMap<>(  );

        File transactionFile = new File(transaction.getPath().getPath());
        Document transactionDocument = getDocumentFromFile( transactionFile );

        NodeList storedProjections = transactionDocument.getElementsByTagName("projections").item( 0 ).getChildNodes();
        for(int i = 0; i < storedProjections.getLength(); i++){
            Element projectionElement = (Element) storedProjections.item( i );
            ProjectedTransaction projectedTransaction = ProjectedTransaction.getInstance( transaction, projectionElement );
            projections.put( projectedTransaction.getScheduledProjectionDate(), projectedTransaction );
        }

        return projections;
    }
    ArrayList<RecordedTransaction> readRecords(Object object){
        File[] recordsFiles = readAppFile("app_records").listFiles();
        ArrayList<RecordedTransaction> foundRecords = new ArrayList<>(  );

        if(recordsFiles == null || recordsFiles.length <= 0) return foundRecords;

        if(object instanceof String && String.valueOf( object ).contentEquals( "all" ))
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
    Object readSettingsValue(String setting) {
        switch (setting){
            case "projection_period": return "months";
            case "projection_periods_to_project": return 12;
            default: return null;
        }
    }

    boolean writeTransaction(Transaction transaction){
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

        if(transaction.getClearStoredProjectionsFlag()){
            //remove all stored projections from transactionDocument

            Element storedProjectionsElement = (Element) transactionDocument.getElementsByTagName( "projections" ).item( 0 );
            while(storedProjectionsElement.getChildNodes().getLength() > 0)
                storedProjectionsElement.removeChild( storedProjectionsElement.getFirstChild() );

        }

        return writeDocumentToFile( transactionDocument );
    }
    boolean writeProjection(ProjectedTransaction projectedTransaction){
        File transactionFile = new File(projectedTransaction.getPath().getPath());
        Document transactionDocument;
        if(!transactionFile.exists()) return false;
        else transactionDocument = getDocumentFromFile( transactionFile );

        updateStoredProjection( transactionDocument, projectedTransaction );

        return writeDocumentToFile( transactionDocument );
    }
    boolean writeRecord(RecordedTransaction recordedTransaction){

        //need to get the file based on the location of the recordedTransaction, not the transaction
        String[] uri = recordedTransaction.getPath().getPath().split( "/" );
        uri[uri.length - 2] = "app_records";


        File recordFile = new File(String.join( "/", uri ));
        Document recordDocument = getDocumentFromFile( recordFile );

        if(recordDocument == null) recordDocument = getNewRecordDocument( recordedTransaction );


        updateRecord( recordDocument, recordedTransaction );

        return writeDocumentToFile( recordDocument );
    }
    boolean writeSettingsValue(String key, String value) {
        return false;
    }

    boolean deleteProjection(ProjectedTransaction projection) {
        File transactionFile = new File(projection.getPath().getPath());
        Document transactionDocument;
        if(!transactionFile.exists()) return false;
        else transactionDocument = getDocumentFromFile( transactionFile );

        NodeList storedProjections = transactionDocument.getElementsByTagName( "projections" ).item( 0 ).getChildNodes();
        Element foundProjection = null;

        int index = 0;
        while(foundProjection == null && index < storedProjections.getLength()){
            Element storedProjection = (Element) storedProjections.item( index );
            if( storedProjection.getAttribute( "scheduledProjectionDate" )
                    .contentEquals( projection.getScheduledProjectionDate().format( DateTimeFormatter.BASIC_ISO_DATE )))
                foundProjection = storedProjection;
            index++;
        }

        if(foundProjection != null)
            ((Element)transactionDocument.getElementsByTagName( "projections" ).item( 0 )).removeChild( foundProjection );


        return true;
    }
    boolean deleteRecord(RecordedTransaction object) {
        return false;
    }
    boolean deleteTransaction(Transaction object) {
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

            propertyElement.setTextContent( String.valueOf( transaction.getProperty( key ) ) );
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



            Element label_node = document.createElement( "transaction_property" );
            label_node.setAttribute( "key", String.valueOf(R.string.label_tag ) );

            Element value_node = document.createElement( "transaction_property" );
            value_node.setAttribute( "key", String.valueOf(R.string.amount_tag ) );

            Element date_node = document.createElement( "transaction_property" );
            date_node.setAttribute( "key", String.valueOf(R.string.date_tag ) );

            Element note_node = document.createElement( "transaction_property" );
            note_node.setAttribute( "key", String.valueOf(R.string.note_tag ) );

            Element incomeflag_node = document.createElement( "transaction_property" );
            incomeflag_node.setAttribute( "key", String.valueOf(R.string.income_tag ) );

            Element recurrance_node = document.createElement( "transaction_property" );
            recurrance_node.setAttribute( "key", String.valueOf(R.string.recurrence_tag ) );

            Element projectedTransactions = document.createElement( "projections" );

            //TODO: given that I"m storing records in their own file, this should be removed.
            //  (don't want to break the storage I have right now)
            Element records = document.createElement( "records" );



            label_node.setTextContent( "" );
            value_node.setTextContent( "" );
            date_node.setTextContent( "" );
            note_node.setTextContent( "" );
            incomeflag_node.setTextContent( transaction.getIncomeFlag().toString() );
            recurrance_node.setTextContent( "" );

            element.appendChild( label_node );
            element.appendChild( value_node );
            element.appendChild( date_node );
            element.appendChild( note_node );
            element.appendChild(incomeflag_node);
            element.appendChild( recurrance_node );
            element.appendChild( projectedTransactions );
            element.appendChild( records );


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


    //TODO: rework given change in how records will be stored
    private void updateRecord(Document recordDocument, RecordedTransaction recordedTransaction) {
        Element recordsSetElement = recordDocument.getDocumentElement();
        Element recordElement = null;
        NodeList storedRecordsNodeList = recordsSetElement.getChildNodes();

        int i = 0;
        while (i < storedRecordsNodeList.getLength() && recordElement == null) {
            Element storedRecord = (Element) storedRecordsNodeList.item( i );
            if (storedRecord.getAttribute( "recordDate" )
                    .contentEquals( ((LocalDate)recordedTransaction.getProperty(R.string.date_tag)).format( DateTimeFormatter.BASIC_ISO_DATE ) ))
                recordElement = storedRecord;
            i++;
        }

        if(recordElement == null) {
            recordElement = getNewRecordElement(recordDocument, recordedTransaction);
            recordsSetElement.appendChild( recordElement );
        }


        updateRecordProperties(recordElement, recordedTransaction);


    }

    //TODO:
    private void updateRecordProperties(Element recordElement, RecordedTransaction recordedTransaction){
        NodeList propertyNodes = recordElement.getElementsByTagName( "record_property" );
        recordElement.setAttribute( "recordDate", ((LocalDate) recordedTransaction.getProperty(R.string.date_tag)).format( DateTimeFormatter.BASIC_ISO_DATE ) );

        for(int i = 0; i < propertyNodes.getLength(); i++){
            Element propertyElement = (Element) propertyNodes.item( i );
            int key = Integer.parseInt( propertyElement.getAttribute( "key" ));

            propertyElement.setTextContent( String.valueOf( recordedTransaction.getProperty( key ) ) );
        }
    }



    //TODO: need to create new method to getTransactionDocument, then rework getting new record Element as needed
    private Document getNewRecordDocument(RecordedTransaction recordedTransaction){
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder;
        Document document;

        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
            document = documentBuilder.newDocument();

            //the path for a record should be: /app_records/(id), but the stored path in recordedTransaction is the transation path which is: /app_transactions/(id)
            //  so I need to figure out how to change the root folder in the path I get from the recordedTransaction
            //  that or rework how the path/uri is implemented by creating the uri when needed using the id of the transaction

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
        element.setAttribute( "recordDate", ((LocalDate)recordedTransaction.getProperty( R.string.date_tag )).format( DateTimeFormatter.BASIC_ISO_DATE ) );

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

    private File readAppFile(String path) {
        File file = new File(this.context.getExternalFilesDir( null ), path);

        if( !file.exists() && path.contentEquals( "app_balance" )){
            try {
                file.setReadable( true );
                file.setWritable( true );
                file.createNewFile();


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

        } catch (Exception e){ e.printStackTrace(); return null; }
    }
    private boolean writeDocumentToFile(Document document) {

        try {
            File transaction_file;
//            if(document.getDocumentURI().contains( this.context.getExternalFilesDir(null).toURI().getPath() ))
                transaction_file = new File(document.getDocumentURI());
//            else transaction_file = new File( this.context.getExternalFilesDir(null), document.getDocumentURI());

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
