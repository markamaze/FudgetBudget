package com.e.fudgetbudget.model;


import com.e.fudgetbudget.R;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;


public class Transaction implements Comparable {
    private UUID transaction_id;
    private URI path;
    private Boolean isIncome;
    private Boolean clearStoredProjectionsFlag;

    private String label;
    private Double amount;
    private String note;
    private LocalDate scheduledDate;
    private Object recurrance;


    public static Transaction getInstance(int type, String absolutePath) { return new Transaction(type, absolutePath); }
    static Transaction getInstance(Document document) { return new Transaction( document ); }
    Transaction(Transaction transaction) {
        //this constructor is used when another class extends Transaction and calls super()

        this.transaction_id = transaction.getId();
        this.path = transaction.getPath();
        this.isIncome = transaction.getIncomeFlag();
        this.clearStoredProjectionsFlag = false;

        this.label = transaction.getLabel();
        this.amount = transaction.getAmount();
        this.note = transaction.getNote();
        this.scheduledDate = transaction.getScheduledDate();
        this.recurrance = transaction.getRecurrance();
    }
    private Transaction(int type, String absolutePath){
        this.transaction_id = UUID.randomUUID();
        this.path = URI.create(absolutePath + "/app_transactions/" + this.transaction_id);
        if(type == R.string.income_tag) this.setIncomeFlag( true );
        else this.setIncomeFlag( false );
        this.clearStoredProjectionsFlag = false;


        this.setLabel( null );
        this.setScheduledDate( null );
        this.setAmount( null );
        this.setNote( null );
        this.setRecurrance( null );

    }
    private Transaction(Document document) {
        Element element = document.getDocumentElement();
        String id = element.getAttribute( "id" );
        this.transaction_id = UUID.fromString( id );
        this.path = URI.create(document.getDocumentURI());
        this.clearStoredProjectionsFlag = false;


        NodeList propertyNodes = element.getElementsByTagName( "transaction_property" );

        for(int i = 0; i < propertyNodes.getLength(); i++){
            Element propertyElement = (Element) propertyNodes.item( i );
            int key = Integer.parseInt( propertyElement.getAttribute( "key" ));
            String value = propertyElement.getTextContent();

            if(key == R.string.income_tag) this.setIncomeFlag( value );
            else this.setProperty( key, value );
        }

    }


    private boolean setLabel(Object label){
        if(label == null) this.label = "";
        else if(label instanceof String) this.label = (String) label;
        else return false;


//        this.updateStorageDocument("label", this.label);

        return true;
    }
    private boolean setIncomeFlag(Object flag) {
        if(flag == null) this.isIncome = false;
        else if(flag instanceof Boolean) this.isIncome = (Boolean) flag;
        else if(flag instanceof String) {
            if(((String) flag).contentEquals( "true" ))this.isIncome = true;
            else this.isIncome = false;
        }
        else return false;

        return true;
    }
    private boolean setAmount(Object amount) {
        if(amount == null || amount instanceof String && ((String)amount).isEmpty()) this.amount = new Double( "0" );
        else if(amount instanceof Double) this.amount = (Double) amount;
        else if(amount instanceof String) this.amount = Double.valueOf( (String) amount );
        else return false;

        return true;
    }
    private boolean setNote(Object note){
        if(note == null) this.note = new String();
        else if(note instanceof String) this.note = (String) note;
        else return false;

        return true;
    }
    private boolean setScheduledDate(Object scheduledDate){
        if(scheduledDate == null) this.scheduledDate = LocalDate.now();
        else if(scheduledDate instanceof String && ((String)scheduledDate).isEmpty()) this.scheduledDate = null;
        else if(scheduledDate instanceof String && ((String) scheduledDate).contentEquals( "null" )) this.scheduledDate = null;
        else if( scheduledDate instanceof LocalDate) this.scheduledDate = (LocalDate) scheduledDate;
        else if( scheduledDate instanceof String) this.scheduledDate = LocalDate.parse( (String)scheduledDate );
        else if( scheduledDate instanceof Long) this.scheduledDate = LocalDate.ofEpochDay( (Long)scheduledDate );
        else return false;

        this.clearStoredProjectionsFlag = true;
        return true;
    }
    private boolean setRecurrance(Object recurrance){
        if( recurrance == null) this.recurrance = "0-0-0-0-0-0-0";
        else if( recurrance instanceof String && ((String)recurrance).length() <= 0) this.recurrance = "0-0-0-0-0-0-0";
        else if( recurrance instanceof String) this.recurrance = recurrance;
        else return false;

        this.clearStoredProjectionsFlag = true;
        return true;
    }


    public UUID getId() { return this.transaction_id; }
    public URI getPath() { return this.path; }
    public Boolean getIncomeFlag(){ return this.isIncome; }
    public Boolean getClearStoredProjectionsFlag() { return this.clearStoredProjectionsFlag; }

    private String getLabel(){ return this.label; }
    private Double getAmount(){ return this.amount; }
    private String getNote(){ return this.note; }
    private LocalDate getScheduledDate(){ return this.scheduledDate; }
    private Object getRecurrance() { return this.recurrance; }


    public boolean setProperty(int propertyTag, Object value){
        switch(propertyTag){
            case R.string.amount_tag: return this.setAmount(value);
            case R.string.date_tag: return this.setScheduledDate(value);
            case R.string.note_tag: return this.setNote(value);
            case R.string.label_tag: return this.setLabel(value);
            case R.string.recurrence_tag: return this.setRecurrance(value);
            default: return false;
        }
    }
    public Object getProperty(int propertyTag){
        switch(propertyTag){
            case R.string.income_tag: return this.getIncomeFlag();
            case R.string.id_tag: return this.getId();
            case R.string.amount_tag: return this.getAmount();
            case R.string.date_tag: return this.getScheduledDate();
            case R.string.note_tag: return this.getNote();
            case R.string.label_tag: return this.getLabel();
            case R.string.recurrence_tag: return this.getRecurrance();
            default: return null;
        }
    }


    @Override
    public int compareTo(Object transaction) {
        LocalDate comparisonDate;
        if(transaction instanceof ProjectedTransaction) comparisonDate = (LocalDate)(((ProjectedTransaction)transaction).getProperty(R.string.date_tag ));
        else comparisonDate = (LocalDate)(((Transaction)transaction).getProperty(R.string.date_tag ));

        if ( ((LocalDate)this.getProperty(R.string.date_tag)).isAfter( comparisonDate )) return 1;
        if ( ((LocalDate)this.getProperty(R.string.date_tag)).isEqual( comparisonDate )) return 0;
        else return -1;
    }
}
