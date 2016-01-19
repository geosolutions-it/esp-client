package org.esp.publisher.form;

import it.jrc.form.component.FormConstants;
import it.jrc.form.controller.EditorController;
import it.jrc.persist.Dao;

import java.util.List;

import javax.persistence.TypedQuery;

import com.vaadin.data.Property;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

public class SimpleCombo<T> extends CustomField<T> {

	private static final long serialVersionUID = 1L;
	
    protected Class<T> clazz;
    protected Dao dao;
    
    protected IAbstractSelect<Object> encapsulatedField;

    public SimpleCombo(Class<T> javaType, Dao dao2, String query) {
    	this.encapsulatedField = new ComboBox2<T>();
    	
        this.clazz = javaType;
        this.dao = dao2;

        encapsulatedField.setWidth(FormConstants.FIELD_DEFAULT_WIDTH);

        encapsulatedField.setImmediate(true);

        if(query!=null && !query.isEmpty()){
            populateCombo(query);
        }else{
            populateCombo();
        }

        encapsulatedField.addValueChangeListener(new Property.ValueChangeListener() {

            public void valueChange(Property.ValueChangeEvent event) {
                Object obj = event.getProperty().getValue();
                setValue((T) obj);
            }
        });
	}


	@Override
    public void setInternalValue(T newValue) {
        encapsulatedField.setInternalValue(newValue);
        super.setInternalValue(newValue);
    }

    protected void populateCombo() {
        List<T> items = dao.all(clazz);
        for (T t : items) {
            encapsulatedField.addItem(t);
        }
    }
    
    protected void populateCombo(String query) {
        TypedQuery<T> jpaQuery = dao.getEntityManager().createQuery(query, clazz);
        List<T> items = jpaQuery.getResultList();
        for (T t : items) {
            encapsulatedField.addItem(t);
        }
    }

    @Override
    public void setPropertyDataSource(@SuppressWarnings("rawtypes") Property newDataSource) {
    
        encapsulatedField.setPropertyDataSource(newDataSource);
        super.setPropertyDataSource(newDataSource);
    }

    @Override
    public T getValue() {
        return (T) encapsulatedField.getValue();
    }

    protected Component initContent() {
        CssLayout l = new CssLayout();
        l.addStyleName("select-create-field");
        l.addComponent(encapsulatedField);
        return l;
    }

    
    @Override
    public Class<? extends T> getType() {
        return clazz;
    }


}