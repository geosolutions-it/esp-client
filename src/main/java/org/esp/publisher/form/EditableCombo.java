package org.esp.publisher.form;


import it.jrc.form.component.FormConstants;
import it.jrc.form.controller.EditorController;
import it.jrc.form.controller.EditorController.EditCompleteListener;
import it.jrc.form.controller.EditorController.DeleteCompleteListener;
import it.jrc.persist.Dao;

import java.util.List;

import javax.persistence.TypedQuery;

import com.vaadin.data.Property;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

/**
 * A combo box which can manage the items within it.
 * 
 * Deletions have not yet been implemented.
 * 
 * @author Will Temperley
 *
 * @param <T>
 */
public class EditableCombo<T> extends EditableField<T> {

    public EditableCombo(final Class<T> clazz, Dao dao) {
        this(clazz, dao, "");
    }
    private String query = null;
    public EditableCombo(final Class<T> clazz, Dao dao, String query) {
        
        /*
         * The selection widget
         */
        super(new ComboBox2<T>());
        this.query = query;
        this.clazz = clazz;
        this.dao = dao;

        /*
         * The window
         */
        w = new Window();
        w.setModal(true);
        w.setWidth("728px");
        w.setHeight("450px");

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
    
    public ComboBox2 getEncapsulatedField() {
        return (ComboBox2)encapsulatedField;
    }

    @Override
    public void setEditor(EditorController<T> editor) {
        this.editor = editor;
        w.setContent(editor);

        /*
         * Decide what to do
         */
        editor.addEditCompleteListener(new EditCompleteListener<T>() {
            @Override
            public void onEditComplete(T entity) {
            	
            	encapsulatedField.removeAllItems();
				if(query!=null && !query.isEmpty()){
		            populateCombo(query);
		        }else{
		            populateCombo();
		        }
				encapsulatedField.setValue(entity);
				fireValueChange(false);

                UI.getCurrent().removeWindow(w);

            }
        });
        
        editor.addDeleteCompleteListener(new DeleteCompleteListener<T>() {

			@Override
			public void onDeleteComplete(T entity) {
				encapsulatedField.removeAllItems();
				if(query!=null && !query.isEmpty()){
		            populateCombo(query);
		        }else{
		            populateCombo();
		        }
				UI.getCurrent().removeWindow(w);
			}
        	
		});
    }
    
    @Override
    protected void populateCombo(String query) {
        TypedQuery<T> jpaQuery = dao.getEntityManager().createQuery(query, clazz);
        List<T> items = jpaQuery.getResultList();
        for (T t : items) {
            encapsulatedField.addItem(t);
        }
    }
}
