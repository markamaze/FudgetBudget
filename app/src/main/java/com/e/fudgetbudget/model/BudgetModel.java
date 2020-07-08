package com.e.fudgetbudget.model;


import android.content.Context;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class BudgetModel {
    private final Context context;
//    private LinkedList<Record> records_linkedlist;
//    private HashMap<URI, Record> records_map;
    private HashMap<URI, Transaction> transactions_expenses_map;
    private HashMap<URI, Transaction> transactions_income_map;
    private Double current_balance;
    private UUID last_record_id;
    private final String projectionPeriod;
    private final int periodsToProject;


    public BudgetModel(Context context) {
        this.context = context;
        this.projectionPeriod = "days";
        this.periodsToProject = 130;
        File transactions_file = readAppFile("app_transactions");
        File records_file = readAppFile("app_records");
        File balance_file = readAppFile( "app_balance" );


        //Build Transaction Resources
        List transactions_file_set = Arrays.asList( transactions_file.listFiles() );
        HashMap<URI, Transaction> income_transactions = new HashMap<>(  );
        HashMap<URI, Transaction> expense_transactions = new HashMap<>(  );

        for( Object file : transactions_file_set){
            Transaction transaction = Transaction.getInstance( getDocumentFromFile( (File) file ) );
            if (!transaction.getIncomeFlag()) expense_transactions.put(transaction.getPath(), transaction);
            else income_transactions.put(transaction.getPath(), transaction);
        }
        this.transactions_income_map = income_transactions;
        this.transactions_expenses_map = expense_transactions;


        //Build Records Resources  TODO: finish writing once I'm ready to create records
//        this.records_linkedlist = new LinkedList<>(  );
//        this.records_map = new HashMap<>(  );


        //Read Balance From File
        Document balance_document = getDocumentFromFile( balance_file );

        if(balance_document == null) this.current_balance = 0.00;
        else {
            Node balance_node = balance_document.getFirstChild();
            String balance_string = balance_node.getTextContent();
            this.current_balance = Double.parseDouble( balance_string );
        }
    }
    public Object factory(Class clazz, int type){
        if(clazz.equals( Transaction.class )) return Transaction.getInstance(type);
//        else if(clazz.equals( Record.class )) return Record.getInstance();
        else return null;
    }
    public Object factory(Class clazz, int type, UUID id){
        if(clazz.equals( Transaction.class )) return Transaction.getInstance(type, id);
//        else if(clazz.equals( Record.class )) return Record.getInstance(id);
        else return null;
    }

    private File readAppFile(String path) {
        File file = new File(this.context.getFilesDir(), path);

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
    private boolean writeDocumentToFile(Document document) {

        try {
            File transaction_file;
            if(document.getDocumentURI().contains( this.context.getFilesDir().toURI().getPath() )) transaction_file = new File(document.getDocumentURI());
            else transaction_file = new File( this.context.getFilesDir(), document.getDocumentURI());
            
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
        } catch ( TransformerConfigurationException e) {
            e.printStackTrace();
            return false;
        } catch (TransformerException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    private Document getDocumentFromFile(File file){

        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

            Document document = documentBuilder.parse(file);
            return document;

        } catch (Exception e){ e.printStackTrace(); return null; }
    }


    //todo: add a property for period
    //  adjust method by grouping results into entries by the period property
    //todo: remove days_to_project parameter and replace with a property in budgetmodel for how far to project
    public LinkedHashMap<LocalDate, ArrayList<ProjectedTransaction>> getProjectedTransactionsByPeriod(){
        LinkedHashMap<LocalDate, ArrayList<ProjectedTransaction>> result = new LinkedHashMap<>();
        ArrayList<Transaction> allTransactions = new ArrayList<>();

        allTransactions.addAll( this.transactions_expenses_map.values() );
        allTransactions.addAll( this.transactions_income_map.values() );

        LocalDate currentDate = LocalDate.now();


        for( int index = 0; index < this.periodsToProject; index ++){
            if(this.projectionPeriod.contentEquals( "days" )) result.put( currentDate.plusDays( index ), new ArrayList<ProjectedTransaction>() );
            else if(this.projectionPeriod.contentEquals( "weeks" )) result.put( currentDate.plusWeeks( index ), new ArrayList<>(  ));
            else if(this.projectionPeriod.contentEquals( "months" )) result.put( currentDate.plusMonths( index ), new ArrayList<>(  ));
            else if(this.projectionPeriod.contentEquals( "years" )) result.put( currentDate.plusYears( index ), new ArrayList<>(  ));
        }

        while(!allTransactions.isEmpty()){
            Transaction currentTransaction = allTransactions.remove(0);
            LocalDate scheduledDate = currentTransaction.getScheduledDate();

            if (scheduledDate.isAfter( this.getCutoffDate() )) continue;

            if(scheduledDate.isBefore( (LocalDate)result.keySet().toArray()[0] )){
                //if scheduledDate is before date in first entry of result, we need to extend the range of result
                int index = 0;
                while(scheduledDate.isBefore( currentDate )){
                    LinkedHashMap<LocalDate, ArrayList<ProjectedTransaction>> result_copy = new LinkedHashMap<>(  );
                    index++;
                    currentDate = currentDate.minusDays( index );
                    result_copy.put( currentDate, new ArrayList<ProjectedTransaction>(  ));

                    result.forEach( (date, projection) -> result_copy.put( date, projection ) );
                    result = result_copy;
                }

            }

            ProjectedTransaction projection = ProjectedTransaction.getInstance( scheduledDate, currentTransaction );
            ArrayList<ProjectedTransaction> resultSet = result.get( scheduledDate );
            resultSet.add( projection );
            result.replace( scheduledDate, resultSet );

            HashMap<LocalDate, ProjectedTransaction> currentTransactionProjections = currentTransaction.getProjectedTransactionsWithProjectedDate(this.getCutoffDate());

            for(Map.Entry<LocalDate, ProjectedTransaction> entry: currentTransactionProjections.entrySet()){
                ArrayList<ProjectedTransaction> resultSet1 = result.get( entry.getKey() );
                resultSet1.add(entry.getValue());
                result.replace( entry.getKey(), resultSet1);
            }
        }

        return result;
    }
    public ArrayList<Transaction> getIncomeTransactions(){
        ArrayList<Transaction> result = new ArrayList<>();
        Arrays.asList( this.transactions_income_map.values().toArray()).stream().forEach( transaction -> result.add((Transaction) transaction) );
        return result;
    }
    public ArrayList<Transaction> getExpenseTransactions(){
        ArrayList<Transaction> result = new ArrayList<>();
        Arrays.asList( this.transactions_expenses_map.values().toArray()).stream().forEach( transaction ->
                result.add((Transaction) transaction ) );
        return result;
    }
    public Double getCurrentBalance() { return this.current_balance; }
    public double getExpenseSumFromList(ArrayList<?> projected_transaction_list) {
        double total = 0.00;

        for(Object item : projected_transaction_list){
            if(item instanceof ProjectedTransaction && !((ProjectedTransaction)item).getIncomeFlag())
                total += ((ProjectedTransaction)item).getAmount();
        }
        return total;
    }
    public double getIncomeSumFromList(ArrayList<?> projected_transaction_list) {
        double total = 0.00;

        for(Object item : projected_transaction_list){
            if(item instanceof ProjectedTransaction && ((ProjectedTransaction)item).getIncomeFlag())
                total += ((ProjectedTransaction)item).getAmount();
        }
        return total;
    }
    public void delete(Object object){
        if(object instanceof Transaction){
            Transaction transaction = (Transaction) object;
            File objectFile = new File(transaction.getPath());

            System.out.println(objectFile.delete());
        }
        else throw new Error("you are trying to delete an unknown object type");
    }
    public Object update(Object object){
        if(object instanceof Transaction){
            Transaction transaction = (Transaction) object;
            writeDocumentToFile( transaction.writeToDocument());

            return transaction;
        }

        else if(object instanceof ProjectedTransaction) {
            System.out.println( "handle removing projection from transactino here" );
        }
//        else if(object instanceof Record){
//            Record record = (Record) object;
//            writeDocumentToFile( record.writeToDocument() );
//            return record;
//        }
        else if(object instanceof Double){
            //TODO: need to handle difference between new balance and old by creating a record that reflects the change
            Double value = (Double) object;

            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = null;
            try { documentBuilder = documentBuilderFactory.newDocumentBuilder(); }
            catch (ParserConfigurationException e) { e.printStackTrace(); }

            Document document = documentBuilder.newDocument();
            document.setDocumentURI( "app_balance" );
            Element element = document.createElement( "current_balance" );
            element.setTextContent( value.toString() );
            document.appendChild( element );
//            document.setTextContent( value.toString() );
//            document.createTextNode( value.toString() );
            writeDocumentToFile( document );
            this.current_balance = value;
            return value;
        }
        return object;
    }

    public LocalDate getCutoffDate() {
        switch (this.projectionPeriod) {
            case "days":
                return LocalDate.now().plusDays( this.periodsToProject );
            case "weeks":
                return LocalDate.now().plusWeeks( this.periodsToProject );
            case "months":
                return LocalDate.now().plusMonths( this.periodsToProject );
            case "years":
                return LocalDate.now().plusYears( this.periodsToProject );
            default:
                return LocalDate.now();
        }
    }
}

