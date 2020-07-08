package com.e.fudgetbudget.model;


import com.e.fudgetbudget.R;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/*
    A Transaction is a representation of a potential income or expense record
    User defines Transactions, either recurring or not
    They are used for the projection of future balances and for defining values of Record
    When a Record is created, it will store the trasaction data within the Record

 */
public class Transaction {
    private UUID transaction_id;
    private String label;
    private Boolean isIncome;
    private Double amount;
    private String note;
    private LocalDate scheduledDate;
    private Object recurrance;
    private HashMap<LocalDate, ProjectedTransaction> projectedTransactions;
    private URI path;

    public static Transaction getInstance(int type) { return new Transaction(type); }
    public static Transaction getInstance(int type, UUID id) { return new Transaction(type, id); }
    public static Transaction getInstance(Document document) { return new Transaction( document ); }

    private Transaction(int type){
        this.transaction_id = UUID.randomUUID();
        this.path = URI.create("/app_transactions/" + this.transaction_id);
        if(type == R.string.tag_transaction_income) this.setIncomeFlag( true );
        else this.setIncomeFlag( false );
        this.setLabel( null );
        this.setScheduledDate( null );
        this.setAmount( null );
        this.setNote( null );
        this.setProjectedTransactions( null );
        this.setRecurrance( null );
        
    }
    private Transaction(int type, UUID id){
        this.transaction_id = id;
        this.path = URI.create("/app_transactions/" + this.transaction_id);
        if(type == R.string.tag_transaction_income) this.setIncomeFlag( true );
        else this.setIncomeFlag( false );
        this.setLabel( null );
        this.setScheduledDate( null );
        this.setAmount( null );
        this.setNote( null );
        this.setProjectedTransactions( null );
        this.setRecurrance( null );
    }
    private Transaction(Document document) {
        Element element = document.getDocumentElement();
        String id = element.getAttribute( "id" );

        this.transaction_id = UUID.fromString( id );
        this.path = URI.create(document.getDocumentURI());

        NodeList element_nodes = element.getChildNodes();

        for(int i = 0; i < element_nodes.getLength(); i++){
            Node node = element_nodes.item( i );
            String key = node.getNodeName();
            String value = node.getTextContent();

            if(key.contentEquals( "label" )) this.setLabel( value );
            else if(key.contentEquals( "value" )) this.setAmount( value );
            else if(key.contentEquals( "scheduledDate" )) this.setScheduledDate( value );
            else if(key.contentEquals( "note" )) this.setNote( value );
            else if(key.contentEquals( "incomeFlag" )) this.setIncomeFlag( value );
            else if(key.contentEquals( "projectedTransactions" )) this.setProjectedTransactions( node.getChildNodes() );
            else if(key.contentEquals( "recurrance" )) this.setRecurrance( value );
            else throw new Error("unknown element in Transaction document: " + key);
        }

    }


    private void setLabel(Object label){
        if(label == null) this.label = "";
        else if(label instanceof String) this.label = (String) label;
        else throw new Error("unknown data type while setting: Transaction.label");
    }
    private void setIncomeFlag(Object flag) {
        if(flag == null) this.isIncome = false;
        else if(flag instanceof Boolean) this.isIncome = (Boolean) flag;
        else if(flag instanceof String) {
            if(((String) flag).contentEquals( "true" ))this.isIncome = true;
            else this.isIncome = false;
        }
        else throw new Error("unknown data type while setting: Transaction.isIncome");
    }
    private void setAmount(Object amount) {
        if(amount == null) this.amount = new Double( "0" );
        else if(amount instanceof Double) this.amount = (Double) amount;
        else if(amount instanceof String) this.amount = Double.valueOf( (String) amount );
        else throw new Error("unknown data type while setting: Transaction.amount");
    }
    private void setNote(Object note){
        if(note == null) this.note = new String();
        else if(note instanceof String) this.note = (String) note;
        else throw new Error("unknown data type while setting: Transaction.note");
    }
    private void setScheduledDate(Object scheduledDate){
        LocalDate setDate;

        if(scheduledDate == null) {
            setDate = LocalDate.now();
            this.scheduledDate = setDate;
        }
        else if( scheduledDate instanceof LocalDate) {
            setDate = (LocalDate) scheduledDate;
            this.scheduledDate = setDate;
        }
        else if( scheduledDate instanceof String) {
            setDate = LocalDate.parse( (String)scheduledDate, DateTimeFormatter.BASIC_ISO_DATE );
            this.scheduledDate = setDate;
        }
        else throw new Error("unknown data type while setting: Transaction.scheduledDate");

        //clear all projections when changing scheduled date
        this.clearProjectedTransactions();
    }
    private void setRecurrance(Object recurrance){
        if( recurrance == null) this.recurrance = "0-0-0-0-0-0-0";
        else if( recurrance instanceof String && ((String)recurrance).length() <= 0) this.recurrance = "0-0-0-0-0-0-0";
        else if( recurrance instanceof String) this.recurrance = recurrance;
        else throw new Error("unknown data type while setting: Transaction.recurrance");

        //clear all projections when changing recurrence value
        this.clearProjectedTransactions();
    }
    private void setProjectedTransactions(Object projections) {
        if( projections == null) this.projectedTransactions = new HashMap<>();
        else if( projections instanceof NodeList ){
            Set<ProjectedTransaction> projectedList = new HashSet<>();
            NodeList projectionNodes = (NodeList) projections;
            int size = projectionNodes.getLength();
            int index = 0;
            while( index < size ){
                ProjectedTransaction projection = ProjectedTransaction.getInstance(projectionNodes.item( index ));
                projectedList.add( projection );
                index++;
            }
        }
        else if( projections instanceof Map ) {
            this.projectedTransactions = (HashMap<LocalDate, ProjectedTransaction>) projections;
        }
        else throw new Error("unknown data type while setting: Transaction.projectedTransactions");
    }
    private void clearProjectedTransactions(){ this.projectedTransactions = new HashMap<LocalDate, ProjectedTransaction>(); }
    //should make getters private and accessible through taggedPropertyGetters()
    public String getLabel(){ return this.label; }
    public Boolean getIncomeFlag(){ return this.isIncome; }
    public Double getAmount(){ return this.amount; }
    public String getNote(){ return this.note; }
    public LocalDate getScheduledDate(){ return this.scheduledDate; }
    public Object getRecurrance() { return this.recurrance; }
    public UUID getId() { return this.transaction_id; }
    public URI getPath() { return this.path; }
    private HashMap<LocalDate, ProjectedTransaction> getStoredProjections() { return this.projectedTransactions; }
    public HashMap<LocalDate, ProjectedTransaction> getProjectedTransactionsWithProjectedDate(LocalDate cutoff_date) {

        this.projectTransactionsToDate( cutoff_date );

        HashMap<LocalDate, ProjectedTransaction> projectionSet = new HashMap<>(  );
        for(ProjectedTransaction projectedTransaction: this.projectedTransactions.values()){
            projectionSet.put(projectedTransaction.getProjectedDate(), projectedTransaction);
        }
        return projectionSet;
    }


    public HashMap<Integer, PropertySetter> taggedPropertySetters(){
        HashMap<Integer, PropertySetter> set = new HashMap<>(  );
        set.put( R.string.transaction_label_tag, value -> this.setLabel(value));
        set.put( R.string.date_tag, value -> this.setScheduledDate(value));
        set.put( R.string.transaction_amount_tag, value -> this.setAmount( value ));
        set.put( R.string.transaction_note_tag, value -> this.setNote( value ));
//        set.put( R.string.transaction_incomeFlag_tag, value -> this.setIncomeFlag( value ));
        set.put( R.string.transaction_recurrence_tag, value -> this.setRecurrance( value ));
//        set.put( R.string.transaction_projectedTransactions_tag, value -> this.setProjectedTransactions( value ));

        return set;
    }
    public HashMap<Integer, PropertyGetter> taggedPropertyGetters(){
        HashMap<Integer, PropertyGetter> set = new HashMap<>(  );
//        set.put( R.string.transaction_label_tag, () -> this.getLabel());
//        set.put( R.string.transaction_date_tag, () -> this.getScheduledDate());
//        set.put( R.string.transaction_amount_tag, () -> this.getAmount());
//        set.put( R.string.transaction_note_tag, () -> this.getNote());
//        set.put( R.string.transaction_incomeFlag_tag, () -> this.getIncomeFlag());
//        set.put(R.string.transaction_recurrance_tag, () -> this.getRecurrance());
//        set.put( R.string.transaction_projectedTransactions_tag, () -> this.getProjectedTransactions(  ));

        return set;
    }

    public Document writeToDocument() {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = null;
        try { documentBuilder = documentBuilderFactory.newDocumentBuilder(); }
            catch (ParserConfigurationException e) { e.printStackTrace(); }
        Document document = documentBuilder.newDocument();
        document.setDocumentURI( this.getPath().getPath() );
        Element element = document.createElement("transaction");
        element.setAttribute( "id", this.getId().toString() );

        Element label_node = document.createElement( "label" );
        Element value_node = document.createElement( "value" );
        Element date_node = document.createElement( "scheduledDate" );
        Element note_node = document.createElement( "note" );
        Element incomeflag_node = document.createElement( "incomeFlag" );
        Element recurrance_node = document.createElement( "recurrance" );
        Element projectedTransactions = document.createElement( "projectedTransactions" );

        label_node.setTextContent( this.getLabel() );
        value_node.setTextContent( this.getAmount().toString() );
        date_node.setTextContent( this.getScheduledDate().format( DateTimeFormatter.BASIC_ISO_DATE ) );
        note_node.setTextContent( this.getNote() );
        incomeflag_node.setTextContent( this.getIncomeFlag().toString() );
        recurrance_node.setTextContent( this.getRecurrance().toString() );
        this.getStoredProjections().forEach( (scheduledDate, projectedTransaction) ->
                projectedTransactions.appendChild( projectedTransaction.writeToElement() ) );

        element.appendChild( label_node );
        element.appendChild( value_node );
        element.appendChild( date_node );
        element.appendChild( note_node );
        element.appendChild(incomeflag_node);
        element.appendChild( recurrance_node );
        element.appendChild( projectedTransactions );


        document.appendChild( element );
        return document;
    }

    private void projectTransactionsToDate(LocalDate cuttoff_date) {
        HashMap<LocalDate, ProjectedTransaction> newProjectionSet = new HashMap<>(  );
        HashMap<LocalDate, ProjectedTransaction> currentProjectionSet = this.getStoredProjections();

        LocalDate indexDate = getNextProjectedDate( this.getScheduledDate() );

        while(indexDate !=null && indexDate.isBefore( cuttoff_date )){
            ProjectedTransaction projection = currentProjectionSet.get( indexDate );

            if(projection ==null) projection = ProjectedTransaction.getInstance( indexDate, this );
            newProjectionSet.put( projection.getScheduledDate(), projection );
            indexDate = getNextProjectedDate( indexDate );
        }

        this.setProjectedTransactions( newProjectionSet );
    }

    private LocalDate getNextProjectedDate(LocalDate indexDate) {
        String[] recurrenceGroups = this.getRecurrance().toString().split( "-" );
        int recurrenceBit = Integer.parseInt( recurrenceGroups[0] );
        int frequency = Integer.parseInt( recurrenceGroups[1] );
        String period = recurrenceGroups[2];
        String onDayType = recurrenceGroups[3];
        int endRecurrenceBit = Integer.parseInt( recurrenceGroups[4] );
        String endParameterType = recurrenceGroups[5];
        String endParameterValue = recurrenceGroups[6];

        LocalDate nextDate;
        if(recurrenceBit == 0) nextDate = null;
        else if(period.contentEquals( "Day" )) nextDate = indexDate.plusDays( frequency );
        else if(period.contentEquals( "Week" )) nextDate = indexDate.plusWeeks( frequency );
        else if(period.contentEquals("Month" )){
            if(onDayType.contentEquals( "DayOfMonth" )) nextDate = indexDate.plusMonths( frequency );
            else if(onDayType.contentEquals( "WeekDayOfMonth" )){
                int indexDayOfWeekValue = indexDate.getDayOfWeek().getValue();
                int tempDayValue = indexDate.getDayOfMonth();
                int weekCount = 1;

                while (tempDayValue > 7) {
                    tempDayValue = tempDayValue - 7;
                    weekCount++;
                }

                //create a temp next date set to the correct month, but starting on day 1
                //then increment until it is the correct day of the week
                //once on correct day of week, increment by week until count at 1
                //this should land us on the correct day (mostly: doesn't handle 5th and/or last day of week of the month
                LocalDate tempNextDate = indexDate.plusMonths( frequency ).withDayOfMonth( 1 );
                while( tempNextDate.getDayOfWeek().getValue() != indexDayOfWeekValue )  {
                    tempNextDate.plusDays( 1 );
                }
                while( weekCount > 1) {
                    tempNextDate.plusWeeks( 1 );
                    weekCount--;
                }

                nextDate = tempNextDate;
            }
            else nextDate = null;
        }
        else if(period.contentEquals( "Year" )) nextDate = indexDate.plusYears( frequency );
        else nextDate = null;

        if(endRecurrenceBit == 0) return nextDate;
        else if(endParameterType.contentEquals( "StopAfterDate" )){
            LocalDate endDate = LocalDate.parse( endParameterValue, DateTimeFormatter.BASIC_ISO_DATE );
            if(endDate.isAfter( nextDate )) return nextDate;
            else return null;
        }
        else if(endParameterType.contentEquals( "StopAfterOccurrenceCount" )){
            //don't have this setup for now, will require checking records and stored projections to get the count
            return nextDate;
        }
        else if(endParameterType.contentEquals( "StopAfterRecordedAmount" )){
            //don't have this setup for now, will require checking records and stored projections to get balance
            return nextDate;
        }
        else return nextDate;
    }
}
