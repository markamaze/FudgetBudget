package com.e.fudgetbudget.model;

import com.e.fudgetbudget.R;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class ProjectedTransaction {
    private String label;
    private Boolean isIncome;
    private Double amount;
    private String note;
    private LocalDate scheduledDate;
    private LocalDate projectedDate;

    public static ProjectedTransaction getInstance(LocalDate projected_date, Transaction root_transaction){
        return new ProjectedTransaction( projected_date, root_transaction );
    }
    public static ProjectedTransaction getInstance(Node item){ return new ProjectedTransaction( item ); }

    private ProjectedTransaction(Node item) {
        NodeList propertyNodes = item.getChildNodes();

        for ( int index = 0; index < propertyNodes.getLength(); index++ ) {
            Node currentNode = propertyNodes.item( index );
            String property = currentNode.getNodeName();
            Object value = currentNode.getNodeValue();

            if(property.contentEquals( "label" )) setLabel( value );
            else if(property.contentEquals( "value" )) setAmount( value );
            else if(property.contentEquals( "projectedDate" )) setProjectedDate( value );
            else if(property.contentEquals( "scheduledDate" )) setScheduledDate( value );
            else if(property.contentEquals( "note" )) setNote( value );
            else if(property.contentEquals( "incomeFlag" )) setIncomeFlag( value );
            else throw new Error("unknown property found while creating: ProjectedTransaction");
        }
    }
    private ProjectedTransaction(LocalDate projected_date, Transaction transaction){
        setLabel(transaction.getLabel());
        setIncomeFlag(transaction.getIncomeFlag());
        setAmount(transaction.getAmount());
        setNote(transaction.getNote());
        setProjectedDate(projected_date);
        setScheduledDate(transaction.getScheduledDate());
    }
    private boolean setLabel(Object object) {
        if(object instanceof String) this.label = (String) object;
        else throw new Error("invalid property type: ProjectedTransaction.label");

        return true;
    }
    private boolean setScheduledDate(Object object){
        if(object == null) this.scheduledDate = LocalDate.now();
        else if(object instanceof String) this.scheduledDate = LocalDate.parse( (String)object, DateTimeFormatter.BASIC_ISO_DATE );
        if(object instanceof LocalDate) this.scheduledDate = (LocalDate) object;
        else throw new Error("invalid property type: ProjectedTransaction.scheduledDate");
        return true;
    }
    private boolean setProjectedDate(Object object){
        if(object == null) this.projectedDate = LocalDate.now();
        else if(object instanceof String) this.projectedDate = LocalDate.parse( (String) object, DateTimeFormatter.BASIC_ISO_DATE );
        else if(object instanceof LocalDate) this.projectedDate = (LocalDate) object;
        else throw new Error("invalid property type: ProjectedTransaction.projectedDate");
        return true;
    }
    private boolean setAmount(Object object){
        if(object instanceof Double) this.amount = (Double) object;
        else throw new Error("invalid property type: ProjectedTransaction.amount");
        return true;
    }
    private boolean setIncomeFlag(Object object) {
        if(object instanceof Boolean) this.isIncome = (Boolean) object;
        else throw new Error("invalid property type: ProjectedTransaction.isIncome");
        return true;
    }
    private boolean setNote(Object object) {
        if(object instanceof String) this.note = (String) object;
        else throw new Error("invalid property type: ProjectedTransaction.note");
        return true;
    }

    public String getLabel(){ return this.label; }
    public LocalDate getScheduledDate(){ return this.scheduledDate; }
    public LocalDate getProjectedDate(){ return this.projectedDate; }
    public Double getAmount(){ return this.amount; }
    public Boolean getIncomeFlag(){ return this.isIncome; }
    public String getNote(){ return this.note; }


    public boolean setPropertyById(int propertyId, Object value){
        switch (propertyId){
            case R.id.label_value: return this.setLabel( value );
            case R.id.scheduled_date_value: return this.setScheduledDate( value );
            case R.id.projected_date_value: return this.setProjectedDate( value );
            case R.id.amount_value: return this.setAmount( value );
            case R.id.note_value: return this.setNote( value );
            default: return false;
        }
    }
    public Object getPropertyById(int propertyId){
        return null;
    }

    public Element writeToElement() {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = null;
        try { documentBuilder = documentBuilderFactory.newDocumentBuilder(); }
        catch (ParserConfigurationException e) { e.printStackTrace(); }
        Document document = documentBuilder.newDocument();
        Element element = document.createElement("projectedTransaction");

        Element label_node = document.createElement( "label" );
        Element value_node = document.createElement( "value" );
        Element scheduledDate_node = document.createElement( "scheduledDate" );
        Element projectedDate_node = document.createElement( "projectedDate" );
        Element note_node = document.createElement( "note" );
        Element incomeflag_node = document.createElement( "incomeFlag" );

        label_node.setTextContent( this.getLabel() );
        value_node.setTextContent( this.getAmount().toString() );
        scheduledDate_node.setTextContent( this.getScheduledDate().format( DateTimeFormatter.BASIC_ISO_DATE ) );
        projectedDate_node.setTextContent( this.getProjectedDate().format( DateTimeFormatter.BASIC_ISO_DATE ) );
        note_node.setTextContent( this.getNote() );
        incomeflag_node.setTextContent( this.getIncomeFlag().toString() );

        element.appendChild( label_node );
        element.appendChild( value_node );
        element.appendChild( scheduledDate_node );
        element.appendChild( projectedDate_node );
        element.appendChild( note_node );
        element.appendChild(incomeflag_node);

        return element;
    }


}
