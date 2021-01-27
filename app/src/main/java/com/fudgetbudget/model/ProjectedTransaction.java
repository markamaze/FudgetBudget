package com.fudgetbudget.model;

import com.fudgetbudget.R;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ProjectedTransaction extends Transaction {
    private final LocalDate scheduledProjectionDate;
    private final Double scheduledAmount;
    private final String scheduledNote;
    String projectedNote;
    Double projectedAmount;
    LocalDate projectedDate;

    public static ProjectedTransaction getInstance(Transaction transaction, LocalDate scheduledDate) { return new ProjectedTransaction( transaction, scheduledDate ); }
    public static ProjectedTransaction getInstance(Transaction transaction, Element projectionElement) { return new ProjectedTransaction( transaction, projectionElement ); }

    private ProjectedTransaction(Transaction transaction, LocalDate scheduledDate) {
        super( transaction );
        this.scheduledProjectionDate = scheduledDate ;
        this.scheduledAmount = (Double) transaction.getProperty( R.string.amount_tag );
        this.scheduledNote = (String) transaction.getProperty( R.string.note_tag );

        setProjectedDate( scheduledDate );
        setProjectedAmount( transaction.getProperty( R.string.amount_tag ) );
        setProjectedNote( transaction.getProperty( R.string.note_tag ) );
    }
    private ProjectedTransaction(Transaction transaction, Element projectionElement){
        super(transaction);
        String scheduledProjectionDate = projectionElement.getAttribute( "scheduledProjectionDate" );
        if(scheduledProjectionDate == null || scheduledProjectionDate.isEmpty())
            this.scheduledProjectionDate = (LocalDate) transaction.getProperty( R.string.date_tag );
        else this.scheduledProjectionDate = LocalDate.parse( scheduledProjectionDate, DateTimeFormatter.BASIC_ISO_DATE );

        this.scheduledAmount = (Double) transaction.getProperty( R.string.amount_tag );
        this.scheduledNote = (String) transaction.getProperty( R.string.note_tag );

        NodeList projectionProperties = projectionElement.getElementsByTagName( "projection_property" );
        for(int i = 0; i < projectionProperties.getLength(); i++) {
            Element propertyElement = (Element) projectionProperties.item( i );
            this.setProperty( Integer.parseInt( propertyElement.getAttribute( "key" ) ), propertyElement.getTextContent() );

        }
    }

    private boolean setProjectedDate(Object object){
        if(object == null || object instanceof String && ((String)object).isEmpty()) this.projectedDate = LocalDate.now();
        else if( object instanceof LocalDate) this.projectedDate = (LocalDate) object;
        else if( object instanceof String) this.projectedDate = LocalDate.parse( (String)object );
        else if( object instanceof Long) this.projectedDate = LocalDate.ofEpochDay( (Long)object );
        else return false;

        return true;
    }
    private boolean setProjectedAmount(Object amount){
        if(amount == null || amount instanceof String && ((String)amount).isEmpty()) this.projectedAmount = new Double( "0" );
        else if(amount instanceof Double) this.projectedAmount = (Double) amount;
        else if(amount instanceof String) {
            String stringAmount = (String) amount;
            if(stringAmount.substring( 0, 1 ).contentEquals( "$" ))
                this.projectedAmount = Double.valueOf( stringAmount.substring( 1 ) );
            else this.projectedAmount = Double.valueOf( (String) amount );
        }
        else return false;

        return true;
    }
    private boolean setProjectedNote(Object note){
        if(note == null) this.projectedNote = new String();
        else if(note instanceof String) this.projectedNote = (String) note;
        else return false;

        return true;
    }

    public LocalDate getScheduledProjectionDate() { return this.scheduledProjectionDate; }
    public Double getScheduledAmount() { return this.scheduledAmount; }
    private LocalDate getProjectedDate(){ return this.projectedDate; }
    private Double getProjectedAmount(){ return this.projectedAmount; }
    private String getProjectedNote(){ return this.projectedNote; }


    public boolean setProperty(int propertyTag, Object value){
        switch (propertyTag){
            case R.string.date_tag: return setProjectedDate( value );
            case R.string.amount_tag: return setProjectedAmount( value );
            case R.string.note_tag: return setProjectedNote( value );
            default: return super.setProperty(propertyTag, value);

        }
    }
    public Object getProperty(int propertyTag){
        switch(propertyTag){
            case R.string.date_tag: return getProjectedDate();
            case R.string.amount_tag: return getProjectedAmount();
            case R.string.note_tag: return getProjectedNote();
            default: return super.getProperty(propertyTag);
        }
    }

    public String getScheduledNote() { return this.scheduledNote; }


//    @Override
//    public int compareTo(Object transaction) {
//        LocalDate comparisonDate;
//        if(transaction instanceof ProjectedTransaction) comparisonDate = (LocalDate)(((ProjectedTransaction)transaction).getProperty(R.string.date_tag ));
//        else comparisonDate = (LocalDate)(((Transaction)transaction).getProperty(R.string.date_tag ));
//
//        if ( ((LocalDate)this.getProperty(R.string.date_tag)).isAfter( comparisonDate )) return 1;
//        if ( ((LocalDate)this.getProperty(R.string.date_tag)).isEqual( comparisonDate )) return 0;
//        else return -1;
//    }
}
