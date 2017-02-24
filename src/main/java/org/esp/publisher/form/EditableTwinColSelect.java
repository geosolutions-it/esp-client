package org.esp.publisher.form;

import it.jrc.form.component.FormConstants;
import it.jrc.form.controller.EditorController;
import it.jrc.form.controller.EditorController.EditCompleteListener;
import it.jrc.persist.Dao;

import java.util.List;
import java.util.Set;

import javax.persistence.metamodel.SingularAttribute;

import com.vaadin.data.Property;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.TwinColSelect;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

/**
 * A quick fix ... make this extend {@link EditableField}
 * 
 * @author Will Temperley
 *
 * @param <T>
 */
public class EditableTwinColSelect<T> extends CustomField<Set> {

    private NoEventMultiSelect combo;
    private Window w;
    private Class<T> clazz;
    private EditorController<T> editor;
    private Dao dao;

    /**
     * Just makes the internal value method visible
     * 
     * @author Will Temperley
     *
     */
    class NoEventMultiSelect extends TwinColSelect {
        @Override
        protected void setInternalValue(Object newValue) {
            super.setInternalValue(newValue);
        }
    }

    public EditableTwinColSelect(final Class<T> clazz, Dao dao, SingularAttribute<?, ?> orderBy) {

        this.clazz = clazz;
        this.dao = dao;

        /*
         * The window
         */
        w = new Window();
        w.setModal(true);
        w.setWidth("728px");
        w.setHeight("450px");

        /*
         * The selection widget
         */
        this.combo = new NoEventMultiSelect();
        combo.setWidth(FormConstants.LARGE_FIELD_DEFAULT_WIDTH);

        combo.setImmediate(true);

        populateCombo(orderBy);

        combo.addValueChangeListener(new Property.ValueChangeListener() {

            public void valueChange(Property.ValueChangeEvent event) {
                Object obj = event.getProperty().getValue();
                setValue((Set<T>) obj);
            }
        });
    }

    private MenuBar getEditMenu() {

        MenuBar settings = new MenuBar();
        settings.setStyleName("field-menu");
        MenuItem settingsMenu = settings.addItem("", null);
        settingsMenu.setStyleName("icon-cog");

        settingsMenu.addItem("New", new Command() {
            @Override
            public void menuSelected(MenuItem selectedItem) {
                doCreate();
            }
        });

        return settings;
    }

    public void setEditor(EditorController<T> editor) {
        this.editor = editor;
        w.setContent(editor);

        /*
         * Decide what to do
         */
        editor.addEditCompleteListener(new EditCompleteListener<T>() {
            @Override
            public void onEditComplete(T entity) {

                //Add the entity to the container
                if (!combo.containsId(entity)) {
                    combo.addItem(entity);
                    combo.setValue(entity);
                } else {
                    combo.removeItem(entity);
                    combo.addItem(entity);
                }
                
                fireValueChange(false);

                UI.getCurrent().removeWindow(w);

            }
        });
    }

    @Override
    public void setInternalValue(Set newValue) {
        combo.setInternalValue(newValue);
        super.setInternalValue(newValue);
    }

    protected void populateCombo(SingularAttribute<?, ?> orderBy) {
    	List<T> items = null; 
    	if(orderBy!= null) {
    		items = dao.all(clazz,orderBy);
    	} else{
    		items = dao.all(clazz);
    	}
        for (T t : items) {
            combo.addItem(t);
        }
    }

    @Override
    public void setPropertyDataSource(@SuppressWarnings("rawtypes") Property newDataSource) {

        combo.setPropertyDataSource(newDataSource);
        super.setPropertyDataSource(newDataSource);
    }

    @Override
    public Set<T> getValue() {
        return (Set<T>) combo.getValue();
    }

    protected Component initContent() {
        CssLayout l = new CssLayout();
        l.addStyleName("select-create-field");
        l.addComponent(combo);
        l.addComponent(getEditMenu());
        Label label = new Label("New/Edit");
        label.setSizeUndefined();
        label.setStyleName("editable-field-label");
        l.addComponent(label);
        return l;
    }

    protected void doCreate() {
        UI.getCurrent().addWindow(w);
        w.center();
        editor.doCreate();
    }

    protected void doUpdate() {
        Object value = combo.getValue();
        if (value == null) {
            return;
        }
        UI.getCurrent().addWindow(w);
        w.center();
        editor.doUpdate((T) value);
    }

    @Override
    public Class<Set> getType() {

        return Set.class;

    }

}
